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
    /**
     * 获取父级菜单下的最大ID
     */
    @Select("SELECT MAX(id) FROM sys_menu WHERE ${ew.sqlSegment}")
    Integer getMaxAgeByCondition(@Param("ew") Wrapper<MenuEntity> wrapper);

    @Delete("""
            DELETE FROM role_menu WHERE role_menu.role_id = #{role_id}
            """)
    int deleteMenus(int role_id);

    @Insert({
            "<script>",
            "<if test='userRoles != null and !userRoles.isEmpty()'>",
            "INSERT INTO role_menu (menu_id, role_id) VALUES ",
            "<foreach collection='userRoles' item='userRole' separator=','>",
            "(#{userRole.menu_id}, #{userRole.role_id})",
            "</foreach>",
            "</if>",
            "</script>"
    })
    int insertUserRoles(@Param("userRoles") List<Map<String, Object>> userRoles);

    @Select("SELECT DISTINCT" +
            "  m.id AS id1," +
            "  m.* " +
            "FROM" +
            "  sys_menu m" +
            "  LEFT JOIN role_menu ON m.id = role_menu.menu_id" +
            "  LEFT JOIN sys_role r ON r.id = role_menu.role_id" +
            "  LEFT JOIN user_role ON r.id = user_role.role_id" +
            "  LEFT JOIN sys_user u ON u.id = user_role.user_id " +
            "WHERE" +
            "  u.username = #{username} " +
            "ORDER BY" +
            "  m.id ASC;")
    List<MenuEntity> selectMenuLists(String username);
}
