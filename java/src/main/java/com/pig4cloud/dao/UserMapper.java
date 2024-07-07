package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    UserEntity selectUserByUsername(String username);

    @Select("""
            SELECT
                sys_user.*,
                GROUP_CONCAT(DISTINCT sys_role.role_code ORDER BY sys_role.role_code SEPARATOR ',') AS role_codes,
                GROUP_CONCAT(DISTINCT sys_role.role_name ORDER BY sys_role.role_name SEPARATOR ',') AS role_names
            FROM
                sys_user
            LEFT JOIN user_role ON sys_user.id = user_role.user_id
            LEFT JOIN sys_role ON user_role.role_id = sys_role.id
            GROUP BY
              sys_user.id
            LIMIT ${pageSize} OFFSET ${page};
            """)
    List<Map<String, Object>> selectPage(@Param("pageSize") long pageSize, @Param("page") long page);

    @Select("""
            SELECT
                 COUNT(DISTINCT sys_user.id)
             FROM
                 sys_user
             LEFT JOIN user_role ON sys_user.id = user_role.user_id
             LEFT JOIN sys_role ON user_role.role_id = sys_role.id
             """)
    int selectUserList2Count();

    @Select("""
        
    """)
    int updateRole();
}
