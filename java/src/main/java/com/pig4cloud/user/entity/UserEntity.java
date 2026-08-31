package com.pig4cloud.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("sys_user")
public class UserEntity {

    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 账号
     */
    private String username;

    /**
     * 密码(BCrypt哈希)
     */
    private String password;

    /**
     * 姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 租户id（0为平台层账号）
     */
    private Integer tenant_id;

    /**
     * 部门id（租户内组织架构）
     */
    private Integer dept_id;

    /**
     * 强制修改密码标记（租户管理员初始密码/管理员重置后置1，改密后清除）
     */
    private Integer force_pwd_change;

    /**
     * 密码最后修改时间（配合pwd.expire-days做密码过期）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp pwd_update_time;

    /**
     * TOTP两步认证密钥（Base32，未绑定为null）
     */
    private String totp_secret;

    /**
     * 是否开启两步认证
     */
    private Integer totp_enabled;

    /**
     * 备用恢复码（SHA256哈希，逗号分隔，一次性使用）
     */
    private String backup_codes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp update_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp last_login_time;
}
