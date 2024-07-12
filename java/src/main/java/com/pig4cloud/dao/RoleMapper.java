package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
    @Select("""
            SELECT
                sys_role.*,
                IFNULL(GROUP_CONCAT(DISTINCT sys_menu.id ORDER BY sys_menu.id SEPARATOR ','), '') AS menu_codes
            FROM
                sys_role
            LEFT JOIN role_menu ON sys_role.id = role_menu.role_id
            LEFT JOIN sys_menu ON sys_menu.id = role_menu.menu_id
            
            GROUP BY
                sys_role.id
            LIMIT #{pageSize} OFFSET #{page};
            """)
    List<Map<String, Object>> selectList1(@Param("roleName") String roleName, @Param("pageSize") long pageSize, @Param("page") long page);
   // WHERE sys_role.role_name LIKE CONCAT('%', #{roleName}, '%')

    List<Map<String, Object>> getSelectList();
    @Select("""
            SELECT
                COUNT(DISTINCT sys_role.id)
            FROM
                sys_role
            LEFT JOIN role_menu ON sys_role.id = role_menu.role_id
            LEFT JOIN sys_menu ON sys_menu.id = role_menu.menu_id
            """)
    long selectUserList2Count();
}
