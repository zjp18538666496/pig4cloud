package com.pig4cloud.util.verify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

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

    public VerifyResult verify() {
        // 校验用户名长度和是否包含汉字
        if (!isValidUsername(username)) {
            return VerifyResult.invalid("用户名必须由4到12位字母或数字组成，不能包含汉字");
        }

        // 校验密码长度和复杂度（字母加数字）
        if (!isValidPassword(password)) {
            return VerifyResult.invalid("密码必须由6到18位字母和数字组成");
        }
        return VerifyResult.valid("校验通过");
    }

    public VerifyResult creactVerify() {
        VerifyResult verify = verify();
        if (!verify().isValid()) {
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

    public VerifyResult updateVerify() {
        VerifyResult verify = verify();
        if (!verify().isValid()) {
            return VerifyResult.invalid(verify.getMessage());
        }

        // 校验密码是否和之前一样
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        UserEntity user = userMapper.selectOne(queryWrapper);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(user.getPassword(), password)) {
            return VerifyResult.invalid("密码不能和之前一样");
        }
        return VerifyResult.valid("校验通过");
    }

    private boolean isValidUsername(String username) {
        // 用户名长度必须在4到12位，并且不能包含汉字
        return Pattern.matches("^[a-zA-Z0-9]{4,12}$", username);
    }

    private boolean isValidPassword(String password) {
        // 密码长度必须在6到18位，并且必须包含至少一个字母和一个数字
        return Pattern.matches("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{6,18}$", password);
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

