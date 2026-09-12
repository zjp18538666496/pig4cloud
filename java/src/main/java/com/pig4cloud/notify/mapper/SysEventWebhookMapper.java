package com.pig4cloud.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.notify.entity.SysEventWebhookEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysEventWebhookMapper extends BaseMapper<SysEventWebhookEntity> {
}
