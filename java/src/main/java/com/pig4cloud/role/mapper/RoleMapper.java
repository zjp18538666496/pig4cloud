package com.pig4cloud.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.role.entity.RoleEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {

    @Insert("INSERT INTO sys_role (role_code, role_name, description, tenant_id) VALUES (#{role_code}, #{role_name}, #{description}, #{tenant_id})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoleEntity roleEntity);

    @Select("""
            SELECT
                r.*,
                IFNULL(GROUP_CONCAT(DISTINCT m.id ORDER BY m.id SEPARATOR ','), '') AS menu_codes,
                IFNULL(GROUP_CONCAT(DISTINCT m.menu_name ORDER BY m.id SEPARATOR ','), '') AS menu_names
            FROM
                sys_role r
            LEFT JOIN role_menu rm ON r.id = rm.role_id
            LEFT JOIN sys_menu m ON m.id = rm.menu_id AND m.type != '0'
            GROUP BY
                r.id
            LIMIT #{pageSize} OFFSET #{page};
            """)
    List<Map<String, Object>> selectList1(@Param("roleName") String roleName, @Param("pageSize") long pageSize, @Param("page") long page);

    @Select("""
            SELECT
                COUNT(DISTINCT r.id)
            FROM
                sys_role r
            LEFT JOIN role_menu rm ON r.id = rm.role_id
            LEFT JOIN sys_menu m ON m.id = rm.menu_id
            """)
    long selectUserList2Count();
}
