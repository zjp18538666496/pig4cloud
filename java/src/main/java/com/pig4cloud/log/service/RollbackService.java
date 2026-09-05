package com.pig4cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作回滚：基于审计diff把"编辑"类操作还原为变更前的值。
 * 支持模块：角色管理(名称/描述/数据权限)、租户管理(名称/状态/配额)、用户管理(昵称/手机/邮箱——用户名不回滚)。
 * diff为可读字符串（"字段": "旧值 → 新值"），值含逗号时解析可能不精确，回滚前界面会展示diff供人工确认
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackService {

    private static final Pattern SEGMENT = Pattern.compile("\"([^\"]+)\": \"(.*?)\" → \"(.*?)\"");
    private static final Set<String> SUPPORTED_MODULES = Set.of("角色管理", "租户管理", "用户管理");

    private final MongoTemplate mongoTemplate;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;

    /**
     * 回滚：返回实际还原的字段描述
     */
    public String rollback(String logId) {
        OperateLog logEntity = mongoTemplate.findById(logId, OperateLog.class);
        if (logEntity == null) {
            throw new BizException("日志不存在");
        }
        if (!Boolean.TRUE.equals(logEntity.getSuccess())) {
            throw new BizException("仅支持回滚成功的操作");
        }
        if (!SUPPORTED_MODULES.contains(logEntity.getModule())) {
            throw new BizException("该模块暂不支持回滚（支持：角色管理/租户管理/用户管理的编辑操作）");
        }
        if (logEntity.getOperation() == null || !logEntity.getOperation().contains("编辑")
                || logEntity.getDiff() == null || logEntity.getDiff().isBlank()) {
            throw new BizException("仅支持有变更diff的编辑类操作");
        }
        Integer bizId = extractId(logEntity.getParams());
        if (bizId == null) {
            throw new BizException("日志缺少目标id，无法回滚");
        }
        Map<String, String> beforeValues = parseBefore(logEntity.getDiff());
        if (beforeValues.isEmpty()) {
            throw new BizException("未能从diff解析出变更前的值");
        }
        String applied = switch (logEntity.getModule()) {
            case "角色管理" -> applyRole(bizId, beforeValues);
            case "租户管理" -> applyTenant(bizId, beforeValues);
            case "用户管理" -> applyUser(bizId, beforeValues);
            default -> throw new BizException("该模块暂不支持回滚");
        };
        // 回滚本身落一条操作日志（审计闭环）
        OperateLog rollbackLog = new OperateLog();
        rollbackLog.setTenantId(logEntity.getTenantId());
        rollbackLog.setUsername(SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName());
        rollbackLog.setModule(logEntity.getModule());
        rollbackLog.setOperation("回滚：" + logEntity.getOperation());
        rollbackLog.setMethod("ROLLBACK");
        rollbackLog.setUrl("/api/log/rollback");
        rollbackLog.setParams("sourceLogId=" + logId);
        rollbackLog.setIp("127.0.0.1");
        rollbackLog.setSuccess(true);
        rollbackLog.setErrorMsg("已还原字段：" + applied);
        rollbackLog.setCreateTime(new java.util.Date());
        mongoTemplate.save(rollbackLog);
        log.warn("操作回滚：日志{}，模块{}，还原字段[{}]", logId, logEntity.getModule(), applied);
        return applied;
    }

    private String applyRole(Integer id, Map<String, String> before) {
        UpdateWrapper<RoleEntity> wrapper = new UpdateWrapper<RoleEntity>().eq("id", id);
        StringBuilder applied = new StringBuilder();
        for (String field : Set.of("role_name", "description", "data_scope")) {
            if (before.containsKey(field)) {
                wrapper.set(field, nullValue(before.get(field)));
                applied.append(field).append(" ");
            }
        }
        if (applied.length() == 0) {
            throw new BizException("diff中没有可回滚的角色字段");
        }
        roleMapper.update(null, wrapper);
        return applied.toString().trim();
    }

    private String applyTenant(Integer id, Map<String, String> before) {
        UpdateWrapper<TenantEntity> wrapper = new UpdateWrapper<TenantEntity>().eq("id", id);
        StringBuilder applied = new StringBuilder();
        for (String field : Set.of("tenant_name", "status", "user_limit")) {
            if (before.containsKey(field)) {
                wrapper.set(field, nullValue(before.get(field), "user_limit".equals(field)));
                applied.append(field).append(" ");
            }
        }
        if (applied.length() == 0) {
            throw new BizException("diff中没有可回滚的租户字段");
        }
        tenantMapper.update(null, wrapper);
        return applied.toString().trim();
    }

    private String applyUser(Integer id, Map<String, String> before) {
        UpdateWrapper<UserEntity> wrapper = new UpdateWrapper<UserEntity>().eq("id", id);
        StringBuilder applied = new StringBuilder();
        // username是账号身份不回滚
        for (String field : Set.of("name", "mobile", "email")) {
            if (before.containsKey(field)) {
                wrapper.set(field, nullValue(before.get(field)));
                applied.append(field).append(" ");
            }
        }
        if (applied.length() == 0) {
            throw new BizException("diff中没有可回滚的用户字段");
        }
        userMapper.update(null, wrapper);
        return applied.toString().trim();
    }

    /**
     * 解析diff字符串为 字段→变更前值（"∅"表示原为null）
     */
    private Map<String, String> parseBefore(String diff) {
        Map<String, String> result = new LinkedHashMap<>();
        String body = diff.startsWith("{") ? diff.substring(1, diff.length() - 1) : diff;
        for (String segment : body.split("\", \"")) {
            Matcher matcher = SEGMENT.matcher(segment);
            if (matcher.matches()) {
                result.put(matcher.group(1), matcher.group(2));
            }
        }
        return result;
    }

    private Object nullValue(String before) {
        return nullValue(before, false);
    }

    private Object nullValue(String before, boolean numeric) {
        if ("∅".equals(before)) {
            return null;
        }
        if (numeric) {
            try {
                return Integer.valueOf(before.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return before;
    }

    private Integer extractId(String params) {
        if (params == null || params.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(params);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
