package com.pig4cloud.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 密码历史（等保：防止改回最近N次用过的密码），存BCrypt哈希
 */
@Data
@TableName("sys_password_history")
public class UserPasswordHistoryEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer user_id;

    /**
     * 密码哈希（BCrypt）
     */
    private String password;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;
}
