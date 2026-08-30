package com.pig4cloud.job.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.job.entity.SysJobEntity;
import com.pig4cloud.job.entity.SysJobLogEntity;
import com.pig4cloud.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理（平台级，仅超管）
 */
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/getJobLists")
    @PreAuthorize("hasAuthority('job:write')")
    public R<PageResult<SysJobEntity>> getJobLists(@RequestBody JobService.JobQueryDto dto) {
        return jobService.getJobLists(dto);
    }

    @GetMapping("/getJobLogs")
    @PreAuthorize("hasAuthority('job:write')")
    public R<List<SysJobLogEntity>> getJobLogs(@RequestParam Integer jobId) {
        return jobService.getJobLogs(jobId);
    }

    @PostMapping("/createJob")
    @PreAuthorize("hasAuthority('job:write')")
    public R<Void> createJob(@RequestBody SysJobEntity job) {
        return jobService.createJob(job);
    }

    @PostMapping("/updateJob")
    @PreAuthorize("hasAuthority('job:write')")
    public R<Void> updateJob(@RequestBody SysJobEntity job) {
        return jobService.updateJob(job);
    }

    @PostMapping("/delJob")
    @PreAuthorize("hasAuthority('job:remove')")
    public R<Void> delJob(@RequestBody Map<String, Integer> body) {
        return jobService.deleteJob(body.get("id"));
    }

    @PostMapping("/runOnce")
    @PreAuthorize("hasAuthority('job:run')")
    public R<String> runOnce(@RequestBody Map<String, Integer> body) {
        return jobService.runOnce(body.get("id"));
    }
}
