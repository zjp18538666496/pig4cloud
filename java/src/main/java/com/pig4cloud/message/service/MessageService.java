package com.pig4cloud.message.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.message.entity.SysMessageEntity;
import com.pig4cloud.message.mapper.SysMessageMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import com.pig4cloud.websocket.WsPushPublisher;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 站内信：按目标用户扇出落库并经WebSocket实时推送；公告发布可选同时发站内信
 */
@Service
public class MessageService {

    private final SysMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final WsPushPublisher wsPushPublisher;

    public MessageService(SysMessageMapper messageMapper, UserMapper userMapper, WsPushPublisher wsPushPublisher) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.wsPushPublisher = wsPushPublisher;
    }

    @Getter
    @Setter
    public static class MessageQueryDto extends com.pig4cloud.common.dto.BasePageQuery {
    }

    @Getter
    @Setter
    public static class SendDto {
        /**
         * 目标用户名；为空时广播给当前租户全部用户（超管则为全平台用户）
         */
        private String target_username;

        private String title;
        private String content;
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            throw new BizException("获取用户信息失败");
        }
        UserEntity user = userMapper.selectUserByUsername(username);
        return user == null ? null : user.getId().longValue();
    }

    /**
     * 我的消息（分页）
     */
    public R<PageResult<SysMessageEntity>> getMyMessages(MessageQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysMessageEntity> result = messageMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysMessageEntity>()
                        .eq("target_user_id", currentUserId())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    public R<Long> unreadCount() {
        return R.ok("请求成功", messageMapper.selectCount(
                new QueryWrapper<SysMessageEntity>().eq("target_user_id", currentUserId()).eq("read_flag", "0")));
    }

    public R<Void> markRead(Integer id) {
        messageMapper.update(null, new UpdateWrapper<SysMessageEntity>()
                .eq("id", id).eq("target_user_id", currentUserId()).set("read_flag", "1"));
        return R.ok("已读", null);
    }

    public R<Void> markAllRead() {
        messageMapper.update(null, new UpdateWrapper<SysMessageEntity>()
                .eq("target_user_id", currentUserId()).eq("read_flag", "0").set("read_flag", "1"));
        return R.ok("全部已读", null);
    }

    /**
     * 发送站内信：指定用户名发给该用户，否则广播给当前租户全部用户（超管为全平台用户）
     */
    public R<Void> send(SendDto dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BizException("消息标题不能为空");
        }
        String createBy = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserEntity> targets;
        if (StringUtils.hasText(dto.getTarget_username())) {
            UserEntity target = userMapper.selectUserByUsername(dto.getTarget_username());
            if (target == null) {
                throw new BizException("目标用户不存在");
            }
            targets = List.of(target);
        } else {
            // 广播：租户管理员发本租户；超管发全平台（UserContext为super时拦截器不过滤）
            QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
            if (!UserContext.isSuperTenant()) {
                wrapper.eq("tenant_id", UserContext.getTenantId() == null ? 0 : UserContext.getTenantId());
            }
            targets = userMapper.selectList(wrapper);
        }
        for (UserEntity target : targets) {
            insertMessage(dto.getTitle(), dto.getContent(), "1", target, createBy);
        }
        return R.ok("已发送给" + targets.size() + "名用户", null);
    }

    /**
     * 公告扇出：公告发布时按可见范围发站内信（平台公告发全平台，租户公告发本租户）
     */
    public void fanoutNotice(Integer tenantId, String title, String content, String createBy) {
        fanoutNotice(tenantId, null, title, content, createBy);
    }

    /**
     * 公告扇出（带公告id，供已读回执统计）
     */
    public void fanoutNotice(Integer tenantId, Integer noticeId, String title, String content, String createBy) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        if (tenantId != null && tenantId != 0) {
            wrapper.eq("tenant_id", tenantId);
        }
        List<UserEntity> targets = userMapper.selectList(wrapper);
        for (UserEntity target : targets) {
            insertMessage(title, content, "2", target, createBy, noticeId);
        }
    }

    /**
     * 公告已读回执统计：该公告扇出的站内信总数/已读/未读
     */
    public R<java.util.Map<String, Object>> noticeReadStats(Integer noticeId) {
        long total = messageMapper.selectCount(new QueryWrapper<SysMessageEntity>()
                .eq("notice_id", noticeId));
        long read = messageMapper.selectCount(new QueryWrapper<SysMessageEntity>()
                .eq("notice_id", noticeId).eq("read_flag", "1"));
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("total", total);
        data.put("read", read);
        data.put("unread", total - read);
        data.put("readRate", total == 0 ? "0%" : Math.round(read * 1000 / total) / 10.0 + "%");
        return R.ok("获取数据成功", data);
    }

    private void insertMessage(String title, String content, String msgType, UserEntity target, String createBy) {
        insertMessage(title, content, msgType, target, createBy, null);
    }

    private void insertMessage(String title, String content, String msgType, UserEntity target, String createBy, Integer noticeId) {
        SysMessageEntity message = new SysMessageEntity();
        message.setTitle(title);
        message.setContent(content);
        message.setMsg_type(msgType);
        message.setTenant_id(target.getTenant_id());
        message.setTarget_user_id(target.getId());
        message.setNotice_id(noticeId);
        message.setRead_flag("0");
        message.setCreate_by(createBy);
        message.setCreate_time(new Date());
        messageMapper.insert(message);
        // 经发布器推送（memory本机直推；redis经频道广播到所有实例）
        wsPushPublisher.publish(target.getId().longValue(), "{\"type\":\"message\"}");
    }
}
