package com.pig4cloud.notify.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.notify.entity.NotifyLog;
import com.pig4cloud.notify.entity.SysNotifyChannelEntity;
import com.pig4cloud.notify.entity.SysNotifyTemplateEntity;
import com.pig4cloud.notify.mapper.SysNotifyChannelMapper;
import com.pig4cloud.notify.mapper.SysNotifyTemplateMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 通知服务：模板渲染(${变量}) + 多渠道异步投递 + 发送记录。
 * 事件接入点统一走sendByEvent（吞异常，通知失败不影响主流程）：
 * 公告发布(notice-publish)/任务失败(job-failed)/租户到期预警(tenant-expire-warning)/用户配额预警(user-quota-warning)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final SysNotifyChannelMapper channelMapper;
    private final SysNotifyTemplateMapper templateMapper;
    private final NotifySender sender;
    private final MongoTemplate mongoTemplate;

    @Getter
    @Setter
    public static class ChannelQueryDto extends BasePageQuery {
        private String channel_name = "";
    }

    @Getter
    @Setter
    public static class TemplateQueryDto extends BasePageQuery {
        private String template_name = "";
    }

    @Getter
    @Setter
    public static class LogQueryDto extends BasePageQuery {
        private Integer channelId;
        private Boolean success;
    }

    @Getter
    @Setter
    public static class TestSendDto {
        private Integer channelId;
        private String title;
        private String content;
    }

    // ==================== 渠道管理 ====================

    public R<PageResult<SysNotifyChannelEntity>> getChannelLists(ChannelQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysNotifyChannelEntity> result = channelMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysNotifyChannelEntity>()
                        .like(StringUtils.hasText(dto.getChannel_name()), "channel_name", dto.getChannel_name())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @LogRecord(module = "通知管理", operation = "创建渠道")
    public R<SysNotifyChannelEntity> createChannel(SysNotifyChannelEntity entity) {
        validateChannel(entity);
        entity.setId(null);
        entity.setCreate_by(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : null);
        entity.setCreate_time(new Date());
        channelMapper.insert(entity);
        return R.ok("创建成功", entity);
    }

    @LogRecord(module = "通知管理", operation = "编辑渠道")
    public R<Void> updateChannel(SysNotifyChannelEntity entity) {
        validateChannel(entity);
        SysNotifyChannelEntity update = new SysNotifyChannelEntity();
        update.setId(entity.getId());
        update.setChannel_name(entity.getChannel_name());
        update.setChannel_type(entity.getChannel_type());
        update.setConfig(entity.getConfig());
        update.setStatus(entity.getStatus());
        update.setRemark(entity.getRemark());
        update.setUpdate_time(new Date());
        channelMapper.updateById(update);
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "通知管理", operation = "删除渠道")
    public R<Void> delChannel(Integer id) {
        channelMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    private void validateChannel(SysNotifyChannelEntity entity) {
        if (!StringUtils.hasText(entity.getChannel_name())) {
            throw new BizException("渠道名称不能为空");
        }
        if (!StringUtils.hasText(entity.getChannel_type())) {
            throw new BizException("渠道类型不能为空");
        }
        if (!List.of("email", "webhook", "dingtalk", "wecom", "feishu").contains(entity.getChannel_type())) {
            throw new BizException("不支持的渠道类型：" + entity.getChannel_type());
        }
    }

    // ==================== 模板管理 ====================

    public R<PageResult<SysNotifyTemplateEntity>> getTemplateLists(TemplateQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysNotifyTemplateEntity> result = templateMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysNotifyTemplateEntity>()
                        .like(StringUtils.hasText(dto.getTemplate_name()), "template_name", dto.getTemplate_name())
                        .orderByAsc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @LogRecord(module = "通知管理", operation = "创建模板")
    public R<Void> createTemplate(SysNotifyTemplateEntity entity) {
        if (!StringUtils.hasText(entity.getTemplate_code()) || !StringUtils.hasText(entity.getTemplate_name())) {
            throw new BizException("模板编码与名称不能为空");
        }
        if (templateMapper.selectCount(new QueryWrapper<SysNotifyTemplateEntity>()
                .eq("template_code", entity.getTemplate_code())) > 0) {
            throw new BizException("模板编码已存在");
        }
        entity.setId(null);
        entity.setCreate_time(new Date());
        templateMapper.insert(entity);
        return R.ok("创建成功", null);
    }

    @LogRecord(module = "通知管理", operation = "编辑模板")
    public R<Void> updateTemplate(SysNotifyTemplateEntity entity) {
        if (entity.getId() == null) {
            throw new BizException("模板id不能为空");
        }
        SysNotifyTemplateEntity update = new SysNotifyTemplateEntity();
        update.setId(entity.getId());
        update.setTemplate_name(entity.getTemplate_name());
        update.setTitle_template(entity.getTitle_template());
        update.setContent_template(entity.getContent_template());
        update.setStatus(entity.getStatus());
        update.setRemark(entity.getRemark());
        update.setUpdate_time(new Date());
        templateMapper.updateById(update);
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "通知管理", operation = "删除模板")
    public R<Void> delTemplate(Integer id) {
        templateMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    // ==================== 发送 ====================

    /**
     * 事件通知入口（异步、吞异常）：按模板编码渲染后投递到全部启用渠道，通知失败不影响主流程
     */
    @Async
    public void sendByEvent(String templateCode, Map<String, String> params) {
        try {
            send(templateCode, params);
        } catch (Exception ex) {
            log.warn("事件通知[{}]发送失败: {}", templateCode, ex.getMessage());
        }
    }

    /**
     * 按模板编码渲染并投递到全部启用渠道，返回成功投递的渠道数
     */
    public int send(String templateCode, Map<String, String> params) {
        SysNotifyTemplateEntity template = templateMapper.selectOne(
                new QueryWrapper<SysNotifyTemplateEntity>().eq("template_code", templateCode));
        if (template == null || !"1".equals(template.getStatus())) {
            return 0;
        }
        String title = render(template.getTitle_template(), params);
        String content = render(template.getContent_template(), params);
        List<SysNotifyChannelEntity> channels = channelMapper.selectList(
                new QueryWrapper<SysNotifyChannelEntity>().eq("status", "1"));
        int success = 0;
        for (SysNotifyChannelEntity channel : channels) {
            String error = deliver(channel, templateCode, title, content);
            if (error == null) {
                success++;
            }
        }
        return success;
    }

    /**
     * 测试发送（管理界面验证渠道连通性）
     */
    public R<String> testSend(TestSendDto dto) {
        SysNotifyChannelEntity channel = channelMapper.selectById(dto.getChannelId());
        if (channel == null) {
            throw new BizException("渠道不存在");
        }
        String title = StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "PIGX ADMIN 通知测试";
        String content = StringUtils.hasText(dto.getContent()) ? dto.getContent() : "这是一条测试通知，收到说明渠道配置正确。";
        String error = deliver(channel, "test", title, content);
        return error == null ? R.ok("发送成功，请到目标群/邮箱确认", null) : R.fail("发送失败：" + error);
    }

    /**
     * 投递单渠道并落发送记录（同步调用方自行决定线程），返回null=成功
     */
    private String deliver(SysNotifyChannelEntity channel, String templateCode, String title, String content) {
        long start = System.currentTimeMillis();
        String receiver = "";
        try {
            Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(channel.getConfig() == null ? "{}" : channel.getConfig(), Map.class);
            Object url = config.get("url");
            Object to = config.get("to");
            receiver = url != null ? url.toString() : (to != null ? to.toString() : "");
        } catch (Exception ignored) {
        }
        String error;
        try {
            error = sender.send(channel.getChannel_type(), channel.getConfig(), title, content);
        } catch (Exception ex) {
            error = ex.getMessage();
        }
        saveLog(channel, templateCode, title, receiver, error, System.currentTimeMillis() - start);
        return error;
    }

    @Async
    protected void saveLog(SysNotifyChannelEntity channel, String templateCode, String title,
                           String receiver, String error, long costMs) {
        try {
            NotifyLog logEntity = new NotifyLog();
            logEntity.setChannelId(channel.getId());
            logEntity.setChannelName(channel.getChannel_name());
            logEntity.setChannelType(channel.getChannel_type());
            logEntity.setTemplateCode(templateCode);
            logEntity.setTitle(title);
            logEntity.setReceiver(receiver);
            logEntity.setSuccess(error == null);
            logEntity.setMessage(error);
            logEntity.setCostMs(costMs);
            logEntity.setCreateTime(new Date());
            mongoTemplate.save(logEntity);
        } catch (Exception ex) {
            log.warn("通知发送记录写入失败: {}", ex.getMessage());
        }
    }

    /**
     * 发送记录分页
     */
    public R<PageResult<NotifyLog>> getLogs(LogQueryDto dto) {
        List<Criteria> criteria = new java.util.ArrayList<>();
        if (dto.getChannelId() != null) {
            criteria.add(Criteria.where("channelId").is(dto.getChannelId()));
        }
        if (dto.getSuccess() != null) {
            criteria.add(Criteria.where("success").is(dto.getSuccess()));
        }
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        long total = mongoTemplate.count(query, NotifyLog.class);
        long page = dto.getPage() == null ? 1 : dto.getPage();
        long pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();
        query.with(org.springframework.data.domain.PageRequest.of(
                (int) Math.max(0, page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime")));
        List<NotifyLog> rows = mongoTemplate.find(query, NotifyLog.class);
        return R.ok("获取数据成功", PageResult.of(rows, total, pageSize, page));
    }

    /**
     * 模板渲染：替换${key}占位，未提供的变量保留原样便于排查
     */
    private String render(String template, Map<String, String> params) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                result = result.replace("${" + entry.getKey() + "}",
                        entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return result;
    }
}
