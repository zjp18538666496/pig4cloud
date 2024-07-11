package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {
    @Select("SELECT MAX(id) FROM sys_menu WHERE ${ew.sqlSegment}")
    Integer getMaxAgeByCondition(@Param("ew") Wrapper<MenuEntity> wrapper);
}
