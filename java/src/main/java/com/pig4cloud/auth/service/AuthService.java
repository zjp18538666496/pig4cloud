package com.pig4cloud.auth.service;

import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.auth.dto.ResetPasswordByEmailDto;
import com.pig4cloud.auth.dto.SendResetCodeDto;
import com.pig4cloud.common.result.R;

public interface AuthService {

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
}
