package com.pig4cloud.profile.controller;

import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.auth.online.SessionRecord;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.service.LoginLogService;
import com.pig4cloud.log.service.OperateLogService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 个人中心增强：我的登录历史/操作记录/登录设备（可踢自己的其它设备）
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final LoginLogService loginLogService;
    private final OperateLogService operateLogService;
    private final OnlineUserStore onlineUserStore;
    private final SessionKickService sessionKickService;
    private final JwtUtils jwtUtils;
    private final com.pig4cloud.profile.ProfileService profileService;

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }

    /**
     * 我的登录历史（按账号过滤，与租户无关——账号全库唯一）
     */
    @GetMapping("/loginLogs")
    public R<PageResult<LoginLog>> loginLogs(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok("获取数据成功", loginLogService.pageQuery(currentUsername(), null, null, page, pageSize));
    }

    /**
     * 我的操作记录
     */
    @GetMapping("/operateLogs")
    public R<PageResult<OperateLog>> operateLogs(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok("获取数据成功", operateLogService.pageQuery(currentUsername(), null, page, pageSize));
    }

    /**
     * 我的在线设备（isCurrent标记当前设备）
     */
    @GetMapping("/sessions")
    public R<List<SessionRecord>> sessions(HttpServletRequest request) {
        String username = currentUsername();
        String currentJti = currentJti(request);
        List<SessionRecord> sessions = onlineUserStore.list().stream()
                .filter(session -> username.equals(session.getUsername()))
                .peek(session -> session.setCurrent(Boolean.valueOf(session.getTokenJti().equals(currentJti))))
                .toList();
        return R.ok("获取数据成功", sessions);
    }

    /**
     * 踢掉自己的某个其它设备（仅允许操作本人会话）
     */
    @PostMapping("/sessions/kick")
    public R<Void> kickOwnSession(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = currentUsername();
        SessionRecord session = onlineUserStore.findByAccessJti(body.get("tokenJti"));
        if (session == null) {
            throw new BizException("会话不存在或已下线");
        }
        if (!username.equals(session.getUsername())) {
            throw new BizException("只能下线本人的设备");
        }
        if (session.getTokenJti().equals(currentJti(request))) {
            throw new BizException("不能下线当前设备，请使用退出登录");
        }
        sessionKickService.kickSession(session);
        return R.ok("已下线", null);
    }

    private String currentJti(HttpServletRequest request) {
        try {
            String auth = request.getHeader("authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                Claims claims = jwtUtils.parseJwt(auth.substring(7));
                return claims.getId();
            }
        } catch (Exception ignored) {
            // token无效时无法标记当前设备
        }
        return null;
    }

    /**
     * 首页工作台自定义配置读取（当前用户）
     */
    @GetMapping("/workbench")
    public R<String> getWorkbench() {
        return R.ok("获取数据成功", profileService.getWorkbenchConfig(currentUser()));
    }

    /**
     * 首页工作台自定义配置保存
     */
    @PostMapping("/workbench")
    public R<Void> saveWorkbench(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        profileService.saveWorkbenchConfig(currentUser(), body.get("config"));
        return R.ok("保存成功", null);
    }

    private String currentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null
                ? null : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }
}