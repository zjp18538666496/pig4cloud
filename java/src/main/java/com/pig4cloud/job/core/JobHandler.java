package com.pig4cloud.job.core;

/**
 * 定时任务处理器接口：实现类注册为Spring Bean即可在任务管理中按name()引用
 */
public interface JobHandler {

    /**
     * 处理器标识（sys_job.handler引用）
     */
    String name();

    /**
     * 执行任务
     */
    void execute() throws Exception;
}
