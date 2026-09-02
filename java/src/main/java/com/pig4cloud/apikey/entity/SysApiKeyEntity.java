package com.pig4cloud.apikey.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * Open API密钥（平台级，sys_api_key为拦截器忽略表）：
 * 第三方系统以请求头X-Api-Key调用/api/open/**接口，按scopes授权、按分钟限流
 */
@Data
@TableName("sys_api_key")
public class SysApiKeyEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 接入方名称
     */
    private String app_name;

    /**
     * API Key（请求头X-Api-Key）
     */
    private String api_key;

    /**
     * 授权范围（逗号分隔，如user:read,notice:read）
     */
    private String scopes;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 过期时间（空为永不过期）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expire_time;

    /**
     * 最后调用时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date last_used_time;

    /**
     * 创建人
     */
    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;
}
