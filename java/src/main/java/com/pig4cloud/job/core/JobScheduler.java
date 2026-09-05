package com.pig4cloud.job.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.store.StateStore;
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
import java.util.UUID;

/**
 * 轻量定时调度：每30秒扫描启用任务，按cron计算下次执行时间到期即运行并落执行日志。
 * 多实例适配：cron触发执行前以StateStore抢占周期锁（putIfAbsent，TTL到下个触发点+60s），
 * 谁抢到谁执行本周期，其余实例跳过；手动执行使用独立的运行互斥锁防并发。
 * 内存记忆下次执行时间，重启后重新计算
 */
@Slf4j
@Component
public class JobScheduler {

    private static final long LOCK_TTL_MILLIS = 10 * 60 * 1000L;

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final StateStore stateStore;
    private final Map<String, JobHandler> handlers;
    private final String nodeId = UUID.randomUUID().toString().substring(0, 8);
    /**
     * jobId -> 下次执行时间
     */
    private final Map<Integer, Date> nextRunMap = new HashMap<>();

    public JobScheduler(SysJobMapper jobMapper, SysJobLogMapper jobLogMapper,
                        StateStore stateStore, List<JobHandler> handlerList) {
        this.jobMapper = jobMapper;
        this.jobLogMapper = jobLogMapper;
        this.stateStore = stateStore;
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
                Date nextFire = computeNext(job.getCron(), new Date());
                nextRunMap.put(job.getId(), nextFire);
                // 分布式锁：抢占本周期执行权，锁持有到下个触发点+60s才过期（不在执行后立即释放，
                // 否则其它实例晚几秒tick时会在同一周期重复执行），多实例只有一个实例执行本周期
                long slotTtl = Math.max(30_000L,
                        (nextFire == null ? LOCK_TTL_MILLIS : nextFire.getTime() - System.currentTimeMillis() + 60_000L));
                if (!stateStore.putIfAbsent("job:slot:" + job.getId(), nodeId, slotTtl)) {
                    log.debug("任务[{}]本周期已被其它实例抢占执行", job.getJob_name());
                    continue;
                }
                try {
                    runJob(job);
                } catch (Exception runEx) {
                    log.error("任务[{}]执行异常: {}", job.getJob_name(), runEx.getMessage());
                }
            } catch (Exception ex) {
                log.error("任务[{}]调度异常: {}", job.getJob_name(), ex.getMessage());
                nextRunMap.remove(job.getId());
            }
        }
    }

    /**
     * 立即执行一次（手动触发），落执行日志；运行互斥锁防重复触发
     */
    public String runOnce(SysJobEntity job) {
        JobHandler handler = handlers.get(job.getHandler());
        if (handler == null) {
            return "处理器不存在：" + job.getHandler();
        }
        if (!stateStore.putIfAbsent("job:running:" + job.getId(), nodeId, LOCK_TTL_MILLIS)) {
            return "该任务正在执行中，请稍后再试";
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
        } finally {
            stateStore.delete("job:running:" + job.getId());
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
