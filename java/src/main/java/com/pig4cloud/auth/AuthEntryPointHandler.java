package com.pig4cloud.auth;

import com.pig4cloud.common.result.R;
import com.pig4cloud.common.util.ResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败处理类：未携带token访问受限接口
 */
@Component
@RequiredArgsConstructor
public class AuthEntryPointHandler implements AuthenticationEntryPoint {

    private final ResponseWriter responseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // 保持HTTP 200+响应体code=401，前端据此走刷新token/登录弹窗流程
        responseWriter.write(response, R.fail(R.UNAUTHORIZED, "未携带token"));
    }
}
