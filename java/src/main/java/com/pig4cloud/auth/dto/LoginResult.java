package com.pig4cloud.auth.dto;

import com.pig4cloud.user.vo.UserVO;

/**
 * 登录结果：双token与脱敏后的用户信息
 */
public record LoginResult(String accessToken, String refreshToken, UserVO user) {
}
