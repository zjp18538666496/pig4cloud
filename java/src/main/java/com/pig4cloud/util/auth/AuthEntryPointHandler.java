package com.pig4cloud.util.auth;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.response.WriteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 认证失败处理类
 */
@Component
public class AuthEntryPointHandler implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws java.io.IOException {
        Response res = new ResponseImpl(401, "未携带token", null);
        new WriteResponse(response, res);
    }


}
