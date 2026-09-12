package com.pig4cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.user.entity.UserPasswordHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface UserPasswordHistoryMapper extends BaseMapper<UserPasswordHistoryEntity> {
}
