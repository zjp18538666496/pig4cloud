package com.pig4cloud.log.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.dto.LogQueryDto;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.service.OperateLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final OperateLogService operateLogService;

    @PostMapping("/getOperateLogs")
    @PreAuthorize("hasAuthority('log:read')")
    public R<PageResult<OperateLog>> getOperateLogs(@RequestBody LogQueryDto dto) {
        return R.ok("获取数据成功", operateLogService.pageQuery(dto.getUsername(), dto.getPage(), dto.getPageSize()));
    }
}
