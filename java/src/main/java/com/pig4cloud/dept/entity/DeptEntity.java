package com.pig4cloud.dept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * 部门：租户内树形组织架构（sys_dept带tenant_id列，租户拦截器自动隔离）
 */
@Data
@TableName("sys_dept")
public class DeptEntity {

    /**
     * 部门id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 父部门id（0为根）
     */
    private Integer parent_id;

    /**
     * 部门名称
     */
    private String dept_name;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 租户id
     */
    private Integer tenant_id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp update_time;

    /**
     * 子部门（树形展示用，非表字段）
     */
    @TableField(exist = false)
    private List<DeptEntity> children;
}
