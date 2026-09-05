package com.pig4cloud.export.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 异步导出任务（下载中心）：submit创建任务，异步写Excel经StorageService落存储，
 * 下载中心查询并下载（本地/FTP/S3均支持）
 */
@Data
@TableName("sys_export_task")
public class SysExportTaskEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务标题（如"用户列表导出"）
     */
    private String title;

    /**
     * 类型(user)
     */
    private String task_type;

    /**
     * 状态(0处理中1成功2失败)
     */
    private String status;

    /**
     * 文件存储路径（StorageService按app.storage.type读取）
     */
    private String file_path;

    /**
     * 导出行数
     */
    private Integer total;

    /**
     * 失败原因
     */
    private String message;

    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finish_time;
}
