package com.pig4cloud.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pig4cloud.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 用户视图对象：对外暴露的用户信息，不包含密码等敏感字段
 */
@Getter
@Setter
public class UserVO {

    private Integer id;
    private String username;
    private String name;
    private String mobile;
    private String email;
    private String avatar;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp update_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp last_login_time;

    public static UserVO from(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setName(entity.getName());
        vo.setMobile(entity.getMobile());
        vo.setEmail(entity.getEmail());
        vo.setAvatar(entity.getAvatar());
        vo.setCreate_time(entity.getCreate_time());
        vo.setUpdate_time(entity.getUpdate_time());
        vo.setLast_login_time(entity.getLast_login_time());
        return vo;
    }
}
