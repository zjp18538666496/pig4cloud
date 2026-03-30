package com.pig4cloud.util.auth;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.response.WriteResponse;
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
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
//        Response res = new ResponseImpl(403, "权限不足", null);
//        new WriteResponse (response, res);


        // 1. 获取用户当前试图访问的接口路径
        String requestUri = request.getRequestURI();

        // 2. 获取 Spring Security 底层抛出的具体异常信息 (通常是 "Access is denied")
        String exceptionMessage = accessDeniedException.getMessage();

        // 3. 组装详细的错误提示信息
        // 方案 A：直接拼接成一段详细的话
        String detailMessage = String.format("权限不足，拒绝访问接口: [%s]。底层原因: %s", requestUri, exceptionMessage);
        Response res = new ResponseImpl(403, detailMessage, null);

        /*
        // 方案 B：保持 message 简短给前端弹窗用，把详细报错塞进 data 里供开发排查
        // String shortMessage = "抱歉，您没有权限执行此操作";
        // String errorDetails = "URI: " + requestUri + " | Error: " + exceptionMessage;
        // Response res = new ResponseImpl(403, shortMessage, errorDetails);
        */

        // 4. 写回给前端
        new WriteResponse(response, res);
    }
}