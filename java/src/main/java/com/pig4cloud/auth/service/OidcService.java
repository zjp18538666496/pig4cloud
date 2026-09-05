package com.pig4cloud.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * OIDC单点登录（Keycloak/Casdoor等，app.oidc.enabled=true开启）：
 * 标准授权码流程 authorize → code → token → userinfo，按username-claim匹配本地账号。
 * 票据机制：callback校验通过后签发一次性ticket（3分钟），前端用ticket换登录态，
 * 避免token出现在URL中。需要先在本地建同名账号（不匹配则拒绝并提示）。
 * 联调提示：需可访问的IdP；未接入前此路径不可用，代码按OIDC标准实现
 */
@Slf4j
@Service
public class OidcService {

    private final StateStore stateStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Getter
    @Value("${app.oidc.enabled:false}")
    private boolean enabled;

    @Value("${app.oidc.client-id:}")
    private String clientId;

    @Value("${app.oidc.client-secret:}")
    private String clientSecret;

    @Value("${app.oidc.authorize-uri:}")
    private String authorizeUri;

    @Value("${app.oidc.token-uri:}")
    private String tokenUri;

    @Value("${app.oidc.userinfo-uri:}")
    private String userinfoUri;

    @Value("${app.oidc.redirect-uri:}")
    private String redirectUri;

    @Value("${app.oidc.scope:openid profile email}")
    private String scope;

    @Value("${app.oidc.username-claim:preferred_username}")
    private String usernameClaim;

    public OidcService(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 构造授权跳转URL（带state防CSRF，state存StateStore 5分钟）
     */
    public String buildAuthorizeUrl() {
        if (!enabled) {
            throw new BizException("OIDC单点登录未开启");
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.put("oidc:state:" + state, "1", 5 * 60 * 1000L);
        return authorizeUri
                + (authorizeUri.contains("?") ? "&" : "?")
                + "response_type=code&client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc(scope)
                + "&state=" + state;
    }

    /**
     * callback：校验state与code，换取userinfo返回用户名；失败抛异常
     */
    public String resolveUsername(String code, String state) {
        if (state == null || stateStore.get("oidc:state:" + state) == null) {
            throw new BizException("OIDC状态校验失败(state无效或已过期)");
        }
        stateStore.delete("oidc:state:" + state);
        try {
            String form = "grant_type=authorization_code&code=" + enc(code)
                    + "&redirect_uri=" + enc(redirectUri)
                    + "&client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret);
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUri))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> token = objectMapper.readValue(tokenResponse.body(), Map.class);
            String accessToken = (String) token.get("access_token");
            if (accessToken == null) {
                throw new BadCredentialsException("OIDC换token失败：" + tokenResponse.body().substring(0, Math.min(200, tokenResponse.body().length())));
            }
            HttpRequest userinfoRequest = HttpRequest.newBuilder()
                    .uri(URI.create(userinfoUri))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> userinfoResponse = httpClient.send(userinfoRequest, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> claims = objectMapper.readValue(userinfoResponse.body(), Map.class);
            String username = claims.get(usernameClaim) == null ? null : claims.get(usernameClaim).toString();
            if (username == null || username.isBlank()) {
                throw new BadCredentialsException("OIDC用户信息缺少用户名声明(" + usernameClaim + ")");
            }
            return username;
        } catch (BizException | BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("OIDC回调处理失败", ex);
            throw new BadCredentialsException("OIDC登录失败：" + ex.getMessage());
        }
    }

    /**
     * 签发一次性登录票据（3分钟有效），前端拿ticket换正式登录态
     */
    public String issueTicket(String username) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        stateStore.put("oidc:ticket:" + ticket, username, 3 * 60 * 1000L);
        return ticket;
    }

    /**
     * 用票据换取用户名（一次性）
     */
    public String exchangeTicket(String ticket) {
        String username = ticket == null ? null : stateStore.get("oidc:ticket:" + ticket);
        if (username == null) {
            throw new BizException("登录票据无效或已过期");
        }
        stateStore.delete("oidc:ticket:" + ticket);
        return username;
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
