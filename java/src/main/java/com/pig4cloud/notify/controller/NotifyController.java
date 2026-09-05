package com.pig4cloud.notify.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.notify.entity.SysNotifyChannelEntity;
import com.pig4cloud.notify.entity.SysNotifyTemplateEntity;
import com.pig4cloud.notify.service.NotifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知管理：渠道/模板/发送记录/测试发送（平台级，notify:manage仅授予super）
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @PostMapping("/getChannelLists")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<PageResult<SysNotifyChannelEntity>> getChannelLists(@RequestBody NotifyService.ChannelQueryDto dto) {
        return notifyService.getChannelLists(dto);
    }

    @PostMapping("/createChannel")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<SysNotifyChannelEntity> createChannel(@Valid @RequestBody SysNotifyChannelEntity entity) {
        return notifyService.createChannel(entity);
    }

    @PostMapping("/updateChannel")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<Void> updateChannel(@RequestBody SysNotifyChannelEntity entity) {
        return notifyService.updateChannel(entity);
    }

    @PostMapping("/delChannel")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<Void> delChannel(@RequestBody SysNotifyChannelEntity entity) {
        return notifyService.delChannel(entity.getId());
    }

    @PostMapping("/getTemplateLists")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<PageResult<SysNotifyTemplateEntity>> getTemplateLists(@RequestBody NotifyService.TemplateQueryDto dto) {
        return notifyService.getTemplateLists(dto);
    }

    @PostMapping("/createTemplate")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<Void> createTemplate(@RequestBody SysNotifyTemplateEntity entity) {
        return notifyService.createTemplate(entity);
    }

    @PostMapping("/updateTemplate")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<Void> updateTemplate(@RequestBody SysNotifyTemplateEntity entity) {
        return notifyService.updateTemplate(entity);
    }

    @PostMapping("/delTemplate")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<Void> delTemplate(@RequestBody SysNotifyTemplateEntity entity) {
        return notifyService.delTemplate(entity.getId());
    }

    @PostMapping("/getLogs")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<PageResult<com.pig4cloud.notify.entity.NotifyLog>> getLogs(@RequestBody NotifyService.LogQueryDto dto) {
        return notifyService.getLogs(dto);
    }

    @PostMapping("/testSend")
    @PreAuthorize("hasAuthority('notify:manage')")
    public R<String> testSend(@RequestBody NotifyService.TestSendDto dto) {
        return notifyService.testSend(dto);
    }
}
