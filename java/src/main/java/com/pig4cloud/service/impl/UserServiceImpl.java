package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.RoleMapper;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.UserDto;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.service.UserService;
import com.pig4cloud.util.file.FileUtils;
import com.pig4cloud.util.verify.VerifyResult;
import com.pig4cloud.util.verify.VerifyUser;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerifyUser verifyUser;

    @Autowired
    RoleMapper roleMapper;

    public Response createUser(UserEntity userEntity) {
        verifyUser.setUsername(userEntity.getUsername());
        verifyUser.setPassword(userEntity.getPassword());
        VerifyResult verify = verifyUser.creactVerify();
        if (verify.isValid()) {
            String password = userEntity.getPassword();
            password = new BCryptPasswordEncoder().encode(password);
            userEntity.setPassword(password);
            userEntity.setName(userEntity.getUsername());
            userEntity.setCreate_time(new Timestamp(System.currentTimeMillis()));
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
        userEntity.setUpdate_time(new Timestamp(System.currentTimeMillis()));
        updateWrapper.eq("username", userEntity.getUsername())
                .set("name", userEntity.getName())
                .set("email", userEntity.getEmail())
                .set("mobile", userEntity.getMobile())
                .set("update_time", userEntity.getUpdate_time());

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
    @Transactional
    public Response updateUser1(Map<String, Object> map) {
        try {
            Long userId = ((Number) map.get("id")).longValue();

            // 更新用户信息
            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userId)
                    .set("username", map.get("username"))
                    .set("name", map.get("name"))
                    .set("email", map.get("email"))
                    .set("mobile", map.get("mobile"))
                    .set("update_time", new Timestamp(System.currentTimeMillis()));
            int updateResult = userMapper.update(null, updateWrapper);

            // 更新角色信息
            if (updateResult > 0) {
                String roleCodesStr = map.get("role_codes").toString().trim();
                List<String> roleCodes = roleCodesStr.isEmpty() ? Collections.emptyList() : List.of(roleCodesStr.split(","));

                // 查询角色是否存在
                List<Map<String, Object>> roles = roleCodes.isEmpty() ? Collections.emptyList() : userMapper.selectUserRoles(roleCodes);
                roles.forEach(role -> role.put("user_id", userId));

                // 删除原有角色关联
                int deleteResult = userMapper.deleteUserRoles(userId);

                // 插入新的角色关联
                if (!roles.isEmpty()) {
                    int insertResult = userMapper.insertUserRoles(roles);
                    if (deleteResult >= 0 && insertResult > 0) {
                        return new ResponseImpl(200, "更新成功", null);
                    } else {
                        throw new RuntimeException("更新用户角色关联失败");
                    }
                } else {
                    // 如果角色列表为空，直接返回更新成功
                    return new ResponseImpl(200, "更新成功", null);
                }
            } else {
                throw new RuntimeException("更新用户信息失败");
            }
        } catch (Exception e) {
            // 异常处理
            return new ResponseImpl(-200, "更新失败：" + e.getMessage(), null);
        }
    }

    @Override
    public Response updateAvatar(Map<String, Object> map) throws IOException {
        FileUtils fileUtils = new FileUtils();
        FTPClient ftpClient = new FTPClient();
        FTPServiceImpl ftpService = new FTPServiceImpl();
        Long userId = ((Number) map.get("id")).longValue();
        MultipartFile file = (MultipartFile) map.get("avatar");
        String remotePath = "/test/" + fileUtils.generateFilePath(file);
        ftpService.uploadFile(remotePath, file, ftpClient);
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .set("avatar", remotePath)
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        int updateResult = userMapper.update(null, updateWrapper);
        if (updateResult > 0) {
            return new ResponseImpl(200, "更新成功", null);
        }else {
            return new ResponseImpl(-200, "更新失败", null);
        }
    }


    @Override
    public Response getUser(String username) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return new ResponseImpl(200, "请求成功", userMapper.selectOne(wrapper));
    }
}
