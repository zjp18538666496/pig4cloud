package com.pig4cloud.common.exception;

import com.pig4cloud.common.result.R;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

/**
 * 兜底错误处理：无对应控制器(如404)、过滤器链抛出等最终会转发到/error
 */
@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public R<Void> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status != null ? Integer.parseInt(status.toString()) : 500;
        String path = Optional.ofNullable(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI))
                .map(Object::toString)
                .orElse(request.getRequestURI());
        return R.fail(code, "请求 '" + path + "' 失败");
    }
}
