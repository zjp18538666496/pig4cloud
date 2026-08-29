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
}
