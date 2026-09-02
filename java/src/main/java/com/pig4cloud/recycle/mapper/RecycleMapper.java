package com.pig4cloud.recycle.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 回收站：裸SQL绕过@TableLogic，专查/恢复/彻底删除软删除数据。
 * 租户隔离由MP租户拦截器自动追加（super全量）
 */
@Repository
@Mapper
public interface RecycleMapper {

    @Select("""
            SELECT u.id, u.username, u.name, u.mobile, u.email, u.tenant_id, u.delete_time,
                   t.tenant_name, 'user' AS item_type
            FROM sys_user u
            LEFT JOIN sys_tenant t ON u.tenant_id = t.id
            WHERE u.deleted = 1
            ORDER BY u.delete_time DESC
            """)
    List<Map<String, Object>> deletedUsers();

    @Select("""
            SELECT r.id, r.role_name, r.role_code, r.description, r.tenant_id, r.delete_time,
                   t.tenant_name, 'role' AS item_type
            FROM sys_role r
            LEFT JOIN sys_tenant t ON r.tenant_id = t.id
            WHERE r.deleted = 1
            ORDER BY r.delete_time DESC
            """)
    List<Map<String, Object>> deletedRoles();

    @Update("UPDATE sys_user SET deleted = 0, delete_time = NULL WHERE id = #{id} AND deleted = 1")
    int restoreUser(@Param("id") Integer id);

    @Update("UPDATE sys_role SET deleted = 0, delete_time = NULL WHERE id = #{id} AND deleted = 1")
    int restoreRole(@Param("id") Integer id);

    /**
     * 彻底删除（硬删除，FK级联清user_role/user_post）
     */
    @Delete("DELETE FROM sys_user WHERE id = #{id} AND deleted = 1")
    int purgeUser(@Param("id") Integer id);

    @Delete("DELETE FROM sys_role WHERE id = #{id} AND deleted = 1")
    int purgeRole(@Param("id") Integer id);
}
