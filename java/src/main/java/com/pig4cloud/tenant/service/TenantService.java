package com.pig4cloud.tenant.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.tenant.dto.TenantCreateDto;
import com.pig4cloud.tenant.dto.TenantDto;
import com.pig4cloud.tenant.dto.TenantUpdateDto;
import com.pig4cloud.tenant.entity.TenantEntity;

public interface TenantService {

    /**
     * 分页获取租户列表
     */
    R<PageResult<TenantEntity>> getTenantLists(TenantDto tenantDto);

    /**
     * 开通租户：绑定套餐菜单并自动创建租户管理员账号（初始密码按配置强制首登修改）
     */
    R<Void> createTenant(TenantCreateDto dto);

    /**
     * 编辑租户（名称/状态/套餐/有效期/配额）；套餐变更重绑管理员角色菜单，禁用/换套餐踢会话
     */
    R<Void> updateTenant(TenantUpdateDto dto);

    /**
     * 删除租户：租户下存在用户时拒绝；级联清理角色/部门/公告/套餐绑定并踢会话
     */
    R<Void> deleteTenant(Integer id);
}
