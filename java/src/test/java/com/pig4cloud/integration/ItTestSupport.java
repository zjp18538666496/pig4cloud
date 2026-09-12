package com.pig4cloud.integration;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.DriverManager;
import java.util.Map;

/**
 * 集成测试基类：起完整应用（RANDOM_PORT），使用独立schema pigx_admin_it
 * （DB_INIT自动建库灌演示数据），状态存储memory不碰共享Redis。
 * 设置环境变量RUN_IT=true才会执行（CI默认跳过）。
 * 用例需要的图形验证码答案直接从StateStore读取（等价于人工读图）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "RUN_IT", matches = "true")
public abstract class ItTestSupport {

    static {
        // 清理挂钩：全部IT类跑完（JVM退出）后再删IT专用schema——不能放@AfterAll，
        // 否则第一个类结束就删库，后续类复用的数据源指向已删库导致连锁失败
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (var connection = DriverManager.getConnection(
                    "jdbc:mysql://10.126.126.3:3306/?characterEncoding=UTF-8", "root", "123456");
                 var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS pigx_admin_it");
            } catch (Exception ignored) {
            }
            try (var client = com.mongodb.client.MongoClients.create(
                    "mongodb://root:123456@10.126.126.3:27017/admin?authSource=admin")) {
                client.getDatabase("pigx_log_it").drop();
            } catch (Exception ignored) {
            }
        }));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected com.pig4cloud.common.store.StateStore stateStore;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /**
     * 登录并返回带Authorization头的请求头（图形验证码答案从StateStore读取，等价人工读图）
     */
    protected HttpHeaders login(String username, String password) {
        Map<String, Object> captchaResponse = restTemplate
                .getForEntity(baseUrl() + "/api/auth/captcha", Map.class).getBody();
        Map<?, ?> data = (Map<?, ?>) captchaResponse.get("data");
        String captchaId = String.valueOf(data.get("captchaId"));
        String saved = stateStore.get("auth:captcha:" + captchaId);
        org.junit.jupiter.api.Assertions.assertNotNull(saved, "验证码应已写入StateStore");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", password,
                "captchaId", captchaId,
                "captchaCode", saved.startsWith("{") ? "" : saved);
        var response = restTemplate.postForEntity(baseUrl() + "/api/auth/login",
                new HttpEntity<>(body, headers), Map.class);
        org.junit.jupiter.api.Assertions.assertEquals(200,
                ((Number) ((Map<?, ?>) response.getBody()).get("code")).intValue(), "登录应成功");
        String authorization = response.getHeaders().getFirst("Authorization");
        org.junit.jupiter.api.Assertions.assertNotNull(authorization, "登录应下发token");

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("Authorization", authorization);
        return authHeaders;
    }

    /**
     * 带认证头的POST
     */
    protected Map<String, Object> post(String url, Object body, HttpHeaders headers) {
        var response = restTemplate.exchange(baseUrl() + url, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
        return response.getBody();
    }

    protected Map<String, Object> get(String url, HttpHeaders headers) {
        var response = restTemplate.exchange(baseUrl() + url, HttpMethod.GET,
                new HttpEntity<>(null, headers), Map.class);
        return response.getBody();
    }

}
