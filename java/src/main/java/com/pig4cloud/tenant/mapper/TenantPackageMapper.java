package com.pig4cloud.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.tenant.entity.TenantPackageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface TenantPackageMapper extends BaseMapper<TenantPackageEntity> {
}
