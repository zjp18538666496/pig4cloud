package com.pig4cloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.auth.entity.AuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询用户权限。权限点=角色编码(role_code)，与@PreAuthorize中的hasAnyAuthority对应
 */
@Repository
@Mapper
public interface AuthorityMapper extends BaseMapper<AuthorityEntity> {

    @Select("""
            SELECT
                r.id,
                r.role_code AS name,
                r.role_name AS description
            FROM sys_user u
            INNER JOIN user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE u.username = #{username}
            GROUP BY r.id, r.role_code, r.role_name
            """)
    List<AuthorityEntity> selectAuthorityByUsername(String username);
}
