package com.pig4cloud.util.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足处理类
 */
@Component
public class AuthAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        String detailMessage = String.format("权限不足，拒绝访问接口: [%s]。底层原因: %s",
                request.getRequestURI(), accessDeniedException.getMessage());
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(403, detailMessage)));
    }
}
