package com.pig4cloud.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.dept.entity.DeptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface DeptMapper extends BaseMapper<DeptEntity> {

    @Select("SELECT * FROM sys_dept ORDER BY sort ASC, id ASC")
    List<DeptEntity> selectAllOrdered();

    /**
     * 按租户过滤查询（tenantId为空查全部）；超管跨租户查看部门树时使用，
     * 普通用户查询即使不传tenantId也会被租户拦截器自动限制在本租户
     */
    @Select({
            "<script>",
            "SELECT * FROM sys_dept",
            "<where>",
            "  <if test='tenantId != null'>",
            "    AND tenant_id = #{tenantId}",
            "  </if>",
            "</where>",
            "ORDER BY sort ASC, id ASC",
            "</script>"
    })
    List<DeptEntity> selectListOrdered(@Param("tenantId") Integer tenantId);
}
