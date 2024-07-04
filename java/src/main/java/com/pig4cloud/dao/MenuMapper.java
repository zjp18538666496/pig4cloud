package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {
}
