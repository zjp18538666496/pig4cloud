package com.pig4cloud.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务：spring.mail.host未配置时JavaMailSender bean不存在，
 * 用ObjectProvider延迟获取，保证未配置邮件也能正常启动
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 邮件功能是否可用（显式开启且SMTP已配置）
     */
    public boolean isEnabled() {
        return enabled && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 异步发送找回密码验证码邮件，发送失败只记日志（验证码在有效期内仍可使用）
     */
    @Async
    public void sendResetCodeMail(String to, String code) {
        if (!isEnabled()) {
            log.warn("邮件服务未启用(app.mail.enabled=false或未配置spring.mail.host)，验证码邮件未发送: {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("PIGX ADMIN 找回密码验证码");
            message.setText("您正在找回登录密码，验证码：" + code + "，5分钟内有效。若非本人操作请忽略本邮件。");
            mailSenderProvider.getObject().send(message);
            log.info("找回密码验证码邮件已发送: {}", to);
        } catch (Exception ex) {
            log.error("找回密码验证码邮件发送失败: {}，原因: {}", to, ex.getMessage());
        }
    }
}
