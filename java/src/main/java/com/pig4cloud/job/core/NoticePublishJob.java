package com.pig4cloud.job.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 公告定时发布：每分钟扫描status=2（定时待发布）且publish_time已到的公告，
 * 置为发布（status=1）；是否扇出站内信跟随公告创建时的选择无法回溯，仅翻状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticePublishJob implements JobHandler {

    private final NoticeMapper noticeMapper;

    @Override
    public String name() {
        return "noticePublishJob";
    }

    @Override
    public void execute() {
        List<NoticeEntity> due = noticeMapper.selectList(new QueryWrapper<NoticeEntity>()
                .eq("status", "2")
                .le("publish_time", new Date()));
        for (NoticeEntity notice : due) {
            noticeMapper.update(null, new UpdateWrapper<NoticeEntity>()
                    .eq("id", notice.getId())
                    .eq("status", "2")
                    .set("status", "1")
                    .set("update_time", new Date()));
            log.info("公告[{}]定时发布完成", notice.getTitle());
        }
    }
}
