package com.pig4cloud.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.result.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在过滤器/安全处理器中直接写JSON响应（此时未经过DispatcherServlet，无法走统一返回值）
 */
@Component
@RequiredArgsConstructor
public class ResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, R<?> body) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
