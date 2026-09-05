package com.pig4cloud.apikey.controller;

import com.pig4cloud.apikey.entity.SysApiKeyEntity;
import com.pig4cloud.apikey.service.ApiKeyService;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.service.OpenApiLogService;
import com.pig4cloud.common.result.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open API密钥管理（平台级，仅超管）：创建时返回完整密钥，仅此一次展示机会由前端提示保存
 */
@RestController
@RequestMapping("/api/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final OpenApiLogService openApiLogService;

    @PostMapping("/getLists")
    @PreAuthorize("hasAuthority('apikey:manage')")
    public R<PageResult<SysApiKeyEntity>> getLists(@RequestBody ApiKeyService.ApiKeyQueryDto dto) {
        return apiKeyService.getLists(dto);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('apikey:manage')")
    public R<SysApiKeyEntity> create(@RequestBody SysApiKeyEntity entity) {
        return apiKeyService.create(entity);
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('apikey:manage')")
    public R<Void> update(@RequestBody SysApiKeyEntity entity) {
        return apiKeyService.update(entity);
    }

    /**
     * 调用日志分页查询（按Key/时间范围/结果过滤）
     */
    @PostMapping("/getLogs")
    @PreAuthorize("hasAuthority('apikey:manage')")
    public R<PageResult<com.pig4cloud.log.entity.OpenApiLog>> getLogs(@RequestBody LogQueryDto dto) {
        return R.ok("获取数据成功", openApiLogService.pageQuery(
                dto.getKeyId(), dto.getSuccess(), dto.getStartTime(), dto.getEndTime(),
                dto.getPage(), dto.getPageSize()));
    }

    @Getter
    @Setter
    public static class LogQueryDto extends BasePageQuery {
        private Integer keyId;
        private Boolean success;
        /**
         * 开始时间（毫秒时间戳，空为不限）
         */
        private Long startTime;
        /**
         * 结束时间（毫秒时间戳，空为不限）
         */
        private Long endTime;
    }

    @PostMapping("/del")
    @PreAuthorize("hasAuthority('apikey:manage')")
    public R<Void> del(@RequestBody SysApiKeyEntity entity) {
        return apiKeyService.delete(entity.getId());
    }
}
