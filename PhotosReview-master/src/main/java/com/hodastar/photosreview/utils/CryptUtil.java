package com.hodastar.photosreview.utils;

import com.hodastar.photosreview.config.Config;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public class CryptUtil {

    // BCrypt
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 普通SHA256
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));

            StringBuffer hexString = new StringBuffer();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // HMAC-SHA256加密
    public static String hmacSha256(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // BCrypt加密
    public static String BCEcrypt(String input) {
        try {
            return encoder.encode(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // BCrypt校验
    public static Boolean checkBCEcrypt(String raw, String secret) {
        return encoder.matches(raw, secret);
    }

    // 牛逼加密
    public static String nBCrypt(String input) {
        // 加盐
        String inputSalt = input + Config.SALT;
        // 返回HMAC加密
        return hmacSha256(inputSalt, Config.HMAC_SECRET);
    }

    // 牛逼加密2
    public static String nBCrypt2(String input) {
        // 加盐
        String inputSalt = input + Config.SALT + Config.SALT;
        // 返回SHA256加密
        return hmacSha256(inputSalt, Config.HMAC_SECRET + Config.SALT);
    }

    /**
     * byte[] 转 HEX
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) hex.append('0');
            hex.append(s);
        }
        return hex.toString();
    }
}
