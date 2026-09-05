package com.pig4cloud.apikey.open;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Open API签名工具：HMAC-SHA256。签名串与服务端逐行拼接（\n分隔）：
 *   METHOD + "\n" + path[?sortedQuery] + "\n" + X-Api-Key + "\n" + X-Timestamp + "\n" + X-Nonce
 * query按参数名字典序排序（值保持原始编码不解码），空则只拼path
 */
public final class OpenApiSigner {

    private OpenApiSigner() {
    }

    /**
     * 计算HMAC-SHA256签名（hex小写）
     */
    public static String sign(String secret, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("签名计算失败", ex);
        }
    }

    /**
     * 常量时间比较，防时序侧信道
     */
    public static boolean safeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
