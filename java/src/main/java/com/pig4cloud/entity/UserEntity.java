package com.pig4cloud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("sys_user")
public class UserEntity {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String mobile;
    private String email;
    private Timestamp create_time;
    private Timestamp update_time;
    private Timestamp last_login_time;
}
