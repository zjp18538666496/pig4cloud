package com.pig4cloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.auth.entity.AuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询用户权限点，由两部分合并：
 * 1. 角色编码(sys_role.role_code)——角色级权限，如root
 * 2. 按钮菜单的权限标识(sys_menu.perms, type=2)——操作级权限，如user:remove
 * 供登录时写入token及@PreAuthorize/前端v-permission使用
 */
@Repository
@Mapper
public interface AuthorityMapper extends BaseMapper<AuthorityEntity> {

    @Select("""
            SELECT r.id,
                   r.role_code  AS name,
                   r.role_name  AS description
            FROM sys_user u
            INNER JOIN user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE u.username = #{username}
            GROUP BY r.id, r.role_code, r.role_name
            UNION
            SELECT m.id,
                   m.perms      AS name,
                   m.menu_name  AS description
            FROM sys_user u
            INNER JOIN user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            INNER JOIN role_menu rm ON rm.role_id = r.id
            INNER JOIN sys_menu m ON m.id = rm.menu_id
            WHERE u.username = #{username}
              AND m.type = '2'
              AND m.perms IS NOT NULL
              AND m.perms != ''
            GROUP BY m.id, m.perms, m.menu_name
            """)
    List<AuthorityEntity> selectAuthorityByUsername(String username);
}
