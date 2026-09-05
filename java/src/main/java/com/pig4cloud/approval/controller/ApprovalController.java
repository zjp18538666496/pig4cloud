package com.pig4cloud.approval.controller;

import com.pig4cloud.approval.entity.SysApprovalEntity;
import com.pig4cloud.approval.service.ApprovalService;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 轻量审批：apply/myApplications登录即可用；审批动作需approval:manage（super）
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 提交申请（登录用户均可）
     */
    @PostMapping("/apply")
    public R<Void> apply(@RequestBody ApprovalService.ApplyDto dto) {
        return approvalService.apply(dto, currentUser());
    }

    /**
     * 可申请的角色选项（登录即可用，申请弹窗下拉）：本租户全部启用角色
     */
    @GetMapping("/roleOptions")
    public R<List<Map<String, Object>>> roleOptions() {
        return R.ok("获取数据成功", approvalService.roleOptions(currentUser()));
    }

    /**
     * 我的申请
     */
    @GetMapping("/myApplications")
    public R<PageResult<SysApprovalEntity>> myApplications(ApprovalService.ApprovalQueryDto dto) {
        return approvalService.myApplications(dto);
    }

    /**
     * 审批中心列表
     */
    @PostMapping("/getLists")
    @PreAuthorize("hasAuthority('approval:manage')")
    public R<PageResult<SysApprovalEntity>> getLists(@RequestBody ApprovalService.ApprovalQueryDto dto) {
        return approvalService.getLists(dto);
    }

    /**
     * 审批（通过/驳回）
     */
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('approval:manage')")
    public R<Void> approve(@RequestBody ApprovalService.ApproveDto dto) {
        return approvalService.approve(dto, currentUser());
    }
}
