package com.pig4cloud.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import org.springframework.stereotype.Component;

import java.awt.GradientPaint;
import java.awt.geom.Ellipse2D;

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

/**
 * 图形验证码：Java2D绘制PNG返回base64，答案存StateStore（TTL自动过期），
 * 有效期内一次性使用（校验即删除）
 */
@Component
public class CaptchaService {

    private static final long EXPIRE_MILLIS = 2 * 60 * 1000L;
    /**
     * 去掉0/O、1/I/l等易混淆字符
     */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final String KEY_PREFIX = "auth:captcha:";
    private static final String IMAGE_PREFIX = "data:image/png;base64,";

    private static final int PIECE_SIZE = 44;
    private static final int SLIDER_TOLERANCE = 5;

    private final SecureRandom random = new SecureRandom();
    private final StateStore stateStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaptchaService(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 生成验证码（默认图形字符型）：返回captchaId与dataURL图片
     */
    public Map<String, String> generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        String captchaId = UUID.randomUUID().toString();
        stateStore.put(KEY_PREFIX + captchaId, code.toString(), EXPIRE_MILLIS);
        return Map.of(
                "captchaId", captchaId,
                "image", IMAGE_PREFIX + Base64.getEncoder().encodeToString(drawImage(code.toString())));
    }

    /**
     * 滑块拼图验证码：返回背景图/拼图块(base64)与拼图块Y坐标；X坐标为答案不入返回值。
     * 存储格式{"t":"s","x":答案}，校验时比对拖动距离（±5px容差）
     */
    public Map<String, Object> generateSlider() {
        String captchaId = UUID.randomUUID().toString();
        int x = 60 + random.nextInt(200);
        int y = 20 + random.nextInt(80);
        stateStore.put(KEY_PREFIX + captchaId,
                "{\"t\":\"s\",\"x\":" + x + "}", EXPIRE_MILLIS);
        BufferedImage background = drawSliderBackground();
        BufferedImage piece = cutPiece(background, x, y);
        darkenHole(background, x, y);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("captchaId", captchaId);
        result.put("type", "slider");
        result.put("bgImage", IMAGE_PREFIX + Base64.getEncoder().encodeToString(toPng(background)));
        result.put("pieceImage", IMAGE_PREFIX + Base64.getEncoder().encodeToString(toPng(piece)));
        result.put("pieceY", y);
        return result;
    }

    public void verify(String captchaId, String code) {
        String saved = captchaId == null ? null : stateStore.get(KEY_PREFIX + captchaId);
        // 取出即删除，保证一次性
        if (captchaId != null) {
            stateStore.delete(KEY_PREFIX + captchaId);
        }
        if (saved == null) {
            throw new BizException("验证码已过期，请刷新后重试");
        }
        if (saved.startsWith("{")) {
            // 滑块拼图：code=拖动的X距离
            try {
                Map<String, Object> stored = objectMapper.readValue(saved, Map.class);
                int answer = ((Number) stored.get("x")).intValue();
                int moved;
                try {
                    moved = Integer.parseInt(code == null ? "" : code.trim());
                } catch (NumberFormatException ex) {
                    throw new BizException("请先完成滑块拖动");
                }
                if (Math.abs(moved - answer) > SLIDER_TOLERANCE) {
                    throw new BizException("滑块位置不正确，请重试");
                }
                return;
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BizException("验证码校验失败，请刷新后重试");
            }
        }
        if (!saved.equalsIgnoreCase(code == null ? "" : code.trim())) {
            throw new BizException("验证码不正确");
        }
    }

    private byte[] toPng(java.awt.image.BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成验证码图片失败", ex);
        }
    }

    /**
     * 滑块背景：渐变底+随机圆形纹理
     */
    private java.awt.image.BufferedImage drawSliderBackground() {
        int width = 310;
        int height = 155;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setPaint(new GradientPaint(0, 0, new Color(0x2E, 0x5C, 0xF6),
                width, height, new Color(0x14, 0xC9, 0xC9)));
        graphics.fillRect(0, 0, width, height);
        for (int i = 0; i < 12; i++) {
            graphics.setColor(randomColor(180, 255));
            int size = 6 + random.nextInt(18);
            graphics.fillOval(random.nextInt(width), random.nextInt(height), size, size);
        }
        graphics.dispose();
        return image;
    }

    /**
     * 从背景抠出圆形拼图块
     */
    private java.awt.image.BufferedImage cutPiece(java.awt.image.BufferedImage background, int x, int y) {
        BufferedImage piece = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = piece.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setClip(new Ellipse2D.Float(0, 0, PIECE_SIZE, PIECE_SIZE));
        graphics.drawImage(background, -x, -y, null);
        graphics.dispose();
        return piece;
    }

    /**
     * 背景上把拼图区域涂暗（挖洞效果）并描边
     */
    private void darkenHole(java.awt.image.BufferedImage background, int x, int y) {
        Graphics2D graphics = background.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.45f));
        graphics.setColor(Color.BLACK);
        graphics.fill(new Ellipse2D.Float(x, y, PIECE_SIZE, PIECE_SIZE));
        graphics.setComposite(java.awt.AlphaComposite.SrcOver);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new Ellipse2D.Float(x, y, PIECE_SIZE, PIECE_SIZE));
        graphics.dispose();
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
