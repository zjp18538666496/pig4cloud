package com.pig4cloud.auth.dto;

import com.pig4cloud.tenant.dto.TenantBrandVO;
import com.pig4cloud.user.vo.UserVO;

/**
 * 登录结果：双token、脱敏后的用户信息与租户品牌（登录后侧边栏/标题展示）
 */
public record LoginResult(String accessToken, String refreshToken, UserVO user, TenantBrandVO brand) {
}
