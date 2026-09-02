package com.pig4cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.user.entity.UserEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    UserEntity selectUserByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE email = #{email} LIMIT 1")
    UserEntity selectUserByEmail(String email);

    @Update("UPDATE sys_user SET last_login_time = NOW() WHERE username = #{username}")
    int updateLastLoginTime(@Param("username") String username);

    /**
     * 用户分页列表：联角色与部门；deptIds/selfId为数据权限过滤条件（均为空则不限制），
     * keyword模糊匹配用户名/姓名，filterDeptIds部门筛选(含子部门)，tenantId租户筛选(仅超管场景传入)
     */
    @Select({
            "<script>",
            "SELECT",
            "  u.id, u.name, u.username, u.mobile, u.email, u.dept_id, u.tenant_id, u.totp_enabled, d.dept_name, t.tenant_name,",
            "  DATE_FORMAT(u.create_time, '%Y-%m-%d %H:%i:%s') AS create_time,",
            "  DATE_FORMAT(u.update_time, '%Y-%m-%d %H:%i:%s') AS update_time,",
            "  DATE_FORMAT(u.last_login_time, '%Y-%m-%d %H:%i:%s') AS last_login_time,",
            "  IFNULL(GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_code SEPARATOR ','), '') AS role_codes,",
            "  IFNULL(GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.role_name SEPARATOR ','), '') AS role_names,",
            "  IFNULL(GROUP_CONCAT(DISTINCT p2.post_name ORDER BY p2.post_name SEPARATOR ','), '') AS post_names,",
            "  IFNULL(GROUP_CONCAT(DISTINCT up.post_id), '') AS post_ids",
            "FROM sys_user u",
            "LEFT JOIN user_role ur ON u.id = ur.user_id",
            "LEFT JOIN sys_role r ON ur.role_id = r.id",
            "LEFT JOIN sys_dept d ON u.dept_id = d.id",
            "LEFT JOIN sys_tenant t ON u.tenant_id = t.id",
            "LEFT JOIN user_post up ON u.id = up.user_id",
            "LEFT JOIN sys_post p2 ON up.post_id = p2.id",
            "<where>",
            "  <if test='deptIds != null and deptIds.size() > 0'>",
            "    <choose>",
            "      <when test='selfId != null'>",
            "        AND (u.dept_id IN <foreach item='item' collection='deptIds' open='(' separator=',' close=')'>#{item}</foreach> OR u.id = #{selfId})",
            "      </when>",
            "      <otherwise>",
            "        AND u.dept_id IN <foreach item='item' collection='deptIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "      </otherwise>",
            "    </choose>",
            "  </if>",
            "  <if test='(deptIds == null or deptIds.size() == 0) and selfId != null'>",
            "    AND u.id = #{selfId}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.name LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "  <if test='filterDeptIds != null and filterDeptIds.size() > 0'>",
            "    AND u.dept_id IN <foreach item='item' collection='filterDeptIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "  </if>",
            "  <if test='tenantId != null'>",
            "    AND u.tenant_id = #{tenantId}",
            "  </if>",
            "</where>",
            "GROUP BY u.id",
            "ORDER BY u.id",
            "LIMIT #{pageSize} OFFSET #{page}",
            "</script>"
    })
    List<Map<String, Object>> selectPage(@Param("pageSize") long pageSize, @Param("page") long page,
                                         @Param("deptIds") Collection<Integer> deptIds, @Param("selfId") Long selfId,
                                         @Param("keyword") String keyword,
                                         @Param("filterDeptIds") Collection<Integer> filterDeptIds,
                                         @Param("tenantId") Integer tenantId);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_user u",
            "<where>",
            "  <if test='deptIds != null and deptIds.size() > 0'>",
            "    <choose>",
            "      <when test='selfId != null'>",
            "        AND (u.dept_id IN <foreach item='item' collection='deptIds' open='(' separator=',' close=')'>#{item}</foreach> OR u.id = #{selfId})",
            "      </when>",
            "      <otherwise>",
            "        AND u.dept_id IN <foreach item='item' collection='deptIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "      </otherwise>",
            "    </choose>",
            "  </if>",
            "  <if test='(deptIds == null or deptIds.size() == 0) and selfId != null'>",
            "    AND u.id = #{selfId}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.name LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "  <if test='filterDeptIds != null and filterDeptIds.size() > 0'>",
            "    AND u.dept_id IN <foreach item='item' collection='filterDeptIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "  </if>",
            "  <if test='tenantId != null'>",
            "    AND u.tenant_id = #{tenantId}",
            "  </if>",
            "</where>",
            "</script>"
    })
    int selectUserList2Count(@Param("deptIds") Collection<Integer> deptIds, @Param("selfId") Long selfId,
                             @Param("keyword") String keyword,
                             @Param("filterDeptIds") Collection<Integer> filterDeptIds,
                             @Param("tenantId") Integer tenantId);

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

    @Select("""
            SELECT DISTINCT u.username
            FROM sys_user u
            JOIN user_role ur ON u.id = ur.user_id
            WHERE ur.role_id = #{roleId}
            """)
    List<String> selectUsernamesByRoleId(Integer roleId);

    @Delete("DELETE FROM user_post WHERE user_id = #{userId}")
    int deleteUserPosts(Long userId);

    @Insert({
            "<script>",
            "INSERT INTO user_post (user_id, post_id) VALUES ",
            "<foreach collection='posts' item='post' separator=','>",
            "(#{post.user_id}, #{post.post_id})",
            "</foreach>",
            "</script>"
    })
    int insertUserPosts(@Param("posts") List<Map<String, Object>> posts);
}
