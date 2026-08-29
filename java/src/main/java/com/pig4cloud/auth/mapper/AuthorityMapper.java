package com.pig4cloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.auth.entity.AuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface AuthorityMapper extends BaseMapper<AuthorityEntity> {

    @Select("""
            SELECT
                E.*
            FROM sys_user A
            INNER JOIN USER_ROLE B ON A.id = B.user_id
            INNER JOIN sys_role C ON B.role_id = C.id
            INNER JOIN role_permission D ON C.id = D.role_id
            INNER JOIN sys_permission E ON D.permission_id = E.id
            WHERE A.username = #{username}
            GROUP BY E.id
            """)
    List<AuthorityEntity> selectAuthorityByUsername(String username);
}
