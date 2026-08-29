package com.pig4cloud.common.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询基类
 */
@Getter
@Setter
public class BasePageQuery {

    private Long page = -1L;
    private Long pageSize = -1L;
}
