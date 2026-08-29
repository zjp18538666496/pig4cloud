package com.pig4cloud.menu.dto;

import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuDto extends BasePageQuery {

    private String menu_name;
}
