package com.pig4cloud.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Open API调用日志（MongoDB open_api_log集合）：过滤器每次调用落一条，
 * 按log.retention-days随logCleanJob自动清理
 */
@Data
@Document("open_api_log")
@CompoundIndex(def = "{'keyId': 1, 'createTime': -1}")
public class OpenApiLog {

    @Id
    private String id;

    /**
     * API Key id（sys_api_key.id）
     */
    private Integer keyId;

    /**
     * 接入方名称（冗余，防Key删除后无法展示）
     */
    private String appName;

    private String method;

    /**
     * 请求路径（如/api/open/v1/users）
     */
    private String path;

    /**
     * 原始query串（不含签名头）
     */
    private String query;

    /**
     * 鉴权方式（simple/hmac）
     */
    private String authMode;

    /**
     * 业务结果（开放接口业务code=200为成功）
     */
    private Boolean success;

    /**
     * 响应业务code（未到业务层时为null，如401签名失败）
     */
    private Integer code;

    /**
     * 拒绝/失败原因（如"签名不匹配"、"时间戳过期"）
     */
    private String message;

    private String ip;

    /**
     * IP归属地（ip2region离线解析）
     */
    private String region;

    private Long costMs;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
