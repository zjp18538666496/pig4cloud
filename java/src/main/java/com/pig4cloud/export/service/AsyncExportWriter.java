package com.pig4cloud.export.service;

import com.alibaba.excel.EasyExcel;
import com.pig4cloud.export.entity.SysExportTaskEntity;
import com.pig4cloud.export.mapper.SysExportTaskMapper;
import com.pig4cloud.file.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

/**
 * 导出Excel异步写组件：独立Bean保证@Async生效（同类自调用不走代理）。
 * 内存生成xlsx后经StorageService落存储（本地/FTP/S3）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncExportWriter {

    private final SysExportTaskMapper taskMapper;
    private final StorageService storageService;

    @Async
    public void write(Integer taskId, String sheetName, List<String> headers, List<List<String>> data) {
        SysExportTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<List<String>> head = headers.stream().map(List::of).toList();
            EasyExcel.write(out).head(head).sheet(sheetName).doWrite(data);
            String path = "export/" + java.util.UUID.randomUUID() + ".xlsx";
            storageService.uploadFile(path, new ByteArrayInputStream(out.toByteArray()), out.size());
            task.setStatus("1");
            task.setFile_path(path);
            task.setFinish_time(new Date());
            taskMapper.updateById(task);
            log.info("导出任务{}完成：{}行", taskId, data.size());
        } catch (Exception ex) {
            log.error("导出任务{}失败", taskId, ex);
            String message = ex.getMessage() == null ? "导出失败" : ex.getMessage();
            task.setStatus("2");
            task.setMessage(message.substring(0, Math.min(490, message.length())));
            task.setFinish_time(new Date());
            taskMapper.updateById(task);
        }
    }
}
