package com.pig4cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.user.entity.UserPasswordHistoryEntity;
import com.pig4cloud.user.mapper.UserPasswordHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 密码历史防重（等保2.0）：改密时不能与最近N次用过的密码相同（N=pwd.history-count，0=不校验），
 * 密码修改成功后把旧密码哈希记入历史
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    private final UserPasswordHistoryMapper historyMapper;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;

    private int historyCount() {
        return configService.getInt("pwd.history-count", 3);
    }

    /**
     * 校验新密码不与最近N次重复，重复抛业务异常
     */
    public void assertNotReused(Integer userId, String rawNewPassword) {
        int count = historyCount();
        if (count <= 0 || userId == null) {
            return;
        }
        List<UserPasswordHistoryEntity> latest = historyMapper.selectList(
                new QueryWrapper<UserPasswordHistoryEntity>()
                        .eq("user_id", userId)
                        .orderByDesc("id")
                        .last("LIMIT " + count));
        for (UserPasswordHistoryEntity history : latest) {
            if (passwordEncoder.matches(rawNewPassword, history.getPassword())) {
                throw new BizException("新密码不能与最近" + count + "次使用过的密码相同");
            }
        }
    }

    /**
     * 记录旧密码哈希并裁剪历史（保留最近N次）
     */
    public void record(Integer userId, String oldEncodedPassword) {
        int count = historyCount();
        if (count <= 0 || userId == null) {
            return;
        }
        UserPasswordHistoryEntity entity = new UserPasswordHistoryEntity();
        entity.setUser_id(userId);
        entity.setPassword(oldEncodedPassword);
        entity.setCreate_time(new Date());
        historyMapper.insert(entity);
        List<UserPasswordHistoryEntity> all = historyMapper.selectList(
                new QueryWrapper<UserPasswordHistoryEntity>()
                        .eq("user_id", userId)
                        .orderByDesc("id"));
        if (all.size() > count) {
            for (UserPasswordHistoryEntity stale : all.subList(count, all.size())) {
                historyMapper.deleteById(stale.getId());
            }
        }
    }
}
