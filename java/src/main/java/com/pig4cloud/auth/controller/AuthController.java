package com.pig4cloud.auth.controller;

import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.auth.dto.RefreshTokenRequest;
import com.pig4cloud.auth.dto.ResetPasswordByEmailDto;
import com.pig4cloud.auth.dto.SendResetCodeDto;
import com.pig4cloud.auth.service.AuthService;
import com.pig4cloud.auth.service.CaptchaService;
import com.pig4cloud.auth.service.IpRateLimiter;
import com.pig4cloud.auth.service.OidcService;
import com.pig4cloud.auth.service.SmsLoginService;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.common.util.ServletUtils;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.user.vo.UserVO;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final IpRateLimiter ipRateLimiter;
    private final OidcService oidcService;
    private final SmsLoginService smsLoginService;
    private final com.pig4cloud.user.mapper.UserMapper userMapper;
    private final ConfigService configService;

    /**
     * 图形验证码，登录前获取；返回captchaId与base64图片。按IP限频防刷
     */
    @GetMapping("/captcha")
    public R<Map<String, String>> captcha(HttpServletRequest request) {
        ipRateLimiter.checkLimit("captcha:ip", ServletUtils.getClientIp(request), 30, 60 * 1000L,
                "验证码获取过于频繁，请稍后再试");
        return R.ok("请求成功", captchaService.generate());
    }

    /**
     * 账号密码登录，token通过响应头Authorization/Refresh-Token下发。按IP限尝试次数防爆破
     */
    @PostMapping("/login")
    @LogRecord(module = "认证", operation = "用户登录")
    public R<UserVO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response,
                           HttpServletRequest servletRequest) {
        String clientIp = ServletUtils.getClientIp(servletRequest);
        ipRateLimiter.checkLimit("login:ip", clientIp,
                configService.getInt("login.ip-window-max", 30), 10 * 60 * 1000L,
                "登录尝试过于频繁，请10分钟后再试");
        LoginResult result = authService.login(request, clientIp, servletRequest.getHeader("User-Agent"));
        response.setHeader("Authorization", "Bearer " + result.accessToken());
        response.setHeader("Refresh-Token", result.refreshToken());
        result.user().setBrand(result.brand());
        return R.ok("请求成功", result.user());
    }

    /**
     * 登出：当前token拉黑+销毁会话（前端随后清空本地缓存）
     */
    @PostMapping("/logout")
    @LogRecord(module = "认证", operation = "用户登出")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return R.ok("登出成功", null);
    }

    /**
     * 发送短信登录验证码（app.sms.enabled开启；mock模式验证码直接返回前端仅供开发）
     */
    @PostMapping("/sms/send")
    public R<String> smsSend(@RequestBody java.util.Map<String, String> body, HttpServletRequest servletRequest) {
        String mobile = body.get("mobile") == null ? "" : body.get("mobile").trim();
        if (!mobile.matches("^1[3-9]\\d{9}$")) {
            return R.fail("手机号格式不正确");
        }
        String clientIp = ServletUtils.getClientIp(servletRequest);
        ipRateLimiter.checkLimit("sms:ip", clientIp, 5, 10 * 60 * 1000L, "短信发送过于频繁，请10分钟后再试");
        if (userMapper.selectUserByUsername(mobile) == null) {
            return R.fail("该手机号未绑定任何账号");
        }
        String mockCode = smsLoginService.sendCode(mobile);
        return mockCode == null ? R.ok("验证码已发送", null) : R.ok("开发模式验证码：" + mockCode, mockCode);
    }

    /**
     * OIDC配置（公开）：登录页据此显示SSO按钮并跳转
     */
    @GetMapping("/oidc/config")
    public R<java.util.Map<String, Object>> oidcConfig() {
        return R.ok(oidcService.isEnabled() ? java.util.Map.of(
                "enabled", true,
                "authorizeUrl", "/api/auth/oidc/login") : java.util.Map.of("enabled", false));
    }

    /**
     * OIDC授权跳转（302到IdP）
     */
    @GetMapping("/oidc/login")
    public void oidcLogin(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect(oidcService.buildAuthorizeUrl());
    }

    /**
     * OIDC回调：code换userinfo→匹配本地账号→签发一次性ticket→回前端换登录态
     */
    @GetMapping("/oidc/callback")
    public void oidcCallback(@org.springframework.web.bind.annotation.RequestParam String code,
                             @org.springframework.web.bind.annotation.RequestParam String state,
                             HttpServletRequest servletRequest,
                             jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        try {
            String username = oidcService.resolveUsername(code, state);
            String ticket = oidcService.issueTicket(username);
            response.sendRedirect("/login?ticket=" + ticket);
        } catch (Exception ex) {
            response.sendRedirect("/login?ssoError=" + java.net.URLEncoder.encode(
                    ex.getMessage() == null ? "OIDC登录失败" : ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * 用一次性ticket换登录态（响应结构与密码登录一致：头带token，体带用户与品牌）
     */
    @GetMapping("/oidc/exchange")
    public R<UserVO> oidcExchange(@org.springframework.web.bind.annotation.RequestParam String ticket,
                                  HttpServletRequest servletRequest, jakarta.servlet.http.HttpServletResponse response) {
        String username = oidcService.exchangeTicket(ticket);
        String clientIp = ServletUtils.getClientIp(servletRequest);
        LoginResult result = authService.oidcLogin(username, clientIp, servletRequest.getHeader("User-Agent"));
        response.setHeader("Authorization", "Bearer " + result.accessToken());
        response.setHeader("Refresh-Token", result.refreshToken());
        result.user().setBrand(result.brand());
        return R.ok("登录成功", result.user());
    }

    /**
     * 用长期refresh token换取新的access token
     */
    @PostMapping("/refresh-token")
    @LogRecord(module = "认证", operation = "刷新令牌")
    public R<Void> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        try {
            String newAccessToken = authService.refreshAccessToken(request.getRefreshToken());
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            return R.ok("请求成功", null);
        } catch (JwtException | IllegalArgumentException e) {
            // 返回code=401，前端据此清理本地缓存并引导重新登录
            return R.fail(R.UNAUTHORIZED, "刷新token无效");
        }
    }

    /**
     * 发送找回密码邮箱验证码（公开接口，需先配置邮件服务）
     */
    @PostMapping("/sendResetCode")
    public R<Void> sendResetCode(@Valid @RequestBody SendResetCodeDto dto) {
        return authService.sendResetCode(dto);
    }

    /**
     * 邮箱验证码重置密码（公开接口，重置后该账号全部会话被踢下线）
     */
    @PostMapping("/resetPasswordByEmail")
    @LogRecord(module = "认证", operation = "邮箱找回密码")
    public R<Void> resetPasswordByEmail(@Valid @RequestBody ResetPasswordByEmailDto dto) {
        return authService.resetPasswordByEmail(dto);
    }

    /**
     * 2FA第一步：生成TOTP密钥与绑定二维码（未启用，首次动态码校验通过才正式启用）
     */
    @PostMapping("/2fa/setup")
    public R<Map<String, String>> setup2fa() {
        return R.ok("请求成功", authService.setup2fa());
    }

    /**
     * 2FA第二步：输入验证器首次动态码确认绑定，返回一次性备用恢复码
     */
    @PostMapping("/2fa/enable")
    public R<java.util.List<String>> enable2fa(@RequestBody Map<String, String> body) {
        return R.ok("绑定成功", authService.enable2fa(body.get("code")));
    }

    /**
     * 2FA解绑：需验证登录密码+当前动态码（或备用码）
     */
    @PostMapping("/2fa/disable")
    @LogRecord(module = "认证", operation = "解绑两步认证")
    public R<Void> disable2fa(@RequestBody Map<String, String> body) {
        authService.disable2fa(body.get("password"), body.get("code"));
        return R.ok("已解绑两步认证", null);
    }

    /**
     * 当前用户2FA开启状态
     */
    @GetMapping("/2fa/status")
    public R<Map<String, Boolean>> status2fa() {
        return R.ok("请求成功", Map.of("enabled", authService.is2faEnabled()));
    }

    /**
     * 重新生成备用恢复码（需验证动态码/旧备用码），旧码全部作废
     */
    @PostMapping("/2fa/backup-codes/regenerate")
    @LogRecord(module = "认证", operation = "重新生成备用恢复码")
    public R<List<String>> regenerateBackupCodes(@RequestBody Map<String, String> body) {
        return R.ok("请求成功", authService.regenerateBackupCodes(body.get("code")));
    }
}
