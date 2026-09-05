package com.pig4cloud.notify.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 通知发送记录（MongoDB notify_log集合）：每次渠道投递落一条，随log.retention-days自动清理
 */
@Data
@Document("notify_log")
@CompoundIndex(def = "{'channelId': 1, 'createTime': -1}")
public class NotifyLog {

    @Id
    private String id;

    private Integer channelId;

    private String channelName;

    /**
     * 渠道类型(email/webhook/dingtalk/wecom/feishu)
     */
    private String channelType;

    /**
     * 模板编码（测试发送为test）
     */
    private String templateCode;

    private String title;

    /**
     * 接收目标（邮箱/ webhook地址）
     */
    private String receiver;

    private Boolean success;

    /**
     * 失败原因/响应摘要
     */
    private String message;

    private Long costMs;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
