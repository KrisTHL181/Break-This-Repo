package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;

/**
 * DynamicNumber — 统一数字容器
 *
 * <p>包装 6 种数字类型：Long, Int256, UInt256, Float256, UFloat256, BigInteger 根据 PrecisionConfig
 * 自动选择运算精度，所有运算返回新的 DynamicNumber（不可变）
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class DynamicNumber extends Number implements Comparable<DynamicNumber> {

    // ═══════════ 内部存储 ═══════════
    private final Object value;
    private final NumberType type;

    // ──────── 缓存 ────────
    private transient int hash;
    private static final int HASH_NOT_CACHED = Integer.MIN_VALUE;

    // ──────── 常量 ────────
    public static final DynamicNumber ZERO = new DynamicNumber(NumberType.LONG, 0L);
    public static final DynamicNumber ONE = new DynamicNumber(NumberType.LONG, 1L);
    public static final DynamicNumber TWO = new DynamicNumber(NumberType.LONG, 2L);
    public static final DynamicNumber TEN = new DynamicNumber(NumberType.LONG, 10L);

    // ──────── 构造 ────────
    private DynamicNumber(NumberType type, Object value) {
        this.type = type;
        this.value = value;
        this.hash = HASH_NOT_CACHED;
    }

    // ═══════════ 工厂方法 ═══════════

    /** 从 long (快速路径) */
    public static DynamicNumber of(long val) {
        if (val == 0) return ZERO;
        if (val == 1) return ONE;
        if (val == 2) return TWO;
        if (val == 10) return TEN;
        return new DynamicNumber(NumberType.LONG, val);
    }

    /** 从 double（先转 Float256） */
    public static DynamicNumber of(double val) {
        if (val == 0.0) return ZERO;
        if (val == 1.0) return ONE;
        if (Double.isNaN(val) || Double.isInfinite(val))
            throw new IllegalArgumentException("Cannot convert NaN/Inf to DynamicNumber");
        return new DynamicNumber(NumberType.FLOAT256, Float256.of(val));
    }

    /** 从 Int256 */
    public static DynamicNumber of(Int256 val) {
        return new DynamicNumber(NumberType.INT256, val);
    }

    /** 从 UInt256 */
    public static DynamicNumber of(UInt256 val) {
        return new DynamicNumber(NumberType.UINT256, val);
    }

    /** 从 Float256 */
    public static DynamicNumber of(Float256 val) {
        return new DynamicNumber(NumberType.FLOAT256, val);
    }

    /** 从 UFloat256 */
    public static DynamicNumber of(UFloat256 val) {
        return new DynamicNumber(NumberType.UFLOAT256, val);
    }

    /** 从 BigInteger (我们的优化版) */
    public static DynamicNumber of(BigInteger val) {
        return new DynamicNumber(NumberType.BIGINTEGER, val);
    }

    /** 从 BigDecimal */
    /**
     * 从 BigDecimal 创建 DynamicNumber。 修复：原实现 toBigInteger() 会静默截断小数部分。 现在按 BigDecimal 是否有小数部分自动选择： -
     * 整数 BigDecimal → BIGINTEGER - 带小数 → FLOAT256（可表示小数） 若需强制整数截断，请用 ofInteger(BigDecimal)。
     */
    public static DynamicNumber of(BigDecimal val) {
        if (val.signum() == 0) return ZERO;
        if (val.scale() <= 0) {
            // 整数：值 = unscaled × 10^(-scale)，可直接转 BigInteger
            BigInteger bi = val.scale() == 0
                    ? val.unscaledValue()
                    : val.unscaledValue().multiply(BigInteger.TEN.pow(-val.scale()));
            return new DynamicNumber(NumberType.BIGINTEGER, bi);
        }
        // 有小数：走 Float256
        return new DynamicNumber(NumberType.FLOAT256, Float256.of(val));
    }

    /** 明确要求整数截断 */
    public static DynamicNumber ofInteger(BigDecimal val) {
        return of(val.toBigInteger());
    }

    // ═══════════ 类型查询 ═══════════

    public NumberType type() {
        return type;
    }

    public boolean isLong() {
        return type == NumberType.LONG;
    }

    public boolean isInt256() {
        return type == NumberType.INT256;
    }

    public boolean isUInt256() {
        return type == NumberType.UINT256;
    }

    public boolean isFloat256() {
        return type == NumberType.FLOAT256;
    }

    public boolean isUFloat256() {
        return type == NumberType.UFLOAT256;
    }

    public boolean isBigInt() {
        return type == NumberType.BIGINTEGER;
    }

    // ═══════════ 提取 ═══════════

    public long toLong() {
        return switch (type) {
            case LONG -> (long) value;
            case INT256 -> ((Int256) value).longValue();
            case UINT256 -> ((UInt256) value).longValue();
            case FLOAT256 -> ((Float256) value).longValue();
            case UFLOAT256 -> ((UFloat256) value).longValue();
            case BIGINTEGER -> ((BigInteger) value).longValue();
        };
    }

    public Int256 toInt256() {
        return switch (type) {
            case LONG -> Int256.of((long) value);
            case INT256 -> (Int256) value;
            case UINT256 -> ((UInt256) value).toInt256();
            case FLOAT256, UFLOAT256 -> Int256.of(toBigInteger());
            case BIGINTEGER -> Int256.of(((BigInteger) value).toByteArray());
        };
    }

    public UInt256 toUInt256() {
        return switch (type) {
            case LONG -> UInt256.of((long) value);
            case INT256 -> UInt256.fromInt256((Int256) value);
            case UINT256 -> (UInt256) value;
            case FLOAT256, UFLOAT256 -> UInt256.of(toBigInteger());
            case BIGINTEGER -> UInt256.of(((BigInteger) value).toByteArray());
        };
    }

    public Float256 toFloat256() {
        return switch (type) {
            case LONG -> Float256.of((long) value);
            case INT256 -> Float256.of((Int256) value);
            case UINT256 -> Float256.of((UInt256) value);
            case FLOAT256 -> (Float256) value;
            case UFLOAT256 -> ((UFloat256) value).toFloat256();
            case BIGINTEGER -> Float256.of((BigInteger) value);
        };
    }

    public UFloat256 toUFloat256() {
        return switch (type) {
            case LONG -> UFloat256.of(Math.abs((long) value));
            case INT256 -> UFloat256.of(((Int256) value).abs());
            case UINT256 -> UFloat256.of((UInt256) value);
            case FLOAT256 -> ((Float256) value).toUFloat256();
            case UFLOAT256 -> (UFloat256) value;
            case BIGINTEGER -> UFloat256.of(((BigInteger) value).abs());
        };
    }

    public BigInteger toBigInteger() {
        return switch (type) {
            case LONG -> BigInteger.valueOf((long) value);
            case INT256 -> ((Int256) value).toBigInteger();
            case UINT256 -> ((UInt256) value).toBigInteger();
            // 🔧 MCRe：走精确截断/无符号取整路径，避免 toBigDecimal() 的十进制展开开销
            case FLOAT256 -> ((Float256) value).truncate().toBigInteger();
            case UFLOAT256 -> ((UFloat256) value).toUInt256().toBigInteger();
            case BIGINTEGER -> (BigInteger) value;
        };
    }

    // ═══════════ 核心运算 ═══════════
    public DynamicNumber add(DynamicNumber o) {
        NumberType rt = NumberType.wider(type, o.type);
        // 🔧 混用有符号/无符号时，统一升级到有符号容器（BIGINTEGER 或 INT256）
        if (rt == NumberType.UINT256 &&
                (type == NumberType.LONG || type == NumberType.INT256 || o.type == NumberType.LONG || o.type == NumberType.INT256)) {
            // 任一方是符号类型 → 用 BigInteger 兜底，避免无符号回绕
            return DynamicNumber.of(toBigInteger().add(o.toBigInteger()));
        }
        if (rt == NumberType.UFLOAT256 &&
                (type == NumberType.FLOAT256 || o.type == NumberType.FLOAT256
                        || type == NumberType.LONG || o.type == NumberType.LONG
                        || type == NumberType.INT256 || o.type == NumberType.INT256)) {
            return DynamicNumber.of(toFloat256().add(o.toFloat256()));
        }
        return switch (rt) {
            case LONG -> of(toLong() + o.toLong());
            case INT256 -> of(toInt256().add(o.toInt256()));
            case UINT256 -> of(toUInt256().add(o.toUInt256()));
            case FLOAT256 -> of(toFloat256().add(o.toFloat256()));
            case UFLOAT256 -> of(toUFloat256().add(o.toUFloat256()));
            case BIGINTEGER -> of(toBigInteger().add(o.toBigInteger()));
        };
    }

    public DynamicNumber subtract(DynamicNumber o) {
        NumberType rt = NumberType.wider(type, o.type);
        // 🔧 混用有符号/无符号时，统一升级到有符号容器（BIGINTEGER 或 INT256）
        if (rt == NumberType.UINT256 &&
                (type == NumberType.LONG || type == NumberType.INT256 || o.type == NumberType.LONG || o.type == NumberType.INT256)) {
            // 任一方是符号类型 → 用 BigInteger 兜底，避免无符号回绕
            return DynamicNumber.of(toBigInteger().subtract(o.toBigInteger()));
        }
        if (rt == NumberType.UFLOAT256 &&
                (type == NumberType.FLOAT256 || o.type == NumberType.FLOAT256
                        || type == NumberType.LONG || o.type == NumberType.LONG
                        || type == NumberType.INT256 || o.type == NumberType.INT256)) {
            return DynamicNumber.of(toFloat256().subtract(o.toFloat256()));
        }
        return switch (rt) {
            case LONG -> of(toLong() - o.toLong());
            case INT256 -> of(toInt256().subtract(o.toInt256()));
            case UINT256 -> of(toUInt256().subtract(o.toUInt256()));
            case FLOAT256 -> of(toFloat256().subtract(o.toFloat256()));
            case UFLOAT256 -> of(toUFloat256().subtract(o.toUFloat256()));
            case BIGINTEGER -> of(toBigInteger().subtract(o.toBigInteger()));
        };
    }

    public DynamicNumber multiply(DynamicNumber o) {
        NumberType rt = NumberType.wider(type, o.type);
        // 🔧 混用有符号/无符号时，统一升级到有符号容器（BIGINTEGER 或 INT256）
        if (rt == NumberType.UINT256 &&
                (type == NumberType.LONG || type == NumberType.INT256 || o.type == NumberType.LONG || o.type == NumberType.INT256)) {
            // 任一方是符号类型 → 用 BigInteger 兜底，避免无符号回绕
            return DynamicNumber.of(toBigInteger().multiply(o.toBigInteger()));
        }
        if (rt == NumberType.UFLOAT256 &&
                (type == NumberType.FLOAT256 || o.type == NumberType.FLOAT256
                        || type == NumberType.LONG || o.type == NumberType.LONG
                        || type == NumberType.INT256 || o.type == NumberType.INT256)) {
            return DynamicNumber.of(toFloat256().multiply(o.toFloat256()));
        }
        return switch (rt) {
            case LONG -> of(toLong() * o.toLong());
            case INT256 -> of(toInt256().multiply(o.toInt256()));
            case UINT256 -> of(toUInt256().multiply(o.toUInt256()));
            case FLOAT256 -> of(toFloat256().multiply(o.toFloat256()));
            case UFLOAT256 -> of(toUFloat256().multiply(o.toUFloat256()));
            case BIGINTEGER -> of(toBigInteger().multiply(o.toBigInteger()));
        };
    }

    public DynamicNumber divide(DynamicNumber o) {
        NumberType rt = NumberType.wider(type, o.type);
        // 🔧 混用有符号/无符号时，统一升级到有符号容器（BIGINTEGER 或 INT256）
        if (rt == NumberType.UINT256 &&
                (type == NumberType.LONG || type == NumberType.INT256 || o.type == NumberType.LONG || o.type == NumberType.INT256)) {
            // 任一方是符号类型 → 用 BigInteger 兜底，避免无符号回绕
            return DynamicNumber.of(toBigInteger().divide(o.toBigInteger()));
        }
        if (rt == NumberType.UFLOAT256 &&
                (type == NumberType.FLOAT256 || o.type == NumberType.FLOAT256
                        || type == NumberType.LONG || o.type == NumberType.LONG
                        || type == NumberType.INT256 || o.type == NumberType.INT256)) {
            return DynamicNumber.of(toFloat256().divide(o.toFloat256()));
        }
        return switch (rt) {
            case LONG -> of(toLong() / o.toLong());
            case INT256 -> of(toInt256().divide(o.toInt256()));
            case UINT256 -> of(toUInt256().divide(o.toUInt256()));
            case FLOAT256 -> of(toFloat256().divide(o.toFloat256()));
            case UFLOAT256 -> of(toUFloat256().divide(o.toUFloat256()));
            case BIGINTEGER -> of(toBigInteger().divide(o.toBigInteger()));
        };
    }

    /** 取绝对值 */
    public DynamicNumber abs() {
        return switch (type) {
            case LONG -> of(Math.abs((long) value));
            case INT256 -> of(((Int256) value).abs());
            case UINT256 -> this;
            case FLOAT256 -> of(((Float256) value).abs());
            case UFLOAT256 -> this;
            case BIGINTEGER -> of(((BigInteger) value).abs());
        };
    }

    /** 取负（无符号类型不支持） */
    public DynamicNumber negate() {
        return switch (type) {
            case LONG -> of(-(long) value);
            case INT256 -> of(((Int256) value).negate());
            case FLOAT256 -> of(((Float256) value).negate());
            case BIGINTEGER -> of(((BigInteger) value).negate());
            case UINT256, UFLOAT256 ->
                    throw new ArithmeticException("cannot negate unsigned type: " + type);
        };
    }

    // ═══════════ 位运算 ═══════════

    public DynamicNumber shiftLeft(int n) {
        return switch (type) {
            case LONG -> DynamicNumber.of((long) value << n);
            case INT256 -> DynamicNumber.of(((Int256) value).shiftLeft(n));
            case UINT256 -> DynamicNumber.of(((UInt256) value).shiftLeft(n));
            case BIGINTEGER -> DynamicNumber.of(((BigInteger) value).shiftLeft(n));
            default -> this;
        };
    }

    public DynamicNumber shiftRight(int n) {
        return switch (type) {
            case LONG -> DynamicNumber.of((long) value >> n);
            case INT256 -> DynamicNumber.of(((Int256) value).shiftRight(n));
            case UINT256 -> DynamicNumber.of(((UInt256) value).shiftRight(n));
            case BIGINTEGER -> DynamicNumber.of(((BigInteger) value).shiftRight(n));
            default -> this;
        };
    }

    // ═══════════ 比较 ═══════════

    @Override
    public int compareTo(DynamicNumber o) {
        NumberType rt = NumberType.wider(type, o.type);
        if (rt == NumberType.UINT256 &&
                (type == NumberType.LONG || type == NumberType.INT256
                        || o.type == NumberType.LONG || o.type == NumberType.INT256)) {
            return toBigInteger().compareTo(o.toBigInteger());
        }
        if (rt == NumberType.UFLOAT256 &&
                (type == NumberType.FLOAT256 || o.type == NumberType.FLOAT256
                        || type == NumberType.LONG || o.type == NumberType.LONG
                        || type == NumberType.INT256 || o.type == NumberType.INT256)) {
            return toFloat256().compareTo(o.toFloat256());
        }
        return switch (rt) {
            case LONG -> Long.compare(toLong(), o.toLong());
            case INT256 -> toInt256().compareTo(o.toInt256());
            case UINT256 -> toUInt256().compareTo(o.toUInt256());
            case FLOAT256 -> toFloat256().compareTo(o.toFloat256());
            case UFLOAT256 -> toUFloat256().compareTo(o.toUFloat256());
            case BIGINTEGER -> toBigInteger().compareTo(o.toBigInteger());
        };
    }

    // ═══════════ Java Number 接口 ═══════════

    @Override
    public long longValue() {
        return toLong();
    }

    @Override
    public int intValue() {
        return (int) toLong();
    }

    @Override
    public double doubleValue() {
        return toFloat256().doubleValue();
    }

    @Override
    public float floatValue() {
        return (float) toFloat256().doubleValue();
    }

    @Override
    public String toString() {
        return switch (type) {
            case LONG -> Long.toString((long) value);
            case INT256 -> ((Int256) value).toString();
            case UINT256 -> ((UInt256) value).toString();
            case FLOAT256 -> ((Float256) value).toString();
            case UFLOAT256 -> ((UFloat256) value).toString();
            case BIGINTEGER -> ((BigInteger) value).toString();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DynamicNumber other)) return false;
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        if (hash == HASH_NOT_CACHED) hash = toBigInteger().hashCode();
        return hash;
    }

    // ═══════════ 类型提升 ═══════════
    // 提升规则见 NumberType.wider()：LONG < INT256 < UINT256 < FLOAT256 < UFLOAT256 < BIGINTEGER

    // ═══════════ 配置接口 ═══════════

    /** 转换到目标类型（配合 PrecisionConfig 使用）。 LONG 目标对超范围值统一抛 ArithmeticException。 */
    public DynamicNumber convertTo(NumberType targetType) {
        return switch (targetType) {
            case LONG -> DynamicNumber.of(toBigInteger().longValueExact());
            case INT256 -> DynamicNumber.of(toInt256());
            case UINT256 -> DynamicNumber.of(toUInt256());
            case FLOAT256 -> DynamicNumber.of(toFloat256());
            case UFLOAT256 -> DynamicNumber.of(toUFloat256());
            case BIGINTEGER -> DynamicNumber.of(toBigInteger());
        };
    }

    // ═══════════════════════ 测试 ═══════════════════════

    public static void main(String[] args) {
        System.out.println("=== DynamicNumber 验证 ===");
        DynamicNumber a = DynamicNumber.of(100_000);
        DynamicNumber b = DynamicNumber.of(Int256.ONE.shiftLeft(72));
        System.out.println("a (long) = " + a);
        System.out.println("b (Int256) = " + b);
        System.out.println("a + b = " + a.add(b));
        System.out.println("a * b = " + a.multiply(b));
        System.out.println("type of a + b = " + a.add(b).type());
        System.out.println("abs(-5) = " + DynamicNumber.of(-5).abs());
        System.out.println("0.1 + 0.2 = " + DynamicNumber.of(0.1).add(DynamicNumber.of(0.2)));
    }
}
