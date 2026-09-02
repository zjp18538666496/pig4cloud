package com.pig4cloud.apikey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.apikey.entity.SysApiKeyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysApiKeyMapper extends BaseMapper<SysApiKeyEntity> {
}
