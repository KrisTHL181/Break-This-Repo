package net.MinecraftTools.Serializations;

import java.nio.charset.StandardCharsets;
import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;

/**
 * 🔧 MCRe：Base58 文本转换（无第三方依赖）
 *
 * <p>使用自研 {@link DynamicAccuracy.BigInteger} 实现，完全脱离 JDK {@code java.math}，
 * 适用于希望复用动态精度库的坐标/配置序列化场景。
 *
 * <p>采用比特币 Base58 字母表（去掉了 0、O、I、l 以免混淆）：
 * <pre>123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz</pre>
 *
 * <p>与前导零的处理：UTF-8 字节序列中开头的 {@code 0x00} 会被编码为等量的 '1'，
 * 解码时再还原，保证 encode/decode 往返无损。
 *
 * @author MCRe Ultimate Scaler
 */
public final class Base58Transfer {

    // 比特币 Base58 字母表（去掉了 0、O、I、l 以免混淆）
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    private Base58Transfer() {
    }

    /** 将 UTF-8 文本编码为 Base58 字符串；null/空输入返回空串 */
    public static String encode(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);

        // 统计前导 0x00（标准 Base58 中每个前导零字节映射为一个 '1'）
        int leadingZeroes = 0;
        while (leadingZeroes < bytes.length && bytes[leadingZeroes] == 0) {
            leadingZeroes++;
        }

        // 剩余非零部分转为正数
        BigInteger num = new BigInteger(1, bytes);

        StringBuilder result = new StringBuilder();
        while (num.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = num.divideAndRemainder(BASE);
            result.append(ALPHABET.charAt(divRem[1].intValue()));
            num = divRem[0];
        }

        // 循环从低位取余，需反转；前导零补 '1'
        return "1".repeat(leadingZeroes) + result.reverse();
    }

    /** 将 Base58 字符串解码回 UTF-8 文本；null/空输入返回空串，非法字符抛 IllegalArgumentException */
    public static String decode(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        int leadingZeroes = 0;
        while (leadingZeroes < input.length() && input.charAt(leadingZeroes) == '1') {
            leadingZeroes++;
        }

        BigInteger num = BigInteger.ZERO;
        for (int i = leadingZeroes; i < input.length(); i++) {
            int digit = ALPHABET.indexOf(input.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base58 character: " + input.charAt(i));
            }
            num = num.multiply(BASE).add(BigInteger.valueOf(digit));
        }

        byte[] bytes;
        if (num.equals(BigInteger.ZERO)) {
            bytes = new byte[0];
        } else {
            byte[] mag = num.toByteArray();
            // toByteArray 可能带符号位 0x00 前缀，去掉
            int offset = (mag.length > 1 && mag[0] == 0) ? 1 : 0;
            bytes = new byte[leadingZeroes + (mag.length - offset)];
            // 前导零字节还原
            for (int i = 0; i < leadingZeroes; i++) {
                bytes[i] = 0;
            }
            System.arraycopy(mag, offset, bytes, leadingZeroes, mag.length - offset);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}