package com.pig4cloud.menu.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.menu.entity.MenuEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 按角色id集合查菜单（含目录/页面/按钮）：服务层先展开"自身角色+祖先角色"再查询，
     * 实现子角色沿父链继承菜单；角色为空时不应调用本方法
     */
    @Select({
            "<script>",
            "SELECT DISTINCT m.* FROM sys_menu m",
            "INNER JOIN role_menu rm ON m.id = rm.menu_id",
            "WHERE rm.role_id IN",
            "<foreach item='item' collection='roleIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "ORDER BY m.id ASC",
            "</script>"
    })
    List<MenuEntity> selectMenusByRoleIds(@Param("roleIds") List<Integer> roleIds);
}
