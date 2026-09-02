package com.pig4cloud.log.audit;

/**
 * 审计diff上下文：业务Service在修改/删除前调用DiffUtil.diff算出差异并暂存于此，
 * OperateLogAspect在切面结束时取出写入操作日志（无diff则不记）
 */
public class AuditDiffContext {

    private static final ThreadLocal<String> DIFF = new ThreadLocal<>();

    private AuditDiffContext() {
    }

    public static void set(String diffJson) {
        DIFF.set(diffJson);
    }

    public static String getAndClear() {
        String diff = DIFF.get();
        DIFF.remove();
        return diff;
    }
}
