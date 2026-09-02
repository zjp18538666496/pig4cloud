package com.pig4cloud.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pig4cloud.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

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

    /**
     * 是否强制修改密码（初始密码未改/密码过期），登录响应下发，前端弹不可关闭的改密弹窗
     */
    private Boolean forcePwdChange;

    /**
     * 是否需要引导开启两步认证（login.2fa-force-enabled开启且本人未绑定），登录响应下发
     */
    private Boolean force2fa;

    /**
     * 代理登录标记：值为发起代理的超管账号（仅代理登录时返回）
     */
    private String impersonator;

    /**
     * 权限点集合（角色编码+按钮操作权限），登录时填充，供前端v-permission使用
     */
    private List<String> permissions;

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
