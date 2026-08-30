package com.pig4cloud.auth.online;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 在线会话记录：一次登录对应一条，key为access token的jti。
 * redis模式经JSON序列化存储，需保留无参构造器
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SessionRecord {

    /**
     * access token的jti（会话主键，refresh换新token时会更新）
     */
    private String tokenJti;

    /**
     * refresh token的jti（用于强退时同时拉黑刷新令牌）
     */
    private String refreshJti;

    private String username;

    private String name;

    /**
     * 租户id（0为平台层账号）
     */
    private Integer tenantId;

    private String ip;

    /**
     * 登录浏览器（由User-Agent解析）
     */
    private String browser;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date loginTime;

    /**
     * 最后访问时间（每次携带token请求时刷新；volatile保证跨线程写可见）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private volatile Date lastAccessTime;

    /**
     * access token过期时间戳(毫秒)
     */
    private long accessExpireAt;

    /**
     * refresh token过期时间戳(毫秒)；refresh过期后会话整体失效
     */
    private long refreshExpireAt;

    /**
     * 从User-Agent粗解析浏览器名称，仅用于在线列表展示
     */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("chromium")) return "Chromium";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("micromessenger")) return "微信内置浏览器";
        if (ua.contains("safari/")) return "Safari";
        if (ua.contains("msie") || ua.contains("trident/")) return "IE";
        return "其他";
    }
}
