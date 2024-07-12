package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.MenuEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {
    @Select("SELECT MAX(id) FROM sys_menu WHERE ${ew.sqlSegment}")
    Integer getMaxAgeByCondition(@Param("ew") Wrapper<MenuEntity> wrapper);

    @Delete("""
            DELETE FROM role_menu WHERE role_menu.role_id = #{role_id}
            """)
    int deleteMenus(int role_id);

    @Insert({
            "<script>",
            "INSERT INTO role_menu (menu_id, role_id) VALUES ",
            "<foreach collection='userRoles' item='userRole' separator=','>",
            "(#{userRole.menu_id}, #{userRole.id})",
            "</foreach>",
            "</script>"
    })
    int insertUserRoles(@Param("userRoles") List<Map<String, Object>> userRoles);
}
