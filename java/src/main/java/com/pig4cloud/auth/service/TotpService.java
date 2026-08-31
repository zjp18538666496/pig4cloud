package com.pig4cloud.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * TOTP两步认证（RFC 6230，兼容Google Authenticator/腾讯身份验证器等）：
 * SHA1 + 30秒步长 + 6位数字码，验证窗口±1步（容忍30秒时钟偏差）
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final long STEP_SECONDS = 30L;
    private static final int CODE_DIGITS = 6;
    /**
     * 验证窗口：允许前后各1步，容忍时钟偏差
     */
    private static final int WINDOW = 1;

    private final SecureRandom random = new SecureRandom();
    private final Base32 base32 = new Base32();

    /**
     * 生成Base32随机密钥
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32.encodeToString(bytes);
    }

    /**
     * 校验动态码：匹配当前步或前后1步；同时匹配备用恢复码（命中即消费）
     */
    public boolean verify(String base32Secret, String code, String backupCodesHashes) {
        if (code == null || !code.matches("\\d{6}")) {
            return verifyBackupCode(code, backupCodesHashes) != null;
        }
        long step = System.currentTimeMillis() / 1000L / STEP_SECONDS;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (generateTotp(base32Secret, step + i).equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验备用恢复码（8位十六进制），命中返回命中的哈希（调用方负责从库中移除该码）
     */
    public String verifyBackupCode(String code, String backupCodesHashes) {
        if (code == null || backupCodesHashes == null || backupCodesHashes.isBlank()) {
            return null;
        }
        String normalized = code.replace("-", "").trim();
        if (normalized.length() != 8) {
            return null;
        }
        String hash = sha256Hex(normalized);
        for (String saved : backupCodesHashes.split(",")) {
            if (hash.equals(saved.trim())) {
                return saved.trim();
            }
        }
        return null;
    }

    /**
     * 生成8个一次性备用恢复码（形如A1B2C3D4），返回明文列表与逗号分隔的SHA256哈希
     */
    public Map.Entry<List<String>, String> generateBackupCodes() {
        List<String> plain = new java.util.ArrayList<>();
        List<String> hashes = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String code = String.format("%08X", random.nextLong() & 0xFFFFFFFFL);
            plain.add(code);
            hashes.add(sha256Hex(code));
        }
        return Map.entry(plain, String.join(",", hashes));
    }

    /**
     * 生成otpauth://绑定URI（验证器App扫码用）
     */
    public String buildOtpauthUri(String username, String base32Secret) {
        return "otpauth://totp/PIGX%20ADMIN:" + username
                + "?secret=" + base32Secret + "&issuer=PIGX%20ADMIN&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * 绑定URI转二维码PNG（base64 dataURL）
     */
    public String qrImage(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200,
                    Map.of(EncodeHintType.MARGIN, 1));
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("生成二维码失败", ex);
        }
    }

    private String generateTotp(String base32Secret, long step) {
        try {
            byte[] key = base32.decode(base32Secret);
            byte[] counter = new byte[8];
            long value = step;
            for (int i = 7; i >= 0; i--) {
                counter[i] = (byte) (value & 0xFF);
                value >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("TOTP计算失败", ex);
        }
    }

    private String sha256Hex(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA256计算失败", ex);
        }
    }
}
