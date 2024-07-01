package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.service.UserService;
import com.pig4cloud.util.verify.VerifyResult;
import com.pig4cloud.util.verify.VerifyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerifyUser verifyUser;

    public Response createUser(UserEntity userEntity) {
        verifyUser.setUsername(userEntity.getUsername());
        verifyUser.setPassword(userEntity.getPassword());
        VerifyResult verify = verifyUser.creactVerify();
        if (verify.isValid()) {
            String password = userEntity.getPassword();
            password = new BCryptPasswordEncoder().encode(password);
            userEntity.setPassword(password);
            userEntity.setName(userEntity.getUsername());
            int rows = userMapper.insert(userEntity);
            return new ResponseImpl(200, rows > 0 ? "注册成功" : "注册失败", null);
        } else {
            return new ResponseImpl(-200, verify.getMessage(), null);
        }
    }

    @Override
    public Response updateUser(UserEntity userEntity) {
        verifyUser.setUsername(userEntity.getUsername());
        verifyUser.setPassword(userEntity.getPassword());
        VerifyResult verify = verifyUser.updateVerify();
        if (verify.isValid()) {
            String password = userEntity.getPassword();
            password = new BCryptPasswordEncoder().encode(password);
            userEntity.setPassword(password);
            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("username", userEntity.getUsername())
                    .set("password", password)
                    .set("name", userEntity.getName())
                    .set("email", userEntity.getEmail())
                    .set("mobile", userEntity.getMobile());

            int rows = userMapper.update(null, updateWrapper);
            return new ResponseImpl(200, rows > 0 ? "更新成功" : "更新失败", userEntity);
        } else {
            return new ResponseImpl(-200, verify.getMessage(), null);
        }
    }

    @Override
    public Response deleteUser(UserEntity userEntity) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userEntity.getUsername());
        int rows = userMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    public Response getUserLists() {
        return new ResponseImpl(200, "请求成功", userMapper.selectList(null));
    }

    @Override
    public Response getUser(String username) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return new ResponseImpl(200, "请求成功", userMapper.selectOne(wrapper));
    }
}
