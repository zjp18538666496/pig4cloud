package com.pig4cloud.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.job.entity.SysJobLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogEntity> {
}
