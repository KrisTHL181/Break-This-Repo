package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.RoundingMode;

/**
 * PrecisionConfig — 任意精度数字系统的全局配置
 * 
 * 控制 DynamicNumber 的默认精度、溢出行为、坐标转换
 * 线程安全，全局单例
 * 
 * INF32768 / MCRe NoiseFarlands 项目
 */
public final class PrecisionConfig {

    // ═══════════ 静态配置 ═══════════

    /** 默认运算精度类型 */
    private static volatile NumberType defaultType = NumberType.INT256;

    /** 溢出时自动升级类型（true=升级到 BigInteger，false=抛异常） */
    private static volatile boolean autoUpgrade = true;

    /** 浮点运算时的舍入模式 */
    private static volatile RoundingMode roundingMode = RoundingMode.HALF_EVEN;

    /** 打印数字时的最大小数位数 */
    private static volatile int maxDecimalPlaces = 10;

    /** 是否启用调试日志 */
    private static volatile boolean debugLogging = false;

    // ═══════════ 获取器/设置器 ═══════════

    public static NumberType getDefaultType() { return defaultType; }

    public static void setDefaultType(NumberType type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        defaultType = type;
    }

    public static boolean isAutoUpgrade() { return autoUpgrade; }

    public static void setAutoUpgrade(boolean enable) {
        autoUpgrade = enable;
    }

    public static RoundingMode getRoundingMode() { return roundingMode; }

    public static void setRoundingMode(RoundingMode mode) {
        if (mode == null) throw new IllegalArgumentException("roundingMode cannot be null");
        roundingMode = mode;
    }

    public static int getMaxDecimalPlaces() { return maxDecimalPlaces; }

    public static void setMaxDecimalPlaces(int places) {
        if (places < 0) throw new IllegalArgumentException("maxDecimalPlaces >= 0");
        maxDecimalPlaces = places;
    }

    public static boolean isDebugLogging() { return debugLogging; }

    public static void setDebugLogging(boolean enable) {
        debugLogging = enable;
    }

    // ═══════════ 预设配置切换 ═══════════

    /** 快速模式：全部用 Int256（最快，零 GC） */
    public static void selectFastMode() {
        defaultType = NumberType.INT256;
        autoUpgrade = false;
    }

    /** 标准模式：Int256 + 溢出升级 BigInteger */
    public static void selectStandardMode() {
        defaultType = NumberType.INT256;
        autoUpgrade = true;
    }

    /** 精确模式：BigInteger（最精确，稍慢） */
    public static void selectExactMode() {
        defaultType = NumberType.BIGINTEGER;
        autoUpgrade = false;
    }

    /** 浮点模式：Float256 */
    public static void selectFloatMode() {
        defaultType = NumberType.FLOAT256;
        autoUpgrade = false;
    }

    // ═══════════ 坐标转换 ═══════════

    /** Minecraft 坐标 → DynamicNumber（按当前默认类型） */
    public static DynamicNumber fromCoordinate(double coord) {
        return switch (defaultType) {
            case LONG       -> DynamicNumber.of((long) coord);
            case INT256     -> DynamicNumber.of(Int256.of((long) coord));
            case UINT256    -> DynamicNumber.of(UInt256.of(Math.abs((long) coord)));
            case FLOAT256   -> DynamicNumber.of(Float256.of(coord));
            case UFLOAT256  -> DynamicNumber.of(UFloat256.of(Math.abs((double) coord)));
            case BIGINTEGER -> DynamicNumber.of(BigInteger.valueOf((long) coord));
        };
    }

    /** 块坐标 → DynamicNumber */
    public static DynamicNumber fromBlock(int blockX, int blockZ) {
        return switch (defaultType) {
            case LONG       -> DynamicNumber.of((long) blockX * blockZ);
            case INT256   -> DynamicNumber.of(Int256.of(blockX).multiply(Int256.of(blockZ)));
            case UINT256  -> DynamicNumber.of(UInt256.of(blockX).multiply(UInt256.of(blockZ)));
            case FLOAT256 -> DynamicNumber.of(Float256.of(Int256.of(blockX).multiply(Int256.of((blockZ)))));
            case UFLOAT256 -> DynamicNumber.of(UFloat256.of(UInt256.of(blockX).multiply(UInt256.of(blockZ))));
            case BIGINTEGER -> DynamicNumber.of(BigInteger.valueOf(blockX).multiply(BigInteger.valueOf(blockZ)));
        };
    }

    // ═══════════ 溢出检测 ═══════════

    /**
     * 判断 DynamicNumber 是否在当前配置下溢出
     * @param number 待校准的数字
     * @return 溢出则返回升级后的 DynamicNumber，否则返回原值
     */
    public static DynamicNumber handleOverflow(DynamicNumber number) {
        if (!autoUpgrade) return number;

        switch (number.type()) {
            case LONG:
                long val = number.toLong();
                if (val == Long.MAX_VALUE || val == Long.MIN_VALUE)
                    return number.convertTo(NumberType.BIGINTEGER);
                break;

            case INT256:
                Int256 i256 = number.toInt256();
                if (i256.bitLength() >= 254) // 接近溢出
                    return number.convertTo(NumberType.BIGINTEGER);
                break;

            case UINT256:
                UInt256 u256 = number.toUInt256();
                if (u256.bitLength() >= 255)
                    return number.convertTo(NumberType.BIGINTEGER);
                break;

            default:
                break;
        }
        return number;
    }

    // ═══════════════════════ 测试 ═══════════════════════

    public static void main(String[] args) {
        System.out.println("=== PrecisionConfig 测试 ===");
        System.out.println("默认类型: " + getDefaultType());
        System.out.println("自动升级: " + isAutoUpgrade());
        System.out.println("舍入模式: " + getRoundingMode());

        System.out.println("\n切换模式:");
        selectFastMode();
        System.out.println("快速模式: " + getDefaultType());

        selectStandardMode();
        System.out.println("标准模式: " + getDefaultType());

        System.out.println("\n坐标转换:");
        DynamicNumber coord = fromCoordinate(12550824.0);
        System.out.println("12550824 边境之地 → " + coord);
    }
}