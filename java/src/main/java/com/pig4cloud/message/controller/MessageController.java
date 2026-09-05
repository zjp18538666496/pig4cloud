package com.pig4cloud.message.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.message.entity.SysMessageEntity;
import com.pig4cloud.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/getMyMessages")
    public R<PageResult<SysMessageEntity>> getMyMessages(@RequestBody MessageService.MessageQueryDto dto) {
        return messageService.getMyMessages(dto);
    }

    @GetMapping("/unreadCount")
    public R<Long> unreadCount() {
        return messageService.unreadCount();
    }

    @PostMapping("/markRead")
    public R<Void> markRead(@RequestBody Map<String, Integer> body) {
        return messageService.markRead(body.get("id"));
    }

    @PostMapping("/markAllRead")
    public R<Void> markAllRead() {
        return messageService.markAllRead();
    }

    @PostMapping("/send")
    @com.pig4cloud.common.annotation.Idempotent
    @LogRecord(module = "站内信", operation = "发送消息")
    @PreAuthorize("hasAuthority('notice:write')")
    public R<Void> send(@RequestBody MessageService.SendDto dto) {
        return messageService.send(dto);
    }
}
