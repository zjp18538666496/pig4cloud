package com.pig4cloud.profile;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 个人偏好：首页工作台自定义配置（卡片顺序与显隐JSON）按用户持久化
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserMapper userMapper;

    public String getWorkbenchConfig(String username) {
        UserEntity user = userMapper.selectUserByUsername(username);
        return user == null ? null : user.getWorkbench_config();
    }

    public void saveWorkbenchConfig(String username, String config) {
        UserEntity user = userMapper.selectUserByUsername(username);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (config != null && config.length() > 2000) {
            throw new BizException("配置过长");
        }
        UserEntity update = new UserEntity();
        update.setId(user.getId());
        update.setWorkbench_config(config);
        userMapper.updateById(update);
    }
}
