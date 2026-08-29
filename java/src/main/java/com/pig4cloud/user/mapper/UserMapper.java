package com.pig4cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.user.entity.UserEntity;
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
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    UserEntity selectUserByUsername(String username);

    @Select("""
            SELECT
                u.id,
                u.name,
                u.username,
                u.mobile,
                u.email,
                DATE_FORMAT(u.create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                DATE_FORMAT(u.update_time, '%Y-%m-%d %H:%i:%s') AS update_time,
                DATE_FORMAT(u.last_login_time, '%Y-%m-%d %H:%i:%s') AS last_login_time,
                IFNULL(GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_code SEPARATOR ','), '') AS role_codes,
                IFNULL(GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.role_name SEPARATOR ','), '') AS role_names
            FROM
                sys_user u
            LEFT JOIN user_role ur ON u.id = ur.user_id
            LEFT JOIN sys_role r ON ur.role_id = r.id
            GROUP BY
                u.id
            LIMIT #{pageSize} OFFSET #{page};
            """)
    List<Map<String, Object>> selectPage(@Param("pageSize") long pageSize, @Param("page") long page);

    @Select("""
            SELECT
                 COUNT(DISTINCT u.id)
             FROM
                 sys_user u
             LEFT JOIN user_role ur ON u.id = ur.user_id
             LEFT JOIN sys_role r ON ur.role_id = r.id
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
    List<Map<String, Object>> selectRoleIdsByCodes(@Param("roleCodes") List<String> roleCodes);

    @Insert({
            "<script>",
            "INSERT INTO user_role (user_id, role_id) VALUES ",
            "<foreach collection='userRoles' item='userRole' separator=','>",
            "(#{userRole.user_id}, #{userRole.role_id})",
            "</foreach>",
            "</script>"
    })
    int insertUserRoles(@Param("userRoles") List<Map<String, Object>> userRoles);

    @Delete("""
            DELETE FROM user_role WHERE user_id = #{userId}
            """)
    int deleteUserRoles(Long userId);
}
