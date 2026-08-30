package com.pig4cloud.log.dto;

import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogQueryDto extends BasePageQuery {

    /**
     * 操作人，模糊匹配，空则查全部
     */
    private String username = "";

    /**
     * 租户筛选：仅超管可传（普通用户由Controller强制为本租户）；空=本租户(超管为全部)
     */
    private Integer tenantId;
}
