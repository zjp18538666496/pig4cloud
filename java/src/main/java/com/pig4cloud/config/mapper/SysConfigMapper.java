package com.pig4cloud.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.config.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {
}
