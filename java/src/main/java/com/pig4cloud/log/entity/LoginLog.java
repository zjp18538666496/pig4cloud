package com.pig4cloud.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 登录日志（MongoDB login_log集合）：登录成功与失败都记录
 */
@Data
@Document("login_log")
@CompoundIndex(def = "{'createTime': -1}")
public class LoginLog {

    @Id
    private String id;

    private String username;

    private String ip;

    /**
     * 是否登录成功
     */
    private Boolean success;

    /**
     * 失败原因/成功描述
     */
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
