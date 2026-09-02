package com.pig4cloud.notice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 公告创建/编辑请求
 */
@Getter
@Setter
public class NoticeDto {

    /**
     * 编辑时必传
     */
    private Integer id;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 128, message = "公告标题最长128个字符")
    private String title;

    @Size(max = 10000, message = "公告内容过长")
    private String content;

    /**
     * 状态(0草稿1发布2定时发布)
     */
    private String status = "0";

    /**
     * 定时发布时间（status=2时必填）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("publish_time")
    private Date publishTime;

    /**
     * 发布时是否同时发站内信给可见范围用户
     */
    @JsonProperty("send_message")
    private Boolean sendMessage;
}
