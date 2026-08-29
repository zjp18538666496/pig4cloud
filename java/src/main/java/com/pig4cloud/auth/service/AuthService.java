package com.pig4cloud.auth.service;

import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;

public interface AuthService {

    /**
     * 账号密码登录，签发双token
     */
    LoginResult login(LoginRequest request);
}
