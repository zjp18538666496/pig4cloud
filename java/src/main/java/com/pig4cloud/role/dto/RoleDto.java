package com.pig4cloud.role.dto;

import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDto extends BasePageQuery {

    private String roleName = "";
}
