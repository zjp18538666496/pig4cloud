package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.AuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface AuthorityMapper extends BaseMapper<AuthorityEntity> {
    List<AuthorityEntity> selectAuthorityByUsername(String username);
}
