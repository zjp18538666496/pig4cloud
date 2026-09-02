package com.pig4cloud.approval.controller;

import com.pig4cloud.approval.service.ApprovalService;
import com.pig4cloud.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审批中心：角色申请提交/我的申请/待办审批/处理（登录即可，任务按assignee隔离）
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/apply")
    public R<Void> apply(@RequestBody ApprovalService.ApplyDto dto) {
        return approvalService.apply(dto);
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my() {
        return approvalService.my();
    }

    @GetMapping("/pending")
    public R<List<Map<String, Object>>> pending() {
        return approvalService.pending();
    }

    @PostMapping("/complete")
    public R<Void> complete(@RequestBody ApprovalService.CompleteDto dto) {
        return approvalService.complete(dto);
    }
}
