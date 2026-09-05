package com.pig4cloud.auth.service;

import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.auth.dto.ResetPasswordByEmailDto;
import com.pig4cloud.auth.dto.SendResetCodeDto;
import com.pig4cloud.common.result.R;

import java.util.List;
import java.util.Map;

public interface AuthService {

    /**
     * OIDC单点登录签发：username为userinfo中匹配到的本地账号
     */
    LoginResult oidcLogin(String username, String ip, String userAgent);

    /**
     * 账号密码登录：验证码校验→失败锁定校验→认证→注册在线会话→写登录日志
     *
     * @param ip        客户端ip
     * @param userAgent 登录浏览器User-Agent
     */
    LoginResult login(LoginRequest request, String ip, String userAgent);

    /**
     * 刷新access token：校验refresh token未被拉黑，并重挂在线会话
     */
    String refreshAccessToken(String refreshToken);

    /**
     * 登出：拉黑当前access token与其会话的refresh token，移除在线会话
     */
    void logout(String authHeader);

    /**
     * 发送找回密码邮箱验证码
     */
    R<Void> sendResetCode(SendResetCodeDto dto);

    /**
     * 邮箱验证码重置密码，成功后踢掉该账号全部在线会话
     */
    R<Void> resetPasswordByEmail(ResetPasswordByEmailDto dto);

    /**
     * 2FA绑定第一步：生成TOTP密钥与二维码（存库但未启用），返回{secret, qrImage}
     */
    Map<String, String> setup2fa();

    /**
     * 2FA绑定第二步：首次动态码校验通过后启用，返回一次性备用恢复码列表
     */
    List<String> enable2fa(String code);

    /**
     * 2FA解绑：校验登录密码+动态码（或备用码）后清除绑定
     */
    void disable2fa(String password, String code);

    /**
     * 当前用户是否已开启两步认证
     */
    boolean is2faEnabled();

    /**
     * 重新生成备用恢复码（需验证当前动态码或旧备用码），旧码全部作废
     */
    List<String> regenerateBackupCodes(String code);

    /**
     * 超管代理登录：以目标用户身份签发token（claims带impersonator标记，全程登录日志审计）
     */
    LoginResult impersonate(String targetUsername, String operator);
}
