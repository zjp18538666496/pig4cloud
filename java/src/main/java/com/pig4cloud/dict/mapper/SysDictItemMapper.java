package com.pig4cloud.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.dict.entity.SysDictItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItemEntity> {
}
