package com.pig4cloud.util.verify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Getter
@Setter
@Component
public class VerifyUser {
    @Autowired
    private UserMapper userMapper;

    private String username;
    private String password;

    public VerifyUser() {

    }

    public VerifyUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private boolean isValidUsername(String username) {
        // 用户名长度必须在4到12位，并且不能包含汉字
        return Pattern.matches("^[a-zA-Z0-9]{4,12}$", username);
    }

    private boolean isValidPassword(String password) {
        // 密码长度必须在4到18位
        return Pattern.matches("^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>~`\\\\/\\[\\]\\-_+=;']{4,18}$", password);
    }

    public VerifyResult verify() {
        // 校验用户名长度和是否包含汉字
        if (!isValidUsername(username)) {
            return VerifyResult.invalid("请输入4到12位的用户名，支持字母合数字");
        }
        // 校验密码长度和复杂度（字母加数字）
        if (!isValidPassword(password)) {
            return VerifyResult.invalid("请输入4到18位的密码，支持字母、数字和特殊字符");
        }
        return VerifyResult.valid("校验通过");
    }

    public VerifyResult creactVerify() {
        VerifyResult verify = verify();
        if (!verify().getValid()) {
            return VerifyResult.invalid(verify.getMessage());
        }
        // 构建查询条件，查询数据库中是否存在指定用户名的用户
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        UserEntity user = userMapper.selectOne(queryWrapper);

        // 检查是否找到了用户，并且密码匹配
        if (user != null && user.getUsername() != null) {
            return VerifyResult.invalid("用户已存在");
        } else {
            return VerifyResult.valid("用户不存在");
        }
    }

    public VerifyResult updatePasswordVerify(String oldPassword, String newPassword ,String dbPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!isValidPassword(oldPassword) || !isValidPassword(newPassword)) {
            return VerifyResult.invalid("请输入4到18位的密码，支持字母、数字和特殊字符");
        }
        if(!encoder.matches(oldPassword, dbPassword)){
            return VerifyResult.invalid("密码不正确");
        }
        if (encoder.matches(newPassword, dbPassword)) {
            return VerifyResult.invalid("密码不能和之前一样");
        }
        return VerifyResult.valid("校验通过");
    }
}

