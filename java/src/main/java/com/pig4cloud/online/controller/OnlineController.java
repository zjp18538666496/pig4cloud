package com.pig4cloud.online.controller;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.SessionRecord;
import com.pig4cloud.auth.online.TokenBlacklist;
import com.pig4cloud.auth.service.AuthService;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在线用户管理：查看当前登录会话并强制下线（会话为内存实现，单机有效）
 */
@RestController
@RequestMapping("/api/online")
@RequiredArgsConstructor
public class OnlineController {

    private final OnlineUserStore onlineUserStore;
    private final TokenBlacklist tokenBlacklist;
    private final AuthService authService;

    @PostMapping("/getOnlineUsers")
    @PreAuthorize("hasAuthority('online:read')")
    public R<PageResult<SessionRecord>> getOnlineUsers(@RequestBody OnlineQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        List<SessionRecord> all = onlineUserStore.list();
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            all = all.stream()
                    .filter(session -> session.getUsername() != null
                            && session.getUsername().contains(dto.getUsername().trim()))
                    .toList();
        }
        long total = all.size();
        long from = Math.max(0, (page - 1) * pageSize);
        long to = Math.min(total, from + pageSize);
        List<SessionRecord> rows = from >= to ? List.of() : all.subList((int) from, (int) to);
        return R.ok("获取数据成功", PageResult.of(rows, total, pageSize, page));
    }

    /**
     * 强制下线：拉黑该会话的access与refresh token并移除会话
     */
    @PostMapping("/kickOut")
    @PreAuthorize("hasAuthority('online:kick')")
    public R<Void> kickOut(@Valid @RequestBody KickOutDto dto) {
        SessionRecord session = onlineUserStore.findByAccessJti(dto.getTokenJti());
        if (session == null) {
            throw new BizException("会话不存在或已下线");
        }
        tokenBlacklist.revoke(session.getTokenJti(), session.getAccessExpireAt());
        tokenBlacklist.revoke(session.getRefreshJti(), session.getRefreshExpireAt());
        onlineUserStore.remove(session.getTokenJti());
        return R.ok("已强制下线", null);
    }

    @Getter
    @Setter
    public static class OnlineQueryDto extends BasePageQuery {
        private String username = "";
    }

    /**
     * 超管代理登录：以目标用户身份签发token（响应头带新token，前端保存后即进入代理视角）
     */
    @PostMapping("/impersonate")
    @PreAuthorize("hasAuthority('super')")
    public R<com.pig4cloud.user.vo.UserVO> impersonate(@Valid @RequestBody KickOutDto dto,
                                                       jakarta.servlet.http.HttpServletResponse response) {
        String operator = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        var result = authService.impersonate(dto.getTokenJti().trim(), operator);
        response.setHeader("Authorization", "Bearer " + result.accessToken());
        response.setHeader("Refresh-Token", result.refreshToken());
        return R.ok("请求成功", result.user());
    }

    @Getter
    @Setter
    public static class KickOutDto {
        @NotBlank(message = "会话标识不能为空")
        private String tokenJti;
    }
}
