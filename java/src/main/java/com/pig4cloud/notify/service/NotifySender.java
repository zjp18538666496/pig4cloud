package com.pig4cloud.notify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 通知投递器：按渠道类型把标题+内容发出去，返回null表示成功、否则为失败原因。
 * 钉钉/企微/飞书本质都是POST一段JSON到机器人webhook，差别只在报文格式与加签，
 * 报文格式按官方文档实现（未接入真实企业账号前可先用自定义webhook渠道端到端验证链路）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifySender {

    private final MailService mailService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return null=成功；非null=失败原因
     */
    public String send(String type, String configJson, String title, String content) {
        return switch (type == null ? "" : type) {
            case "email" -> sendEmail(configJson, title, content);
            case "dingtalk" -> sendDingtalk(configJson, title, content);
            case "wecom" -> sendWecom(configJson, title, content);
            case "feishu" -> sendFeishu(configJson, title, content);
            case "webhook" -> sendWebhook(configJson, title, content);
            default -> "未知渠道类型：" + type;
        };
    }

    private String sendEmail(String configJson, String title, String content) {
        if (!mailService.isEnabled()) {
            return "邮件服务未启用（需配置spring.mail.*并开启app.mail.enabled）";
        }
        Map<String, Object> config = parse(configJson);
        Object to = config.get("to");
        if (to == null || to.toString().isBlank()) {
            return "渠道配置缺少收件人(to)";
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to.toString().split(","));
            message.setSubject(title);
            message.setText(content);
            mailSenderProvider.getObject().send(message);
            return null;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String sendWebhook(String configJson, String title, String content) {
        Map<String, Object> config = parse(configJson);
        String url = str(config.get("url"));
        if (url == null) {
            return "渠道配置缺少url";
        }
        String body = """
                {"title":%s,"content":%s,"timestamp":%d}
                """.formatted(json(title), json(content), System.currentTimeMillis());
        return post(url, body);
    }

    private String sendDingtalk(String configJson, String title, String content) {
        Map<String, Object> config = parse(configJson);
        String url = str(config.get("url"));
        if (url == null) {
            return "渠道配置缺少url";
        }
        // 钉钉加签：HMAC-SHA256(secret, timestamp+"\n"+secret)后Base64，拼到url上
        String secret = str(config.get("secret"));
        if (secret != null && !secret.isBlank()) {
            long timestamp = System.currentTimeMillis();
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec((timestamp + "\n" + secret).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String sign = Base64.getEncoder().encodeToString(mac.doFinal());
                url = url + (url.contains("?") ? "&" : "?") + "timestamp=" + timestamp + "&sign="
                        + java.net.URLEncoder.encode(sign, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return "钉钉加签失败：" + ex.getMessage();
            }
        }
        String body = objectMapper == null ? "" : ("{\"msgtype\":\"markdown\",\"markdown\":{\"title\":"
                + json(title) + ",\"text\":" + json("### " + title + "\n\n" + content) + "}}");
        return post(url, body);
    }

    private String sendWecom(String configJson, String title, String content) {
        Map<String, Object> config = parse(configJson);
        String url = str(config.get("url"));
        if (url == null) {
            return "渠道配置缺少url";
        }
        String body = "{\"msgtype\":\"markdown\",\"markdown\":{\"content\":"
                + json("**" + title + "**\n" + content) + "}}";
        return post(url, body);
    }

    private String sendFeishu(String configJson, String title, String content) {
        Map<String, Object> config = parse(configJson);
        String url = str(config.get("url"));
        if (url == null) {
            return "渠道配置缺少url";
        }
        String body = "{\"msg_type\":\"text\",\"content\":{\"text\":"
                + json(title + "\n" + content) + "}}";
        return post(url, body);
    }

    private String post(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String respBody = response.body() == null ? "" : response.body();
                // 钉钉/企微/飞书业务失败也是200，返回errcode非0时视为失败
                if (respBody.contains("\"errcode\"") && !respBody.contains("\"errcode\":0")) {
                    return "对端返回业务失败：" + respBody.substring(0, Math.min(200, respBody.length()));
                }
                return null;
            }
            return "HTTP " + response.statusCode() + "：" + (response.body() == null ? ""
                    : response.body().substring(0, Math.min(200, response.body().length())));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String configJson) {
        try {
            return objectMapper.readValue(configJson == null ? "{}" : configJson, Map.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String str(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private String json(String value) {
        try {
            return objectMapper.writeValueAsString(value == null ? "" : value);
        } catch (Exception ex) {
            return "\"\"";
        }
    }
}
