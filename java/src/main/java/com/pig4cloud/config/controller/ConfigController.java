package com.pig4cloud.config.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.entity.SysConfigEntity;
import com.pig4cloud.config.mapper.SysConfigMapper;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.annotation.LogRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * 参数配置管理（平台级，仅超管）：密码策略/登录锁定阈值/日志保留等运行参数
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SysConfigMapper configMapper;
    private final ConfigService configService;

    @Getter
    @Setter
    public static class ConfigQueryDto extends BasePageQuery {
        private String config_key = "";
    }

    @Getter
    @Setter
    public static class ConfigUpdateDto {
        @NotNull(message = "配置id不能为空")
        private Integer id;

        @NotBlank(message = "配置值不能为空")
        private String config_value;
    }

    @PostMapping("/getConfigLists")
    @PreAuthorize("hasAuthority('config:write')")
    public R<PageResult<SysConfigEntity>> getConfigLists(@RequestBody ConfigQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysConfigEntity> result = configMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysConfigEntity>()
                        .like(StringUtils.hasText(dto.getConfig_key()), "config_key", dto.getConfig_key())
                        .orderByAsc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @PostMapping("/updateConfig")
    @LogRecord(module = "参数配置", operation = "修改配置")
    @PreAuthorize("hasAuthority('config:write')")
    public R<Void> updateConfig(@Valid @RequestBody ConfigUpdateDto dto) {
        SysConfigEntity exists = configMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("配置不存在");
        }
        SysConfigEntity update = new SysConfigEntity();
        update.setId(dto.getId());
        update.setConfig_value(dto.getConfig_value());
        update.setUpdate_by(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : null);
        update.setUpdate_time(new Date());
        configMapper.updateById(update);
        configService.refresh();
        return R.ok("更新成功，已即时生效", null);
    }

    /**
     * 密码策略（公开，登录/注册表单提示用，不含敏感信息）
     */
    @GetMapping("/policy")
    public R<Map<String, Object>> policy() {
        return R.ok("请求成功", Map.of(
                "minLength", configService.getInt("pwd.min-length", 8),
                "requireComplex", configService.getBool("pwd.require-complex", false),
                "captchaEnabled", configService.getBool("captcha.enabled", true)));
    }
}
