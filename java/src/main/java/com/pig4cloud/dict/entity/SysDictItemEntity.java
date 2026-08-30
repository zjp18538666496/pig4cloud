package com.pig4cloud.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典项
 */
@Data
@TableName("sys_dict_item")
public class SysDictItemEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属字典id
     */
    private Integer dict_id;

    /**
     * 显示标签
     */
    private String label;

    /**
     * 键值
     */
    private String value;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态(0停用1启用)
     */
    private String status;
}
