package com.pig4cloud.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.approval.entity.SysApprovalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysApprovalMapper extends BaseMapper<SysApprovalEntity> {
}
