package com.pig4cloud.apikey.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.apikey.entity.SysApiKeyEntity;
import com.pig4cloud.apikey.mapper.SysApiKeyMapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.annotation.LogRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

/**
 * Open API密钥管理：CRUD+校验。密钥由服务端生成（sk_前缀+40位hex），
 * 调用方携带请求头X-Api-Key访问/api/open/**，按scopes授权、按分钟限流
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final SysApiKeyMapper apiKeyMapper;
    private final ConfigService configService;
    private final SecureRandom random = new SecureRandom();

    @Getter
    @Setter
    public static class ApiKeyQueryDto extends com.pig4cloud.common.dto.BasePageQuery {
        private String app_name = "";
    }

    public R<PageResult<SysApiKeyEntity>> getLists(ApiKeyQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysApiKeyEntity> result = apiKeyMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysApiKeyEntity>()
                        .like(StringUtils.hasText(dto.getApp_name()), "app_name", dto.getApp_name())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @LogRecord(module = "API密钥", operation = "创建密钥")
    public R<SysApiKeyEntity> create(SysApiKeyEntity entity) {
        if (!StringUtils.hasText(entity.getApp_name())) {
            throw new BizException("接入方名称不能为空");
        }
        entity.setId(null);
        entity.setApi_key(generateKey());
        entity.setApi_secret(generateSecret());
        entity.setStatus(StringUtils.hasText(entity.getStatus()) ? entity.getStatus() : "1");
        entity.setCreate_by(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : null);
        entity.setCreate_time(new Date());
        apiKeyMapper.insert(entity);
        return R.ok("创建成功，请立即保存密钥", entity);
    }

    @LogRecord(module = "API密钥", operation = "编辑密钥")
    public R<Void> update(SysApiKeyEntity entity) {
        SysApiKeyEntity exists = apiKeyMapper.selectById(entity.getId());
        if (exists == null) {
            throw new BizException("密钥不存在");
        }
        // 密钥本体与创建信息不可改；改名称/范围/状态/过期时间/备注
        SysApiKeyEntity update = new SysApiKeyEntity();
        update.setId(entity.getId());
        update.setApp_name(entity.getApp_name());
        update.setScopes(entity.getScopes());
        update.setStatus(entity.getStatus());
        update.setRemark(entity.getRemark());
        update.setExpire_time(entity.getExpire_time());
        apiKeyMapper.updateById(update);
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "API密钥", operation = "删除密钥")
    public R<Void> delete(Integer id) {
        apiKeyMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    /**
     * 过滤器调用：校验密钥有效性（存在/启用/未过期），无效抛业务异常
     */
    public SysApiKeyEntity validate(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BizException(401, "缺少X-Api-Key请求头");
        }
        SysApiKeyEntity entity = apiKeyMapper.selectOne(
                new QueryWrapper<SysApiKeyEntity>().eq("api_key", apiKey));
        if (entity == null || !"1".equals(entity.getStatus())) {
            throw new BizException(401, "API Key无效或已停用");
        }
        if (entity.getExpire_time() != null && entity.getExpire_time().before(new Date())) {
            throw new BizException(401, "API Key已过期");
        }
        return entity;
    }

    /**
     * 记录最后调用时间（每次校验成功后刷新）
     */
    public void touchLastUsed(Integer id) {
        apiKeyMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<SysApiKeyEntity>()
                .eq("id", id).set("last_used_time", new Date()));
    }

    public int rateLimit() {
        return configService.getInt("openapi.rate-limit", 60);
    }

    /**
     * 鉴权模式：simple(仅X-Api-Key) | hmac(强制签名) | both(默认，带签名头走签名，否则simple)
     */
    public String authMode() {
        return configService.getValue("openapi.auth-mode", "both");
    }

    private String generateKey() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return "sk_" + HexFormat.of().formatHex(bytes);
    }

    /**
     * 签名密钥：64位hex（32字节），HMAC-SHA256用
     */
    private String generateSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
