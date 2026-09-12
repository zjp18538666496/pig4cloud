package com.pig4cloud.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.message.service.MessageService;
import com.pig4cloud.notice.dto.NoticeDto;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.mapper.NoticeMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 通知公告：平台超管发平台公告(tenant_id=0全员可见)，租户管理员发本租户公告。
 * sys_notice不走租户拦截器，可见性在这里手动控制
 */
@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final MessageService messageService;
    private final com.pig4cloud.notify.service.NotifyService notifyService;

    public NoticeService(NoticeMapper noticeMapper, MessageService messageService, com.pig4cloud.notify.service.NotifyService notifyService) {
        this.noticeMapper = noticeMapper;
        this.messageService = messageService;
        this.notifyService = notifyService;
    }

    @Getter
    @Setter
    public static class NoticeQueryDto extends BasePageQuery {
        private String title = "";

        /**
         * 状态筛选；空查全部
         */
        private String status = "";
    }

    /**
     * 管理列表：当前租户可见范围的公告（含草稿）
     */
    public R<PageResult<NoticeEntity>> getNoticeLists(NoticeQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Integer tenantId = UserContext.getTenantId() == null ? 0 : UserContext.getTenantId();
        // 超管管理全部公告；普通租户只管理"平台公告+本租户公告"
        QueryWrapper<NoticeEntity> wrapper = new QueryWrapper<>();
        if (!UserContext.isSuperTenant()) {
            wrapper.and(w -> w.eq("tenant_id", 0).or().eq("tenant_id", tenantId));
        }
        wrapper.like(StringUtils.hasText(dto.getTitle()), "title", dto.getTitle())
                .eq(StringUtils.hasText(dto.getStatus()), "status", dto.getStatus())
                .orderByDesc("id");
        Page<NoticeEntity> result = noticeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    /**
     * 最新发布公告（仪表盘展示）：已发布且在可见范围内
     */
    public List<NoticeEntity> latestPublished(int limit) {
        Integer tenantId = UserContext.getTenantId() == null ? 0 : UserContext.getTenantId();
        if (UserContext.isSuperTenant()) {
            return noticeMapper.selectList(new QueryWrapper<NoticeEntity>()
                    .eq("status", "1").orderByDesc("id").last("LIMIT " + limit));
        }
        return noticeMapper.selectVisible(tenantId).stream()
                .filter(notice -> "1".equals(notice.getStatus()))
                .limit(limit)
                .toList();
    }

    @LogRecord(module = "通知公告", operation = "新增公告")
    public R<Void> createNotice(NoticeDto dto) {
        NoticeEntity notice = new NoticeEntity();
        applyDto(dto, notice);
        notice.setContent(sanitizeHtml(notice.getContent()));
        // tenant_id由服务端决定：super发平台公告(0)，租户管理员发本租户公告
        notice.setTenant_id(UserContext.getTenantId() == null ? 0 : UserContext.getTenantId());
        notice.setCreate_by(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : null);
        notice.setCreate_time(new Date());
        noticeMapper.insert(notice);
        fanoutIfRequested(dto, notice);
        return R.ok("创建成功", null);
    }

    @LogRecord(module = "通知公告", operation = "编辑公告")
    public R<Void> updateNotice(NoticeDto dto) {
        NoticeEntity exists = noticeMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("公告不存在");
        }
        checkEditable(exists);
        NoticeEntity notice = new NoticeEntity();
        notice.setId(dto.getId());
        applyDto(dto, notice);
        notice.setContent(sanitizeHtml(notice.getContent()));
        notice.setUpdate_time(new Date());
        noticeMapper.updateById(notice);
        exists.setStatus(notice.getStatus());
        fanoutIfRequested(dto, exists);
        return R.ok("更新成功", null);
    }

    /**
     * 发布状态下勾选"同时发站内信"时，向可见范围用户扇出消息
     */
    private void fanoutIfRequested(NoticeDto dto, NoticeEntity notice) {
        if (Boolean.TRUE.equals(dto.getSendMessage()) && "1".equals(notice.getStatus())) {
            messageService.fanoutNotice(notice.getTenant_id(), notice.getId(), notice.getTitle(),
                    Jsoup.parse(notice.getContent() == null ? "" : notice.getContent()).text(), notice.getCreate_by());
            // 通知渠道事件：公告发布（邮件/钉钉/企微/飞书/Webhook，未配置渠道时静默跳过）
            notifyService.sendByEvent("notice-publish", Map.of(
                    "title", notice.getTitle() == null ? "" : notice.getTitle(),
                    "content", Jsoup.parse(notice.getContent() == null ? "" : notice.getContent()).text()));
        }
    }

    @LogRecord(module = "通知公告", operation = "删除公告")
    public R<Void> deleteNotice(Integer id) {
        NoticeEntity exists = noticeMapper.selectById(id);
        if (exists == null) {
            throw new BizException("公告不存在");
        }
        checkEditable(exists);
        noticeMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    /**
     * 平台公告只有super能改删；租户公告只有本租户管理员能改删
     */
    private void checkEditable(NoticeEntity notice) {
        if (notice.getTenant_id() != null && notice.getTenant_id() == 0 && !UserContext.isSuperTenant()) {
            throw new BizException("平台公告仅平台管理员可操作");
        }
        if (notice.getTenant_id() != null && notice.getTenant_id() != 0
                && !UserContext.isSuperTenant()
                && !notice.getTenant_id().equals(UserContext.getTenantId())) {
            throw new BizException("无权操作其他租户的公告");
        }
    }

    private void applyDto(NoticeDto dto, NoticeEntity notice) {
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setStatus(dto.getStatus() == null ? "0" : dto.getStatus());
        if ("2".equals(notice.getStatus())) {
            // 定时发布必须带发布时间
            if (dto.getPublishTime() == null) {
                throw new BizException("定时发布必须选择发布时间");
            }
            notice.setPublish_time(dto.getPublishTime());
        } else {
            notice.setPublish_time(null);
        }
    }

    /**
     * 公告富文本消毒：白名单过滤标签/属性防XSS（允许图片/标题/表格/行内样式）
     */
    private String sanitizeHtml(String content) {
        if (content == null) {
            return null;
        }
        Safelist safelist = Safelist.basicWithImages()
                .addTags("h1", "h2", "h3", "h4", "h5", "h6", "table", "thead", "tbody", "tr", "td", "th", "font", "span", "u", "s")
                .addAttributes("span", "style")
                .addAttributes("font", "color", "size")
                .addAttributes(":all", "style", "width", "height");
        return Jsoup.clean(content, "", safelist);
    }
}
