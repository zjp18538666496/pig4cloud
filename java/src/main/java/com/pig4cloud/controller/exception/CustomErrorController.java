package com.pig4cloud.controller.exception;

import com.pig4cloud.dao.impl.ResponseImpl;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

/**
 * 自定义错误处理
 */
@Controller
public class CustomErrorController implements ErrorController {
    private final ErrorAttributes errorAttributes;

    @Autowired
    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        // 获取原始请求路径
        String forwardedPath = getOriginalRequestPath(request);
        // 构建自定义错误响应对象
        Map<String, Object> errorDetails = new HashMap<>();
        // 获取错误信息
        Map<String, Object> errorAttributes = getErrorAttributes(request);
        errorDetails.put("forwardedPath", forwardedPath);
        errorDetails.put("errorAttributes", errorAttributes);
        int status = parseInt(errorAttributes.get("status").toString());
        ResponseImpl responseImpl = new ResponseImpl(
                status,
                "请求 '" + forwardedPath + "' 失败",
                errorDetails
        );
        // 返回错误消息
        return ResponseEntity.status(status).body(responseImpl);
    }

    private String getOriginalRequestPath(HttpServletRequest request) {
        Object forwardedPath = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        if (forwardedPath != null) {
            return forwardedPath.toString();
        }
        // 如果找不到转发路径，则返回默认路径
        return request.getRequestURI();
    }

    // 获取错误信息
    private Map<String, Object> getErrorAttributes(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        return this.errorAttributes.getErrorAttributes(webRequest, options);
    }

}
