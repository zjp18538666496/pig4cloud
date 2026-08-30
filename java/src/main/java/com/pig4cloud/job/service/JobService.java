package com.pig4cloud.job.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.job.core.JobScheduler;
import com.pig4cloud.job.entity.SysJobEntity;
import com.pig4cloud.job.entity.SysJobLogEntity;
import com.pig4cloud.job.mapper.SysJobLogMapper;
import com.pig4cloud.job.mapper.SysJobMapper;
import com.pig4cloud.log.annotation.LogRecord;
import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 定时任务管理：CRUD/启停/手动执行/执行日志查询
 */
@Service
public class JobService {

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final JobScheduler jobScheduler;

    public JobService(SysJobMapper jobMapper, SysJobLogMapper jobLogMapper, JobScheduler jobScheduler) {
        this.jobMapper = jobMapper;
        this.jobLogMapper = jobLogMapper;
        this.jobScheduler = jobScheduler;
    }

    @Getter
    @Setter
    public static class JobQueryDto extends com.pig4cloud.common.dto.BasePageQuery {
        private String job_name = "";
    }

    public R<PageResult<SysJobEntity>> getJobLists(JobQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysJobEntity> result = jobMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysJobEntity>()
                        .like(StringUtils.hasText(dto.getJob_name()), "job_name", dto.getJob_name())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    public R<List<SysJobLogEntity>> getJobLogs(Integer jobId) {
        return R.ok("获取数据成功", jobLogMapper.selectList(
                new QueryWrapper<SysJobLogEntity>().eq("job_id", jobId).orderByDesc("id").last("LIMIT 50")));
    }

    private void validate(SysJobEntity job) {
        if (!StringUtils.hasText(job.getJob_name())) {
            throw new BizException("任务名称不能为空");
        }
        if (!StringUtils.hasText(job.getHandler())) {
            throw new BizException("处理器不能为空");
        }
        if (!StringUtils.hasText(job.getCron()) || !CronExpression.isValidExpression(job.getCron())) {
            throw new BizException("cron表达式非法");
        }
    }

    @LogRecord(module = "定时任务", operation = "新增任务")
    public R<Void> createJob(SysJobEntity job) {
        validate(job);
        job.setStatus(job.getStatus() == null ? "0" : job.getStatus());
        job.setCreate_time(new Date());
        jobMapper.insert(job);
        return R.ok("创建成功", null);
    }

    @LogRecord(module = "定时任务", operation = "编辑任务")
    public R<Void> updateJob(SysJobEntity job) {
        if (jobMapper.selectById(job.getId()) == null) {
            throw new BizException("任务不存在");
        }
        validate(job);
        job.setUpdate_time(new Date());
        jobMapper.updateById(job);
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "定时任务", operation = "删除任务")
    public R<Void> deleteJob(Integer id) {
        jobLogMapper.delete(new QueryWrapper<SysJobLogEntity>().eq("job_id", id));
        jobMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    @LogRecord(module = "定时任务", operation = "手动执行任务")
    public R<String> runOnce(Integer id) {
        SysJobEntity job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException("任务不存在");
        }
        return R.ok("请求成功", jobScheduler.runOnce(job));
    }
}
