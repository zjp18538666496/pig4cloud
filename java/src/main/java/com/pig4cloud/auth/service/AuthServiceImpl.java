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

    @Override
    public LoginResult login(LoginRequest request, String ip, String userAgent) {
        try {
            // 验证码校验（一次性，校验即消费）
            captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
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

            Map<String, Object> claims = new HashMap<>();
            claims.put("username", authentication.getName());
            claims.put("authorityString", authorityString);
            claims.put("tenantId", user == null ? 0 : user.getTenant_id());

            UserVO userVO = UserVO.from(user);
            if (userVO != null) {
                // 权限点随登录响应下发，前端v-permission据此控制按钮
                userVO.setPermissions(authorities);
            }
            String accessToken = jwtUtils.getJwt(claims);
            String refreshToken = jwtUtils.getRefreshToken(claims);

            // 登录成功：解除失败锁定、更新最后登录时间、注册在线会话、记录登录日志
            loginAttemptService.recordSuccess(request.getUsername());
            userMapper.updateLastLoginTime(request.getUsername());
            registerSession(accessToken, refreshToken, userVO, user, ip, userAgent);
            saveLoginLog(request.getUsername(), ip, true, "登录成功");
            return new LoginResult(accessToken, refreshToken, userVO);
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(request.getUsername());
            String message = ex instanceof BadCredentialsException ? "用户名或密码不正确" : ex.getMessage();
            saveLoginLog(request.getUsername(), ip, false, message);
            throw ex;
        } catch (BizException ex) {
            saveLoginLog(request.getUsername(), ip, false, ex.getMessage());
            throw ex;
        }
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
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", user.getUsername())
                .set("password", passwordEncoder.encode(dto.getNewPassword()))
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        if (userMapper.update(null, updateWrapper) <= 0) {
            throw new BizException("重置失败，请稍后重试");
        }
        // 改密成功后踢掉该账号全部在线会话，旧token立即失效
        onlineUserStore.removeByUsername(user.getUsername()).forEach(session -> {
            tokenBlacklist.revoke(session.getTokenJti(), session.getAccessExpireAt());
            tokenBlacklist.revoke(session.getRefreshJti(), session.getRefreshExpireAt());
        });
        saveLoginLog(user.getUsername(), null, true, "通过邮箱验证码重置密码");
        return R.ok("密码重置成功，请使用新密码登录", null);
    }

    private void saveLoginLog(String username, String ip, boolean success, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setSuccess(success);
        loginLog.setMessage(message);
        loginLog.setCreateTime(new Date());
        loginLogService.saveLoginLog(loginLog);
    }
}
