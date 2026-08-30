package com.pig4cloud.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.notice.entity.NoticeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface NoticeMapper extends BaseMapper<NoticeEntity> {

    /**
     * 可见公告：平台公告(tenant_id=0)或本租户公告；草稿(status=0)仅发布者侧管理接口可见
     */
    @Select("""
            SELECT * FROM sys_notice
            WHERE (tenant_id = 0 OR tenant_id = #{tenantId})
            ORDER BY id DESC
            """)
    List<NoticeEntity> selectVisible(@Param("tenantId") int tenantId);
}
