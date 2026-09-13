package net.MinecraftTools.Math.DynamicAccuracy;

import java.io.*;

/**
 * MathContext — 任意精度运算上下文
 *
 * <p>MCRe NoiseFarlands 性能优化： - 常用精度组合池化（0-128 precision × 8 rounding modes ≈ 1032 个） -
 * cachedToString 延迟计算，避免每次重复拼接
 */
public final class MathContext implements Serializable {

    private static final RoundingMode DEFAULT_ROUNDINGMODE = RoundingMode.HALF_UP;
    private static final int MIN_DIGITS = 0;

    @java.io.Serial private static final long serialVersionUID = 5579720004786848255L;

    // ────────────── 静态常量（不变）──────────────
    public static final MathContext UNLIMITED = new MathContext(0, RoundingMode.HALF_UP);
    public static final MathContext DECIMAL32 = new MathContext(7, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL64 = new MathContext(16, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL128 = new MathContext(34, RoundingMode.HALF_EVEN);

    // ────────────── 🔧 优化1: 精度池 ──────────────
    // 覆盖 precision 0..128 × 8 rounding modes = 1032个
    // 这是 BigDecimal 内部最常见的临时 MathContext 创建范围
    private static final int POOL_SIZE = 128;
    private static final MathContext[][] PRECISION_POOL = new MathContext[POOL_SIZE + 1][RoundingMode.values().length];

    static {
        for (int p = 0; p <= POOL_SIZE; p++) {
            for (RoundingMode rm : RoundingMode.values()) {
                PRECISION_POOL[p][rm.ordinal()] = new MathContext(p, rm);
            }
        }
    }

    /** 🔧 池化获取 — 避免重复 new。 BigDecimal 内 createTempContext(precision, mode) 调用此方法。 */
    public static MathContext getCached(int precision, RoundingMode rm) {
        if (precision >= 0 && precision <= POOL_SIZE) {
            return PRECISION_POOL[precision][rm.ordinal()];
        }
        return new MathContext(precision, rm);
    }

    // ────────────── 字段 ──────────────
    final int precision;
    final RoundingMode roundingMode;

    // ────────────── 🔧 优化2: 懒缓存 toString ──────────────
    private transient volatile String stringCache;

    // ────────────── 🔧 优化3: 懒缓存 hashCode ──────────────
    private transient volatile int hashCodeCache;
    private static final int HASH_NOT_CACHED = -1;

    // ────────────── 构造器 ──────────────
    public MathContext(int setPrecision) {
        this(setPrecision, DEFAULT_ROUNDINGMODE);
    }

    public MathContext(int setPrecision, RoundingMode setRoundingMode) {
        if (setPrecision < MIN_DIGITS)
            throw new IllegalArgumentException("Digits < 0");
        if (setRoundingMode == null)
            throw new NullPointerException("null RoundingMode");

        precision = setPrecision;
        roundingMode = setRoundingMode;
    }

    public MathContext(String val) {
        int setPrecision;
        if (val == null)
            throw new NullPointerException("null String");
        try {
            if (!val.startsWith("precision="))
                throw new RuntimeException();
            int fence = val.indexOf(' ');
            int off = 10;
            setPrecision = Integer.parseInt(val.substring(10, fence));

            if (!val.startsWith("roundingMode=", fence + 1))
                throw new RuntimeException();
            off = fence + 1 + 13;
            String str = val.substring(off, val.length());
            roundingMode = RoundingMode.valueOf(str);
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("bad string format");
        }

        if (setPrecision < MIN_DIGITS)
            throw new IllegalArgumentException("Digits < 0");

        precision = setPrecision;
    }

    public int getPrecision() {
        return precision;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    @Override
    public boolean equals(Object x) {
        if (!(x instanceof MathContext mc))
            return false;
        return mc.precision == this.precision && mc.roundingMode == this.roundingMode;
    }

    @Override
    public int hashCode() {
        // 🔧 懒缓存的 hashCode — 如果被 HashMap 维护则缓存
        int h = hashCodeCache;
        if (h == HASH_NOT_CACHED) {
            h = this.precision + roundingMode.hashCode() * 59;
            hashCodeCache = h;
        }
        return h;
    }

    @Override
    public String toString() {
        // 🔧 懒缓存的 toString — 反复日志打印也能丝般顺滑
        String sc = stringCache;
        if (sc == null) {
            sc = "precision=" + precision + " roundingMode=" + roundingMode.toString();
            stringCache = sc;
        }
        return sc;
    }

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (precision < MIN_DIGITS) {
            throw new java.io.StreamCorruptedException("MathContext: invalid digits in stream");
        }
        if (roundingMode == null) {
            throw new java.io.StreamCorruptedException("MathContext: null roundingMode in stream");
        }
    }
}