package com.pig4cloud.common.context;

/**
 * 当前请求的租户上下文：JwtAuthenticationFilter解析token后填充，请求结束清理。
 * 未设置(null)表示未认证或后台任务——此时租户拦截器不追加过滤条件。
 */
public final class UserContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private record Context(Integer tenantId, boolean isSuper) {
    }

    private UserContext() {
    }

    public static void set(Integer tenantId, boolean isSuper) {
        HOLDER.set(new Context(tenantId, isSuper));
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static Integer getTenantId() {
        Context context = HOLDER.get();
        return context == null ? null : context.tenantId();
    }

    public static boolean isSuperTenant() {
        Context context = HOLDER.get();
        return context != null && context.isSuper();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
