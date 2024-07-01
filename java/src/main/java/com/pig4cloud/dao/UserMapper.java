package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    UserEntity selectUserByUsername(String username);
    // 这里可以扩展自定义的查询方法
}
