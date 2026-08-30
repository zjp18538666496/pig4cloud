package com.pig4cloud.log.dto;

import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录日志查询：username模糊匹配，success为空查全部
 */
@Getter
@Setter
public class LoginLogQueryDto extends BasePageQuery {

    private String username = "";

    /**
     * 是否成功；null=全部
     */
    private Boolean success;

    /**
     * 租户筛选：仅超管可传（普通用户由Controller强制为本租户）；空=本租户(超管为全部)
     */
    private Integer tenantId;
}
