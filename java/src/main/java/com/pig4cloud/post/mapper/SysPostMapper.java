package com.pig4cloud.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.post.entity.SysPostEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysPostMapper extends BaseMapper<SysPostEntity> {
}
