package com.pig4cloud.apikey.open;

import com.pig4cloud.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * Open API调用上下文：从请求属性读取过滤器写入的scopes，提供Scope断言
 */
public class OpenApiContext {

    private OpenApiContext() {
    }

    public static Set<String> scopes(HttpServletRequest request) {
        Object scopes = request.getAttribute("openApiScopes");
        return scopes instanceof Set<?> set ? (Set<String>) set : Set.of();
    }

    /**
     * 断言当前Key具备指定scope，否则拒绝
     */
    public static void requireScope(HttpServletRequest request, String scope) {
        if (!scopes(request).contains(scope)) {
            throw new BizException(403, "该API Key未授权scope：" + scope);
        }
    }
}
