package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.RoleMapper;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.UserDto;
import com.pig4cloud.entity.RoleEntity;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.service.UserService;
import com.pig4cloud.util.verify.VerifyResult;
import com.pig4cloud.util.verify.VerifyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

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
            userEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
            int rows = userMapper.insert(userEntity);
            return new ResponseImpl(200, rows > 0 ? "注册成功" : "注册失败", null);
        } else {
            return new ResponseImpl(-200, verify.getMessage(), null);
        }
    }

    @Override
    public Response updateUser(UserEntity userEntity) {
//        verifyUser.setUsername(userEntity.getUsername());
//        verifyUser.setPassword(userEntity.getPassword());
//        VerifyResult verify = verifyUser.updateVerify();
//        if (verify.isValid()) {
//            String password = userEntity.getPassword();
//            password = new BCryptPasswordEncoder().encode(password);
//            userEntity.setPassword(password);
//            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
//            updateWrapper.eq("username", userEntity.getUsername())
//                    .set("password", password)
//                    .set("name", userEntity.getName())
//                    .set("email", userEntity.getEmail())
//                    .set("mobile", userEntity.getMobile())
//                    .set("update_time", new Timestamp(System.currentTimeMillis()));
//
//            int rows = userMapper.update(null, updateWrapper);
//            return new ResponseImpl(200, rows > 0 ? "更新成功" : "更新失败", userEntity);
//        } else {
//            return new ResponseImpl(-200, verify.getMessage(), null);
//        }
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        userEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        updateWrapper.eq("username", userEntity.getUsername())
                .set("name", userEntity.getName())
                .set("email", userEntity.getEmail())
                .set("mobile", userEntity.getMobile())
                .set("update_time", userEntity.getUpdateTime());

        int rows = userMapper.update(null, updateWrapper);
        return new ResponseImpl(200, rows > 0 ? "更新成功" : "更新失败", userEntity);
    }

    @Override
    public Response deleteUser(UserEntity userEntity) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userEntity.getUsername());
        int rows = userMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public Response getUserLists(UserDto userDto) {
        long page = userDto.getPage();
        long pageSize = userDto.getPageSize();
        List<Map<String, Object>> list = userMapper.selectPage(pageSize, page - 1);
        long total = userMapper.selectUserList2Count();
        Response response = new ResponseImpl(200, "获取数据成功", null);
        response.pagination(list, total, pageSize, page);
        return response;
    }

    @Override
    public Response updatePassword(UserEntity userEntity) {
        return null;
    }

    @Override
    public Response resetPassword(UserEntity userEntity) {
        return null;
    }

    @Override
    public Response updateUser1(Map<String, Object> map) {
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", map.get("username"))
                .set("name",  map.get("name"))
                .set("email", map.get("email"))
                .set("mobile", map.get("mobile"))
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        int rows = userMapper.update(null, updateWrapper);
        if (rows > 0) {
//            String[] role_codes = map.get("role_codes").toString().split(",");
//            for (String role_code : role_codes) {
//                RoleMapper roleMapper;
//                QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
//                RoleUserEntity roleUser = new RoleUserEntity();
//                roleUser.setUsername(map.get("username").toString());
//                roleUser.setRoleCode(role_code);
//                userMapper.insert(roleUser);
//            }
            return new ResponseImpl(200, "更新成功", null);
        }

        return null;
    }

    @Override
    public Response getUser(String username) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return new ResponseImpl(200, "请求成功", userMapper.selectOne(wrapper));
    }
}
