package com.pig4cloud.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagingQuery{
    Long page;
    Long pageSize;
    String roleName;
}
