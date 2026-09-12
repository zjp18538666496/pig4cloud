package com.pig4cloud.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.approval.entity.SysApprovalEntity;
import com.pig4cloud.approval.mapper.SysApprovalMapper;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.message.entity.SysMessageEntity;
import com.pig4cloud.message.mapper.SysMessageMapper;
import com.pig4cloud.notify.service.NotifyService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 轻量审批：申请单状态机（0待审批→1通过/2驳回）。
 * 审批人=approval.assignee配置的账号（默认admin），新单站内信+通知渠道提醒；
 * role_apply类型通过后自动给申请人绑定角色并踢会话（权限即时生效）。
 * 事件接入：apply-pending（通知审批人）/apply-result（通知申请人）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    public static final String TYPE_ROLE_APPLY = "role_apply";

    private final SysApprovalMapper approvalMapper;
    private final ConfigService configService;
    private final NotifyService notifyService;
    private final SessionKickService sessionKickService;
    private final com.pig4cloud.user.mapper.UserMapper userMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.pig4cloud.message.mapper.SysMessageMapper messageMapper;

    @Getter
    @Setter
    public static class ApprovalQueryDto extends BasePageQuery {
        private String status = "";
        private String applicant = "";
    }

    @Getter
    @Setter
    public static class ApplyDto {
        /**
         * 类型(role_apply/tenant_open/handover)
         */
        private String applyType;

        /**
         * 角色申请：目标角色id
         */
        private Integer roleId;

        private String reason;
    }

    @Getter
    @Setter
    public static class ApproveDto {
        private Integer id;

        /**
         * true=通过 false=驳回
         */
        private Boolean pass;

        private String comment;
    }

    /**
     * 我的申请（当前登录人）
     */
    public R<PageResult<SysApprovalEntity>> myApplications(ApprovalQueryDto dto) {
        String me = currentUser();
        return pageQuery(dto, me);
    }

    /**
     * 可申请的角色选项：本租户（或平台层）的全部角色，排除已拥有的
     */
    public List<Map<String, Object>> roleOptions(String applicant) {
        var user = userMapper.selectUserByUsername(applicant);
        if (user == null) {
            return List.of();
        }
        Integer tenantId = user.getTenant_id() == null ? 0 : user.getTenant_id();
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                "SELECT id, role_name FROM sys_role WHERE (tenant_id = ? OR tenant_id = 0) ORDER BY id",
                tenantId);
        // 排除已拥有角色
        return roles.stream()
                .filter(r -> {
                    Long owned = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM user_role ur JOIN sys_user u ON u.id = ur.user_id "
                                    + "WHERE u.username = ? AND ur.role_id = ? AND u.deleted = 0",
                            Long.class, applicant, r.get("id"));
                    return owned == null || owned == 0;
                })
                .map(r -> Map.<String, Object>of("id", r.get("id"), "roleName", String.valueOf(r.get("role_name"))))
                .toList();
    }

    /**
     * 审批中心列表（approval:manage权限点控制）；status空查全部
     */
    public R<PageResult<SysApprovalEntity>> getLists(ApprovalQueryDto dto) {
        return pageQuery(dto, null);
    }

    private R<PageResult<SysApprovalEntity>> pageQuery(ApprovalQueryDto dto, String onlyApplicant) {
        QueryWrapper<SysApprovalEntity> wrapper = new QueryWrapper<SysApprovalEntity>()
                .orderByDesc("id");
        if (onlyApplicant != null) {
            wrapper.eq("applicant", onlyApplicant);
        } else {
            if (StringUtils.hasText(dto.getStatus())) {
                wrapper.eq("status", dto.getStatus());
            }
            if (StringUtils.hasText(dto.getApplicant())) {
                wrapper.like("applicant", dto.getApplicant());
            }
        }
        Page<SysApprovalEntity> result = approvalMapper.selectPage(
                new Page<>(Math.max(1, dto.getPage()), Math.max(1, dto.getPageSize())), wrapper);
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(),
                dto.getPageSize(), dto.getPage()));
    }

    /**
     * 提交申请：role_apply校验角色存在且未拥有，同类型同目标待审批去重；
     * 成功后给审批人发站内信+通知渠道（apply-pending）
     */
    @LogRecord(module = "审批中心", operation = "提交申请")
    public R<Void> apply(ApplyDto dto, String applicant) {
        if (!List.of(TYPE_ROLE_APPLY, "tenant_open", "handover").contains(dto.getApplyType())) {
            throw new BizException("不支持的申请类型");
        }
        String title;
        String bizData = null;
        if (TYPE_ROLE_APPLY.equals(dto.getApplyType())) {
            if (dto.getRoleId() == null) {
                throw new BizException("请选择要申请的角色");
            }
            Map<String, Object> role = jdbcTemplate.queryForMap(
                    "SELECT id, role_name FROM sys_role WHERE id = ?", dto.getRoleId());
            // 已拥有该角色则不允许重复申请
            Long owned = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_role ur JOIN sys_user u ON u.id = ur.user_id "
                            + "WHERE u.username = ? AND ur.role_id = ? AND u.deleted = 0",
                    Long.class, applicant, dto.getRoleId());
            if (owned != null && owned > 0) {
                throw new BizException("您已拥有该角色，无需申请");
            }
            title = "角色申请：" + role.get("role_name");
            bizData = "{\"roleId\":" + dto.getRoleId() + ",\"roleName\":\""
                    + String.valueOf(role.get("role_name")).replace("\"", "") + "\"}";
        } else {
            title = "tenant_open".equals(dto.getApplyType()) ? "租户开通申请" : "离职交接申请";
        }
        // 同类待审批去重（防重复提交）
        Long pending = approvalMapper.selectCount(new QueryWrapper<SysApprovalEntity>()
                .eq("applicant", applicant).eq("apply_type", dto.getApplyType())
                .eq("biz_data", bizData == null ? "" : bizData)
                .eq("status", "0"));
        if (pending != null && pending > 0) {
            throw new BizException("已有同类申请待审批，请勿重复提交");
        }
        SysApprovalEntity entity = new SysApprovalEntity();
        entity.setTitle(title);
        entity.setApply_type(dto.getApplyType());
        entity.setBiz_data(bizData);
        entity.setReason(dto.getReason());
        entity.setApplicant(applicant);
        var user = userMapper.selectUserByUsername(applicant);
        entity.setTenant_id(user == null ? null : user.getTenant_id());
        entity.setStatus("0");
        entity.setCreate_time(new Date());
        approvalMapper.insert(entity);
        // 提醒审批人（站内信+通知渠道）
        String assignee = configService.getValue("approval.assignee", "admin");
        sendSiteMessage(assignee, "新审批待办：" + title,
                "申请人【" + applicant + "】提交了申请：" + title
                        + (StringUtils.hasText(dto.getReason()) ? "。理由：" + dto.getReason() : ""),
                user == null ? null : user.getTenant_id());
        notifyService.sendByEvent("approval-pending", Map.of(
                "applicant", applicant,
                "title", title,
                "reason", StringUtils.hasText(dto.getReason()) ? dto.getReason() : "-"));
        return R.ok("申请已提交，等待审批", null);
    }

    /**
     * 审批（通过/驳回）：通过且role_apply时给申请人绑角色并踢会话；
     * 结果站内信+通知渠道推给申请人（apply-result）
     */
    @LogRecord(module = "审批中心", operation = "审批")
    @Transactional
    public R<Void> approve(ApproveDto dto, String approver) {
        SysApprovalEntity approval = approvalMapper.selectById(dto.getId());
        if (approval == null) {
            throw new BizException("申请单不存在");
        }
        if (!"0".equals(approval.getStatus())) {
            throw new BizException("该申请已审批完成");
        }
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        approval.setStatus(pass ? "1" : "2");
        approval.setApprover(approver);
        approval.setApprove_comment(dto.getComment());
        approval.setApprove_time(new Date());
        approvalMapper.updateById(approval);
        String resultText = pass ? "通过" : "驳回";
        // 角色申请通过：给申请人绑定角色（幂等）并踢会话让权限即时生效
        if (pass && TYPE_ROLE_APPLY.equals(approval.getApply_type())) {
            try {
                Map<String, Object> biz = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(approval.getBiz_data() == null ? "{}" : approval.getBiz_data(), Map.class);
                Object roleId = biz.get("roleId");
                var applicantUser = userMapper.selectUserByUsername(approval.getApplicant());
                if (roleId != null && applicantUser != null) {
                    Long exists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM user_role WHERE user_id = ? AND role_id = ?",
                            Long.class, applicantUser.getId(), Integer.valueOf(roleId.toString()));
                    if (exists == null || exists == 0) {
                        jdbcTemplate.update("INSERT INTO user_role (user_id, role_id) VALUES (?, ?)",
                                applicantUser.getId(), Integer.valueOf(roleId.toString()));
                    }
                    sessionKickService.kickUser(approval.getApplicant());
                }
            } catch (Exception ex) {
                log.error("审批通过后绑定角色失败：申请单{}", approval.getId(), ex);
                throw new BizException("绑定角色失败：" + ex.getMessage());
            }
        }
        sendSiteMessage(approval.getApplicant(), "审批" + resultText + "：" + approval.getTitle(),
                "您提交的申请【" + approval.getTitle() + "】已被" + resultText
                        + (StringUtils.hasText(dto.getComment()) ? "。审批意见：" + dto.getComment() : ""),
                approval.getTenant_id());
        notifyService.sendByEvent("approval-result", Map.of(
                "title", approval.getTitle(),
                "result", resultText,
                "comment", StringUtils.hasText(dto.getComment()) ? dto.getComment() : "-"));
        return R.ok("审批完成", null);
    }

    /**
     * 站内信直发（落库；审批场景均在登录上下文中，创建人取上下文即可）
     */
    private void sendSiteMessage(String targetUsername, String title, String content, Integer tenantId) {
        try {
            var target = userMapper.selectUserByUsername(targetUsername);
            if (target == null) {
                return;
            }
            SysMessageEntity message = new SysMessageEntity();
            message.setTitle(title);
            message.setContent(content);
            message.setMsg_type("1");
            message.setTenant_id(tenantId);
            message.setTarget_user_id(target.getId());
            message.setRead_flag("0");
            message.setCreate_by(SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName() : "系统");
            message.setCreate_time(new Date());
            messageMapper.insert(message);
        } catch (Exception ex) {
            log.warn("审批站内信发送失败: {}", ex.getMessage());
        }
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
