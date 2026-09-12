package com.pig4cloud.common.mq;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 轻量削峰缓冲执行器（进程内有界队列+单消费者线程）：
 * 日志落库/通知投递/Webhook推送等"可丢、重IO"任务经此异步执行，
 * 洪峰时队列满则丢弃并计数告警（不阻塞业务请求线程，不压垮下游）。
 * 替代场景：@Async默认线程池无界队列的OOM风险。进程重启丢队列内任务属预期（均为旁路任务）
 */
@Slf4j
@Component
public class BufferedExecutor {

    private static final int CAPACITY = 1000;

    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(CAPACITY);
    private final Thread worker;
    private volatile boolean running = true;
    private long droppedCount = 0;

    public BufferedExecutor() {
        worker = new Thread(this::drain, "buffered-executor");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 提交旁路任务：队列满时丢弃并告警（不阻塞调用方）
     */
    public void submit(Runnable task, String taskName) {
        if (!queue.offer(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                log.error("缓冲任务[{}]执行失败: {}", taskName, ex.getMessage());
            }
        })) {
            droppedCount++;
            if (droppedCount % 100 == 1) {
                log.warn("缓冲队列已满({})，任务[{}]被丢弃，累计丢弃{}", queue.size(), taskName, droppedCount);
            }
        }
    }

    private void drain() {
        while (running || !queue.isEmpty()) {
            try {
                Runnable task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.error("缓冲任务执行异常: {}", ex.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }
}
