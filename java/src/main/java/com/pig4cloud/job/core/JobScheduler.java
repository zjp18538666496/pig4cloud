package com.pig4cloud.job.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.job.entity.SysJobEntity;
import com.pig4cloud.job.entity.SysJobLogEntity;
import com.pig4cloud.job.mapper.SysJobLogMapper;
import com.pig4cloud.job.mapper.SysJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量定时调度（单机）：每30秒扫描启用任务，按cron计算下次执行时间到期即运行并落执行日志。
 * 内存记忆下次执行时间，重启后重新计算。多实例部署时每个实例都会调度，任务需自行保证幂等
 */
@Slf4j
@Component
public class JobScheduler {

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final Map<String, JobHandler> handlers;
    /**
     * jobId -> 下次执行时间
     */
    private final Map<Integer, Date> nextRunMap = new HashMap<>();

    public JobScheduler(SysJobMapper jobMapper, SysJobLogMapper jobLogMapper, List<JobHandler> handlerList) {
        this.jobMapper = jobMapper;
        this.jobLogMapper = jobLogMapper;
        this.handlers = new HashMap<>();
        handlerList.forEach(handler -> this.handlers.put(handler.name(), handler));
    }

    @Scheduled(fixedDelay = 30 * 1000L, initialDelay = 30 * 1000L)
    public void tick() {
        List<SysJobEntity> jobs = jobMapper.selectList(
                new QueryWrapper<SysJobEntity>().eq("status", "1"));
        Date now = new Date();
        for (SysJobEntity job : jobs) {
            try {
                Date next = nextRunMap.computeIfAbsent(job.getId(),
                        key -> computeNext(job.getCron(), now));
                if (next == null || next.after(now)) {
                    continue;
                }
                // 补齐错过的周期再执行，避免每30秒重复触发
                nextRunMap.put(job.getId(), computeNext(job.getCron(), new Date()));
                runJob(job);
            } catch (Exception ex) {
                log.error("任务[{}]调度异常: {}", job.getJob_name(), ex.getMessage());
                nextRunMap.remove(job.getId());
            }
        }
    }

    /**
     * 立即执行一次（手动触发），落执行日志
     */
    public String runOnce(SysJobEntity job) {
        JobHandler handler = handlers.get(job.getHandler());
        if (handler == null) {
            return "处理器不存在：" + job.getHandler();
        }
        long start = System.currentTimeMillis();
        try {
            handler.execute();
            saveLog(job, "1", "执行成功", System.currentTimeMillis() - start);
            return "执行成功";
        } catch (Exception ex) {
            log.error("任务[{}]手动执行失败", job.getJob_name(), ex);
            saveLog(job, "0", truncate(ex.getMessage()), System.currentTimeMillis() - start);
            return "执行失败：" + ex.getMessage();
        }
    }

    private void runJob(SysJobEntity job) {
        JobHandler handler = handlers.get(job.getHandler());
        if (handler == null) {
            saveLog(job, "0", "处理器不存在：" + job.getHandler(), 0L);
            return;
        }
        long start = System.currentTimeMillis();
        try {
            handler.execute();
            saveLog(job, "1", "执行成功", System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("任务[{}]执行失败", job.getJob_name(), ex);
            saveLog(job, "0", truncate(ex.getMessage()), System.currentTimeMillis() - start);
        }
    }

    private void saveLog(SysJobEntity job, String success, String message, long costMs) {
        SysJobLogEntity logEntity = new SysJobLogEntity();
        logEntity.setJob_id(job.getId());
        logEntity.setJob_name(job.getJob_name());
        logEntity.setSuccess(success);
        logEntity.setMessage(message);
        logEntity.setCost_ms(costMs);
        logEntity.setCreate_time(new Date());
        jobLogMapper.insert(logEntity);
    }

    private Date computeNext(String cron, Date from) {
        try {
            CronExpression expression = CronExpression.parse(cron);
            java.time.LocalDateTime next = expression.next(
                    java.time.LocalDateTime.ofInstant(from.toInstant(), java.time.ZoneId.systemDefault()));
            return next == null ? null
                    : Date.from(next.atZone(java.time.ZoneId.systemDefault()).toInstant());
        } catch (IllegalArgumentException ex) {
            log.warn("cron表达式非法: {}", cron);
            return null;
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "执行失败";
        }
        return message.length() > 900 ? message.substring(0, 900) + "..." : message;
    }
}
