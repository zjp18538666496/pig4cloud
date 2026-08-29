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
     * 开通租户：创建租户+租户管理员账号+管理员角色并绑定全部菜单
     */
    R<Void> createTenant(TenantCreateDto dto);

    /**
     * 修改租户名称/状态
     */
    R<Void> updateTenant(TenantUpdateDto dto);
}
