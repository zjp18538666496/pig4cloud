package com.pig4cloud.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.message.service.MessageService;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流（Flowable）：角色申请场景。申请人提交（关联角色+理由）→
 * 审批人（sys_config:approval.assignee）在待办中通过/驳回 → 通过后自动绑定角色并站内信通知。
 * 流程定义 processes/approval.bpmn20.xml（key=roleApproval）
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final ConfigService configService;

    @Getter
    @Setter
    public static class ApplyDto {
        private String roleCode;

        private String reason;
    }

    @Getter
    @Setter
    public static class CompleteDto {
        private String taskId;

        private Boolean approved;

        private String comment;
    }

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }

    /**
     * 提交角色申请：校验角色存在且属于本租户，启动流程
     */
    @LogRecord(module = "审批中心", operation = "提交角色申请")
    public R<Void> apply(ApplyDto dto) {
        String applicant = currentUsername();
        if (!org.springframework.util.StringUtils.hasText(dto.getRoleCode())) {
            throw new BizException("请选择申请的角色");
        }
        UserEntity user = userMapper.selectUserByUsername(applicant);
        RoleEntity role;
        if (UserContext.isSuperTenant()) {
            // 超管申请可跨租户指定角色（如平台层账号申请租户角色做排查）
            role = roleMapper.selectOne(new QueryWrapper<RoleEntity>()
                    .eq("role_code", dto.getRoleCode())
                    .orderByAsc("tenant_id").last("LIMIT 1"));
        } else {
            role = roleMapper.selectOne(new QueryWrapper<RoleEntity>()
                    .eq("role_code", dto.getRoleCode())
                    .eq("tenant_id", user == null ? 0 : user.getTenant_id()));
        }
        if (role == null) {
            throw new BizException("申请的角色不存在");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", "role-apply");
        variables.put("applicant", applicant);
        variables.put("applicantName", user == null ? applicant : user.getName());
        variables.put("roleCode", dto.getRoleCode());
        variables.put("roleId", role.getId());
        variables.put("roleName", role.getRole_name());
        variables.put("reason", dto.getReason() == null ? "" : dto.getReason());
        variables.put("approver", configService.getValue("approval.assignee", "admin"));
        runtimeService.startProcessInstanceByKey("roleApproval", applicant + ":" + dto.getRoleCode(), variables);
        return R.ok("申请已提交，等待审批人处理", null);
    }

    /**
     * 我的申请（含进行中与已结束），按提交时间倒序
     */
    public R<List<Map<String, Object>>> my() {
        String applicant = currentUsername();
        List<Map<String, Object>> result = new ArrayList<>();
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .startedBy(applicant)
                .includeProcessVariables()
                .orderByProcessInstanceStartTime().desc()
                .list();
        for (HistoricProcessInstance instance : instances) {
            result.add(toItem(instance.getId(), instance.getEndTime() == null ? "审批中" :
                            Boolean.TRUE.equals(instance.getProcessVariables().get("approved")) ? "已通过" : "已驳回",
                    instance.getProcessVariables(), instance.getStartTime()));
        }
        return R.ok("获取数据成功", result);
    }

    /**
     * 我的待办审批任务（assignee=当前用户；默认审批人为超管）
     */
    public R<List<Map<String, Object>>> pending() {
        String assignee = currentUsername();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : taskService.createTaskQuery()
                .taskAssignee(assignee)
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list()) {
            result.add(toItem(task.getProcessInstanceId(), "待审批", task.getProcessVariables(), task.getCreateTime())
                    );
            result.get(result.size() - 1).put("taskId", task.getId());
        }
        return R.ok("获取数据成功", result);
    }

    private Map<String, Object> toItem(String processInstanceId, String status, Map<String, Object> variables, Date time) {
        Map<String, Object> item = new HashMap<>();
        item.put("processInstanceId", processInstanceId);
        item.put("status", status);
        item.put("type", variables.getOrDefault("type", ""));
        item.put("applicant", variables.getOrDefault("applicant", ""));
        item.put("applicantName", variables.getOrDefault("applicantName", ""));
        item.put("roleCode", variables.getOrDefault("roleCode", ""));
        item.put("roleName", variables.getOrDefault("roleName", ""));
        item.put("reason", variables.getOrDefault("reason", ""));
        item.put("approved", variables.get("approved"));
        item.put("time", time == null ? null : time.toString());
        return item;
    }

    /**
     * 审批通过/驳回：通过时自动绑定角色并发站内信通知申请人
     */
    @LogRecord(module = "审批中心", operation = "处理审批")
    @Transactional
    public R<Void> complete(CompleteDto dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BizException("任务不存在或已处理");
        }
        String assignee = currentUsername();
        if (!assignee.equals(task.getAssignee())) {
            throw new BizException("仅审批人本人可处理该任务");
        }
        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String applicant = String.valueOf(variables.get("applicant"));
        String roleName = String.valueOf(variables.get("roleName"));
        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 通过：按流程变量里的roleId直接追加绑定（不清空申请人既有角色）
            UserEntity applicantUser = userMapper.selectUserByUsername(applicant);
            Long roleId = variables.get("roleId") == null ? null : ((Number) variables.get("roleId")).longValue();
            if (roleId != null && applicantUser != null) {
                userMapper.insertUserRoles(List.of(Map.of(
                        "user_id", applicantUser.getId().longValue(),
                        "role_id", roleId)));
            }
        }
        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());
        taskService.complete(dto.getTaskId(), Map.of("approved", Boolean.TRUE.equals(dto.getApproved())));
        messageService.send(notifyOf(applicant, roleName, Boolean.TRUE.equals(dto.getApproved()), dto.getComment()));
        return R.ok(Boolean.TRUE.equals(dto.getApproved()) ? "已通过" : "已驳回", null);
    }

    private com.pig4cloud.message.service.MessageService.SendDto notifyOf(
            String applicant, String roleName, boolean approved, String comment) {
        com.pig4cloud.message.service.MessageService.SendDto message =
                new com.pig4cloud.message.service.MessageService.SendDto();
        message.setTarget_username(applicant);
        message.setTitle(approved ? "角色申请已通过" : "角色申请被驳回");
        message.setContent("您申请的角色【" + roleName + "】" + (approved ? "已通过并完成绑定" : "被驳回")
                + (comment == null || comment.isBlank() ? "" : "，审批意见：" + comment));
        return message;
    }
}
