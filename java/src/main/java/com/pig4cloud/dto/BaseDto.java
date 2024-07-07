package com.pig4cloud.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BaseDto {
    Long page = -1L;
    Long pageSize = -1L;
}
