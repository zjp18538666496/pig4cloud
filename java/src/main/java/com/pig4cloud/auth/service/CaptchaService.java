package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码（内存实现，单机有效）：Java2D绘制PNG返回base64，答案存内存，
 * 2分钟有效且一次性（校验即删除）。
 */
@Component
public class CaptchaService {

    private static final long EXPIRE_MILLIS = 2 * 60 * 1000L;
    /**
     * 去掉0/O、1/I/l等易混淆字符
     */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final String IMAGE_PREFIX = "data:image/png;base64,";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成验证码：返回captchaId与dataURL图片
     */
    public Map<String, String> generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        String captchaId = UUID.randomUUID().toString();
        store.put(captchaId, new CaptchaEntry(code.toString(), System.currentTimeMillis() + EXPIRE_MILLIS));
        return Map.of(
                "captchaId", captchaId,
                "image", IMAGE_PREFIX + Base64.getEncoder().encodeToString(drawImage(code.toString())));
    }

    public void verify(String captchaId, String code) {
        CaptchaEntry entry = captchaId == null ? null : store.remove(captchaId);
        if (entry == null || entry.expireAt() <= System.currentTimeMillis()) {
            throw new BizException("验证码已过期，请刷新后重试");
        }
        if (!entry.code().equalsIgnoreCase(code == null ? "" : code.trim())) {
            throw new BizException("验证码不正确");
        }
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt() <= now);
    }

    private byte[] drawImage(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 背景
        graphics.setColor(new Color(0xF2, 0xF4, 0xF8));
        graphics.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 5; i++) {
            graphics.setColor(randomColor(160, 220));
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }
        // 逐字符绘制并轻微旋转
        Font font = new Font("Arial", Font.BOLD, 26);
        graphics.setFont(font);
        for (int i = 0; i < code.length(); i++) {
            double theta = (random.nextInt(40) - 20) * Math.PI / 180;
            int x = 14 + i * 26;
            int y = 29;
            graphics.setColor(randomColor(30, 130));
            graphics.rotate(theta, x, y);
            graphics.drawString(String.valueOf(code.charAt(i)), x, y);
            graphics.rotate(-theta, x, y);
        }
        // 噪点
        for (int i = 0; i < 60; i++) {
            image.setRGB(random.nextInt(width), random.nextInt(height), randomColor(100, 240).getRGB());
        }
        graphics.dispose();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成验证码图片失败", ex);
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(min + random.nextInt(max - min),
                min + random.nextInt(max - min),
                min + random.nextInt(max - min));
    }

    private record CaptchaEntry(String code, long expireAt) {
    }
}
