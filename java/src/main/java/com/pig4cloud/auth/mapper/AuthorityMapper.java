package com.pig4cloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.auth.entity.AuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询用户权限点。角色编码取自身角色（不沿父链继承），
 * 按钮权限标识(sys_menu.perms, type=2)由服务层先展开"自身角色+祖先角色"再按角色集合查询（子角色沿父链继承）
 */
@Repository
@Mapper
public interface AuthorityMapper extends BaseMapper<AuthorityEntity> {

    @Select({
            "<script>",
            "SELECT m.id,",
            "       m.perms      AS name,",
            "       m.menu_name  AS description",
            "FROM role_menu rm",
            "INNER JOIN sys_menu m ON m.id = rm.menu_id",
            "WHERE rm.role_id IN",
            "<foreach item='item' collection='roleIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "  AND m.type = '2'",
            "  AND m.perms IS NOT NULL",
            "  AND m.perms != ''",
            "GROUP BY m.id, m.perms, m.menu_name",
            "</script>"
    })
    List<AuthorityEntity> selectPermsByRoleIds(@Param("roleIds") List<Integer> roleIds);
}
