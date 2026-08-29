package com.pig4cloud.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface TenantMapper extends BaseMapper<TenantEntity> {
}
