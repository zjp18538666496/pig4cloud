package com.pig4cloud.export.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.export.entity.SysExportTaskEntity;
import com.pig4cloud.export.mapper.SysExportTaskMapper;
import com.pig4cloud.file.service.StorageService;
import com.pig4cloud.user.dto.UserDto;
import com.pig4cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步导出中心：提交任务（同步完成数据查询以保留数据权限上下文）→ 异步写Excel经
 * StorageService落存储（本地/FTP/S3均支持）→ 下载中心查询与下载。
 * 非超管只能看到/下载自己的任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final SysExportTaskMapper taskMapper;
    private final UserService userService;
    private final StorageService storageService;
    private final AsyncExportWriter asyncExportWriter;

    /**
     * 提交用户导出任务：数据行同步构建（保留数据权限），Excel异步生成
     */
    public R<Long> submitUserExport(UserDto dto, String createBy) {
        // 数据查询在提交线程完成（携带登录用户的数据权限上下文）
        List<Map<String, Object>> rows = userService.exportRows(dto);
        List<List<String>> data = rows.stream().map(row -> List.of(
                str(row.get("username")), str(row.get("name")), str(row.get("dept_name")),
                str(row.get("tenant_name")), str(row.get("mobile")), str(row.get("email")),
                str(row.get("role_names")), str(row.get("create_time")), str(row.get("last_login_time")))).toList();

        SysExportTaskEntity task = new SysExportTaskEntity();
        task.setTitle("用户列表导出（" + data.size() + "行）");
        task.setTask_type("user");
        task.setStatus("0");
        task.setTotal(data.size());
        task.setCreate_by(createBy);
        task.setCreate_time(new Date());
        taskMapper.insert(task);
        asyncExportWriter.write(task.getId(), "用户列表",
                java.util.List.of("账号", "昵称", "部门", "租户", "手机号", "邮箱", "角色", "创建时间", "最后登录"), data);
        return R.ok("任务已提交，请稍后到导出中心下载", task.getId() == null ? null : task.getId().longValue());
    }


    /**
     * 下载任务文件：非超管仅限本人任务
     */
    public InputStream download(Integer taskId, String currentUsername) {
        SysExportTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        boolean isSuper = UserContext.isSuperTenant();
        if (!isSuper && (currentUsername == null || !currentUsername.equals(task.getCreate_by()))) {
            throw new BizException(403, "只能下载自己创建的导出文件");
        }
        if (!"1".equals(task.getStatus()) || task.getFile_path() == null) {
            throw new BizException("任务未完成或已失败");
        }
        try {
            return storageService.download(task.getFile_path());
        } catch (Exception ex) {
            throw new BizException("文件读取失败：" + ex.getMessage());
        }
    }

    public R<PageResult<SysExportTaskEntity>> getLists(BasePageQuery dto, String currentUsername) {
        boolean isSuper = UserContext.isSuperTenant();
        QueryWrapper<SysExportTaskEntity> wrapper = new QueryWrapper<SysExportTaskEntity>()
                .orderByDesc("id");
        if (!isSuper) {
            wrapper.eq("create_by", currentUsername == null ? "" : currentUsername);
        }
        Page<SysExportTaskEntity> result = taskMapper.selectPage(
                new Page<>(Math.max(1, dto.getPage()), Math.max(1, dto.getPageSize())), wrapper);
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(),
                dto.getPageSize(), dto.getPage()));
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
