package com.pig4cloud.auth.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.auth.dto.ResetPasswordByEmailDto;
import com.pig4cloud.auth.dto.SendResetCodeDto;
import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.SessionRecord;
import com.pig4cloud.auth.online.TokenBlacklist;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.service.LoginLogService;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import com.pig4cloud.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;
    private final MailService mailService;
    private final OnlineUserStore onlineUserStore;
    private final TokenBlacklist tokenBlacklist;
    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final PasswordPolicyService passwordPolicyService;
    private final TotpService totpService;
    private final com.pig4cloud.auth.online.SessionKickService sessionKickService;

    @Override
    public LoginResult login(LoginRequest request, String ip, String userAgent) {
        try {
            // 图形验证码（sys_config可关；关闭时不校验，前端也隐藏输入框）
            if (configService.getBool("captcha.enabled", true)) {
                if (request.getCaptchaId() == null || request.getCaptchaId().isBlank()
                        || request.getCaptchaCode() == null || request.getCaptchaCode().isBlank()) {
                    throw new BizException("验证码不能为空");
                }
                captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
            }
            // 失败锁定校验
            loginAttemptService.checkLocked(request.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 角色编码+按钮权限点合并存入token，解析端再拆开
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            String authorityString = String.join(",", authorities);

            UserEntity user = userMapper.selectUserByUsername(request.getUsername());

            // 两步认证：总开关开启且用户已绑定TOTP时，校验动态码/备用恢复码（code 1001=需要动态码）
            if (configService.getBool("login.2fa-enabled", true)
                    && user != null && Integer.valueOf(1).equals(user.getTotp_enabled())) {
                verifyTwoFactor(request, user);
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("username", authentication.getName());
            claims.put("authorityString", authorityString);
            claims.put("tenantId", user == null ? 0 : user.getTenant_id());

            UserVO userVO = UserVO.from(user);
            if (userVO != null) {
                // 权限点随登录响应下发，前端v-permission据此控制按钮
                userVO.setPermissions(authorities);
                // 密码策略：初始密码未改或密码过期时强制修改
                userVO.setForcePwdChange(isForcePwdChange(user));
                // 2FA治理：强制开启时未绑定者登录后引导绑定
                userVO.setForce2fa(configService.getBool("login.2fa-force-enabled", false)
                        && (user == null || !Integer.valueOf(1).equals(user.getTotp_enabled())));
            }
            String accessToken = jwtUtils.getJwt(claims);
            String refreshToken = jwtUtils.getRefreshToken(claims);

            // 登录成功：解除失败锁定、更新最后登录时间、注册在线会话、记录登录日志
            loginAttemptService.recordSuccess(request.getUsername());
            userMapper.updateLastLoginTime(request.getUsername());
            registerSession(accessToken, refreshToken, userVO, user, ip, userAgent);
            // 并发设备数限制：超限自动下线最旧设备（0=不限制）
            int maxSessions = configService.getInt("login.max-sessions-per-user", 3);
            if (maxSessions > 0) {
                sessionKickService.enforceSessionLimit(request.getUsername(), maxSessions);
            }
            saveLoginLog(request.getUsername(), ip, user == null ? null : user.getTenant_id(), true, "登录成功");
            return new LoginResult(accessToken, refreshToken, userVO);
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(request.getUsername());
            String message = ex instanceof BadCredentialsException ? "用户名或密码不正确" : ex.getMessage();
            saveLoginLog(request.getUsername(), ip, resolveTenantId(request.getUsername()), false, message);
            throw ex;
        } catch (BizException ex) {
            saveLoginLog(request.getUsername(), ip, resolveTenantId(request.getUsername()), false, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 两步认证校验：TOTP动态码或一次性备用恢复码（命中即消费）。
     * 失败抛code=1001（前端据此弹出动态码输入框），并计入失败锁定防止6位码爆破
     */
    private void verifyTwoFactor(LoginRequest request, UserEntity user) {
        String code = request.getTotpCode();
        if (code == null || code.isBlank()) {
            throw new BizException(1001, "请输入动态验证码");
        }
        if (totpService.verify(user.getTotp_secret(), code, null)) {
            return;
        }
        String hit = totpService.verifyBackupCode(code, user.getBackup_codes());
        if (hit != null) {
            // 消费命中的备用码
            String remaining = java.util.Arrays.stream(user.getBackup_codes().split(","))
                    .map(String::trim)
                    .filter(saved -> !saved.equals(hit))
                    .collect(java.util.stream.Collectors.joining(","));
            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", user.getId()).set("backup_codes", remaining);
            userMapper.update(null, updateWrapper);
            return;
        }
        throw new BizException(1001, "动态验证码不正确");
    }

    /**
     * 密码是否需要强制修改：初始/重置密码未改，或超出有效期（pwd.expire-days>0时启用）
     */
    private boolean isForcePwdChange(UserEntity user) {
        if (user == null) {
            return false;
        }
        if (user.getForce_pwd_change() != null && user.getForce_pwd_change() == 1) {
            return true;
        }
        int expireDays = configService.getInt("pwd.expire-days", 0);
        return expireDays > 0 && user.getPwd_update_time() != null
                && user.getPwd_update_time().before(new Timestamp(System.currentTimeMillis() - expireDays * 24L * 3600 * 1000));
    }

    /**
     * 登录失败场景尽力解析租户（用户名不存在等场景为null）
     */
    private Integer resolveTenantId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        UserEntity user = userMapper.selectUserByUsername(username);
        return user == null ? null : user.getTenant_id();
    }

    /**
     * 注册在线会话：以access token的jti为主键，强退/登出靠它定位
     */
    private void registerSession(String accessToken, String refreshToken, UserVO userVO,
                                 UserEntity user, String ip, String userAgent) {
        Claims accessClaims = jwtUtils.parseJwt(accessToken);
        Claims refreshClaims = jwtUtils.parseRefreshToken(refreshToken);
        SessionRecord record = SessionRecord.builder()
                .tokenJti(accessClaims.getId())
                .refreshJti(refreshClaims.getId())
                .username(userVO != null ? userVO.getUsername() : null)
                .name(userVO != null ? userVO.getName() : null)
                .tenantId(user == null ? 0 : user.getTenant_id())
                .ip(ip)
                .browser(SessionRecord.parseBrowser(userAgent))
                .loginTime(new Date())
                .lastAccessTime(new Date())
                .accessExpireAt(accessClaims.getExpiration().getTime())
                .refreshExpireAt(refreshClaims.getExpiration().getTime())
                .build();
        onlineUserStore.register(record);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        Claims refreshClaims = jwtUtils.parseRefreshToken(refreshToken);
        // 已登出/被强退的refresh token不允许再换新token
        if (tokenBlacklist.isRevoked(refreshClaims.getId())) {
            throw new IllegalArgumentException("登录状态已失效");
        }
        String newAccessToken = jwtUtils.refreshToken(refreshToken);
        // 重挂在线会话：key从旧access jti换成新access jti，避免刷新后从在线列表消失
        Claims accessClaims = jwtUtils.parseJwt(newAccessToken);
        SessionRecord session = onlineUserStore.findByRefreshJti(refreshClaims.getId());
        if (session != null) {
            // 旧access token立即拉黑，防止轮换后被窃用
            tokenBlacklist.revoke(session.getTokenJti(), session.getAccessExpireAt());
            SessionRecord updated = session.toBuilder()
                    .tokenJti(accessClaims.getId())
                    .accessExpireAt(accessClaims.getExpiration().getTime())
                    .lastAccessTime(new Date())
                    .build();
            onlineUserStore.rekeyAccess(session.getTokenJti(), updated);
        }
        return newAccessToken;
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        try {
            Claims claims = jwtUtils.parseJwt(authHeader.substring("Bearer ".length()));
            String jti = claims.getId();
            tokenBlacklist.revoke(jti, claims.getExpiration().getTime());
            SessionRecord session = onlineUserStore.remove(jti);
            if (session != null) {
                // 同时拉黑该会话的refresh token，防止登出后刷新续命
                tokenBlacklist.revoke(session.getRefreshJti(), session.getRefreshExpireAt());
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // token无效无需登出处理
        }
    }

    @Override
    public R<Void> sendResetCode(SendResetCodeDto dto) {
        if (!mailService.isEnabled()) {
            throw new BizException("邮件服务未配置，请联系管理员重置密码");
        }
        UserEntity user = userMapper.selectUserByEmail(dto.getEmail());
        if (user == null) {
            throw new BizException("该邮箱未绑定任何账号");
        }
        passwordResetService.sendCode(dto.getEmail(), mailService);
        return R.ok("验证码已发送，请查收邮件", null);
    }

    @Override
    public R<Void> resetPasswordByEmail(ResetPasswordByEmailDto dto) {
        passwordResetService.verify(dto.getEmail(), dto.getCode());
        UserEntity user = userMapper.selectUserByEmail(dto.getEmail());
        if (user == null) {
            throw new BizException("该邮箱未绑定任何账号");
        }
        // 新密码走密码策略校验
        passwordPolicyService.validate(dto.getNewPassword());
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", user.getUsername())
                .set("password", passwordEncoder.encode(dto.getNewPassword()))
                .set("pwd_update_time", new Timestamp(System.currentTimeMillis()))
                .set("force_pwd_change", 0)
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        if (userMapper.update(null, updateWrapper) <= 0) {
            throw new BizException("重置失败，请稍后重试");
        }
        // 改密成功后踢掉该账号全部在线会话，旧token立即失效
        onlineUserStore.removeByUsername(user.getUsername()).forEach(session -> {
            tokenBlacklist.revoke(session.getTokenJti(), session.getAccessExpireAt());
            tokenBlacklist.revoke(session.getRefreshJti(), session.getRefreshExpireAt());
        });
        saveLoginLog(user.getUsername(), null, user.getTenant_id(), true, "通过邮箱验证码重置密码");
        return R.ok("密码重置成功，请使用新密码登录", null);
    }

    private void saveLoginLog(String username, String ip, Integer tenantId, boolean success, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setTenantId(tenantId);
        loginLog.setSuccess(success);
        loginLog.setMessage(message);
        loginLog.setCreateTime(new Date());
        loginLogService.saveLoginLog(loginLog);
    }

    private UserEntity currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = username == null ? null : userMapper.selectUserByUsername(username);
        if (user == null) {
            throw new BizException("获取用户信息失败");
        }
        return user;
    }

    @Override
    public Map<String, String> setup2fa() {
        UserEntity user = currentUser();
        // 已启用时不允许重新生成（需先解绑），避免覆盖有效密钥
        if (Integer.valueOf(1).equals(user.getTotp_enabled())) {
            throw new BizException("两步认证已开启，如需重新绑定请先解绑");
        }
        String secret = totpService.generateSecret();
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId())
                .set("totp_secret", secret)
                .set("totp_enabled", 0)
                .set("backup_codes", null);
        userMapper.update(null, updateWrapper);
        String otpauthUri = totpService.buildOtpauthUri(user.getUsername(), secret);
        return Map.of("secret", secret, "otpauthUri", otpauthUri, "qrImage", totpService.qrImage(otpauthUri));
    }

    @Override
    public List<String> enable2fa(String code) {
        UserEntity user = currentUser();
        if (user.getTotp_secret() == null || user.getTotp_secret().isBlank()) {
            throw new BizException("请先生成绑定二维码");
        }
        if (!totpService.verify(user.getTotp_secret(), code, null)) {
            throw new BizException("动态验证码不正确，请确认验证器时间是否准确");
        }
        // 生成一次性备用恢复码：明文仅本次返回，库中只存SHA256
        var entry = totpService.generateBackupCodes();
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId())
                .set("totp_enabled", 1)
                .set("backup_codes", entry.getValue());
        userMapper.update(null, updateWrapper);
        log.info("用户[{}]已开启两步认证", user.getUsername());
        return entry.getKey();
    }

    @Override
    public boolean is2faEnabled() {
        return Integer.valueOf(1).equals(currentUser().getTotp_enabled());
    }

    @Override
    public List<String> regenerateBackupCodes(String code) {
        UserEntity user = currentUser();
        if (!Integer.valueOf(1).equals(user.getTotp_enabled())) {
            throw new BizException("请先开启两步认证");
        }
        boolean verified = totpService.verify(user.getTotp_secret(), code, null)
                || totpService.verifyBackupCode(code, user.getBackup_codes()) != null;
        if (!verified) {
            throw new BizException("动态验证码不正确");
        }
        var entry = totpService.generateBackupCodes();
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId()).set("backup_codes", entry.getValue());
        userMapper.update(null, updateWrapper);
        return entry.getKey();
    }

    @Override
    public void disable2fa(String password, String code) {
        UserEntity user = currentUser();
        if (!passwordEncoder.matches(password == null ? "" : password, user.getPassword())) {
            throw new BizException("登录密码不正确");
        }
        boolean verified = totpService.verify(user.getTotp_secret(), code, null)
                || totpService.verifyBackupCode(code, user.getBackup_codes()) != null;
        if (!verified) {
            throw new BizException("动态验证码不正确");
        }
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId())
                .set("totp_secret", null)
                .set("totp_enabled", 0)
                .set("backup_codes", null);
        userMapper.update(null, updateWrapper);
        log.info("用户[{}]已解绑两步认证", user.getUsername());
    }
}
