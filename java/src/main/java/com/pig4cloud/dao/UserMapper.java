package com.pig4cloud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.entity.UserEntity;
import org.apache.ibatis.annotations.*;
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
                sys_user.id,
                sys_user.name,
                sys_user.username,
                sys_user.mobile,
                sys_user.email,
                DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time,
                DATE_FORMAT(last_login_time, '%Y-%m-%d %H:%i:%s') AS last_login_time,
                IFNULL(GROUP_CONCAT(DISTINCT sys_role.role_code ORDER BY sys_role.role_code SEPARATOR ','), '') AS role_codes,
                IFNULL(GROUP_CONCAT(DISTINCT sys_role.role_name ORDER BY sys_role.role_name SEPARATOR ','), '') AS role_names
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


    @Select({
            "<script>",
            "SELECT id FROM sys_role WHERE role_code IN",
            "<foreach item='item' index='index' collection='roleCodes' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> selectUserRoles(@Param("roleCodes") List<String> roleCodes);

    @Insert({
            "<script>",
            "INSERT INTO user_role (user_id, role_id) VALUES ",
            "<foreach collection='userRoles' item='userRole' separator=','>",
            "(#{userRole.user_id}, #{userRole.id})",
            "</foreach>",
            "</script>"
    })
    int insertUserRoles(@Param("userRoles") List<Map<String, Object>> userRoles);

    @Delete("""
            DELETE FROM user_role WHERE user_id = #{userId}
            """)
    int deleteUserRoles(Long userId);
}
