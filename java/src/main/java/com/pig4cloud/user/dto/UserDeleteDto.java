package com.pig4cloud.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDeleteDto {

    @NotBlank(message = "用户名不能为空")
    private String username;
}
