package com.pig4cloud.log.audit;

import java.util.Map;

/**
 * 审计对比工具：对编辑/删除前后的字段Map做差集，生成可读的diff JSON字符串
 */
public class DiffUtil {

    private DiffUtil() {
    }

    /**
     * 计算before与after的字段级差异：值为"before → after"。
     * 新增字段记为"∅ → after"，删除字段记为"before → ∅"；无差异返回null
     */
    public static String diff(Map<String, ?> before, Map<String, ?> after) {
        Map<String, ?> beforeMap = before == null ? Map.of() : before;
        Map<String, ?> afterMap = after == null ? Map.of() : after;
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (String key : afterMap.keySet()) {
            Object afterValue = afterMap.get(key);
            String afterStr = afterValue == null ? "∅" : String.valueOf(afterValue);
            Object beforeValue = beforeMap.get(key);
            String beforeStr = beforeValue == null ? "∅" : String.valueOf(beforeValue);
            if (!beforeMap.containsKey(key) || !afterStr.equals(beforeStr)) {
                append(json, first, key, beforeStr, afterStr);
                first = false;
            }
        }
        for (String key : beforeMap.keySet()) {
            if (!afterMap.containsKey(key)) {
                append(json, first, key, String.valueOf(beforeMap.get(key)), "∅");
                first = false;
            }
        }
        if (first) {
            return null;
        }
        return json.append("}").toString();
    }

    private static void append(StringBuilder json, boolean first, String key, String before, String after) {
        if (!first) {
            json.append(", ");
        }
        json.append("\"").append(key).append("\": \"").append(escape(before)).append("\" → \"")
                .append(escape(after)).append("\"");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\"", "'").replace("\n", " ");
    }
}
