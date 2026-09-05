package com.pig4cloud.export.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.export.entity.SysExportTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysExportTaskMapper extends BaseMapper<SysExportTaskEntity> {
}
