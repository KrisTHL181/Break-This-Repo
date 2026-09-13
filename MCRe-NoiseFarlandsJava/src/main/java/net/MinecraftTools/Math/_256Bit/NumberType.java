package net.MinecraftTools.Math._256Bit;

/**
 * NumberType — DynamicNumber 支持的数字类型枚举
 * 
 * 类型提升顺序（从低到高）:
 * LONG < INT256 < UINT256 < FLOAT256 < UFLOAT256 < BIGINTEGER
 * 
 * INF32768 / MCRe NoiseFarlands 项目
 */
public enum NumberType {

    /** 64-bit 有符号整数 (Java 原生) */
    LONG,

    /** 256-bit 有符号整数 (补码, 零 GC) */
    INT256,

    /** 256-bit 无符号整数 (零 GC) */
    UINT256,

    /** 256-bit 有符号浮点 (1+79+176, IEEE 风格, RN 向偶舍入) */
    FLOAT256,

    /** 256-bit 无符号浮点 (64+192, IEEE 风格, RN 向偶舍入) */
    UFLOAT256,

    /** 动态精度整数 (优化版 OpenJDK 25.0.3 BigInteger) */
    BIGINTEGER;

    // ═══════════ 类型判断 ═══════════

    /** 是否为整数类型 */
    public boolean isInteger() {
        return this == LONG || this == INT256 || this == UINT256 || this == BIGINTEGER;
    }

    /** 是否为浮点类型 */
    public boolean isFloating() {
        return this == FLOAT256 || this == UFLOAT256;
    }

    /** 是否为无符号类型 */
    public boolean isUnsigned() {
        return this == UINT256 || this == UFLOAT256;
    }

    /** 是否为 256-bit 定长类型 */
    public boolean isFixed256() {
        return this == INT256 || this == UINT256 || this == FLOAT256 || this == UFLOAT256;
    }

    // ═══════════ 位宽 ═══════════

    /** 返回该类型的位宽（BIGINTEGER 返回 -1 表示无限） */
    public int bitWidth() {
        return switch (this) {
            case LONG       -> 64;
            case INT256     -> 256;
            case UINT256    -> 256;
            case FLOAT256   -> 256;
            case UFLOAT256  -> 256;
            case BIGINTEGER -> -1;
        };
    }

    // ═══════════ 类型提升 ─ 二分搜索 ═══════════

    /** 返回两个类型间的最高精度类型 */
    public static NumberType wider(NumberType a, NumberType b) {
        if (a == b) return a;
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(NumberType t) {
        return switch (t) {
            case LONG       -> 0;
            case INT256     -> 1;
            case UINT256    -> 2;
            case FLOAT256   -> 3;
            case UFLOAT256  -> 4;
            case BIGINTEGER -> 5;
        };
    }

    // ═══════════ 从字符串解析 ─══════════

    /** 从配置字符串解析类型 */
    public static NumberType fromString(String name) {
        if (name == null) throw new IllegalArgumentException("null NumberType");
        return switch (name.trim().toUpperCase()) {
            case "LONG"       -> LONG;
            case "INT256"     -> INT256;
            case "UINT256"    -> UINT256;
            case "FLOAT256"   -> FLOAT256;
            case "UFLOAT256"  -> UFLOAT256;
            case "BIGINTEGER" -> BIGINTEGER;
            default -> throw new IllegalArgumentException("Unknown NumberType: " + name);
        };
    }

    // ═══════════════════════ 测试 ═══════════════════════

    public static void main(String[] args) {
        System.out.println("=== NumberType 测试 ===");
        System.out.println("LONG.isInteger()      = " + LONG.isInteger());
        System.out.println("FLOAT256.isFloating() = " + FLOAT256.isFloating());
        System.out.println("UINT256.isUnsigned()  = " + UINT256.isUnsigned());
        System.out.println("INT256.bitWidth()     = " + INT256.bitWidth());
        System.out.println("wider(INT256, BIGINTEGER) = " + wider(INT256, BIGINTEGER));
        System.out.println("fromString(\"UFLOAT256\") = " + fromString("UFLOAT256"));
    }
}