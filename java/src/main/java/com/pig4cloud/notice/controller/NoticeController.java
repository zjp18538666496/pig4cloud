package com.pig4cloud.notice.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.notice.dto.NoticeDto;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知公告：查看登录即可（普通用户看已发布公告），增删改需要权限点
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 管理列表（含草稿，按可见范围过滤）
     */
    @PostMapping("/getNoticeLists")
    public R<PageResult<NoticeEntity>> getNoticeLists(@RequestBody NoticeService.NoticeQueryDto dto) {
        return noticeService.getNoticeLists(dto);
    }

    @PostMapping("/createNotice")
    @PreAuthorize("hasAuthority('notice:write')")
    public R<Void> createNotice(@Valid @RequestBody NoticeDto dto) {
        return noticeService.createNotice(dto);
    }

    @PostMapping("/updateNotice")
    @PreAuthorize("hasAuthority('notice:write')")
    public R<Void> updateNotice(@Valid @RequestBody NoticeDto dto) {
        return noticeService.updateNotice(dto);
    }

    @PostMapping("/delNotice")
    @PreAuthorize("hasAuthority('notice:remove')")
    public R<Void> delNotice(@RequestBody NoticeDto dto) {
        return noticeService.deleteNotice(dto.getId());
    }
}
