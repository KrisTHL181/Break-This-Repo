package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.MathContext;

/**
 * UFloat256 — 无符号 256-bit 浮点数 (IEEE 754 风格)
 *
 * <p>布局: 64 指数 + 192 尾数，偏置 2^63-1，RN(GRS) 向偶舍入，零 GC 范围: [0, 2^(2^63)] 精度: 192 bit ≈ 57 位十进制
 *
 * <p>位映射（连续）:
 *
 * <pre>
 *   a = [exp:64]
 *   b = [mantHi:64]
 *   c = [mantMid:64]
 *   d = [mantLo:64]
 * </pre>
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class UFloat256 extends Number implements Comparable<UFloat256> {

    // ═══════════ 位布局常量 ═══════════
    private static final int MANT_BITS = 192;
    private static final int MANT_IMPLIED = MANT_BITS + 1; // 193（含隐含位）
    private static final long EXPONENT_BIAS = 0x7FFF_FFFF_FFFF_FFFFL; // 2^63-1
    private static final long EXPONENT_ALL_ONES = 0xFFFF_FFFF_FFFF_FFFFL;
    private static final long MANT_MASK = 0xFFFF_FFFF_FFFF_FFFFL;

    // ──────── 内部存储 ────────
    final long a; // [exp:64]（无符号）
    final long b; // [mantHi:64]
    final long c; // [mantMid:64]
    final long d; // [mantLo:64]

    // ──────── 缓存 ────────
    private transient int hash;
    private static final int HASH_NOT_CACHED = Integer.MIN_VALUE;
    private transient BigDecimal cachedBigDecimal;

    // ──────── 常量 ────────
    public static final UFloat256 ZERO = new UFloat256(0L, 0L, 0L, 0L);
    public static final UFloat256 ONE = make(EXPONENT_BIAS, 0L, 0L, 0L);
    public static final UFloat256 TWO = make(EXPONENT_BIAS + 1, 0L, 0L, 0L);
    public static final UFloat256 THREE = UFloat256.of(3L);
    public static final UFloat256 TEN = UFloat256.of(10L);
    public static final UFloat256 INF = make(EXPONENT_ALL_ONES, 0L, 0L, 0L);
    public static final UFloat256 NaN = make(EXPONENT_ALL_ONES, 1L, 0L, 0L);

    // ──────── 构造 ────────
    private UFloat256(long a, long b, long c, long d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.hash = HASH_NOT_CACHED;
    }

    static UFloat256 make(long exp, long mantHi, long mantMid, long mantLo) {
        return new UFloat256(exp, mantHi, mantMid, mantLo);
    }

    @Override
    public long longValue() {
        if (isZero()) return 0L;
        UInt256 u = toUInt256();
        return u.longValue();
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        if (isZero()) return 0.0;
        if (isNaN()) return Double.NaN;
        if (isInfinity()) return Double.POSITIVE_INFINITY;
        return toBigDecimal().doubleValue();
    }

    // ═══════════ 字段提取 ═══════════

    private long exponent() {
        return a;
    }

    private long mantissaHi() {
        return b;
    }

    private long mantissaMid() {
        return c;
    }

    private long mantissaLo() {
        return d;
    }

    public boolean isZero() {
        return a == 0L && b == 0L && c == 0L && d == 0L;
    }

    public boolean isInfinity() {
        return a == EXPONENT_ALL_ONES && b == 0L && c == 0L && d == 0L;
    }

    public boolean isNaN() {
        return a == EXPONENT_ALL_ONES && (b != 0L || c != 0L || d != 0L);
    }

    /** 实际指数（有符号）：exp - BIAS（64-bit 补码，无符号下溢自动回绕为负数） */
    private long realExponent() {
        return a - EXPONENT_BIAS;
    }

    // ═══════════ 工厂 ═══════════

    /** 从 double（精确：IEEE 754 位模式转换，53-bit 尾数完整保留） */
    public static UFloat256 of(double value) {
        if (value <= 0.0) return ZERO;
        if (Double.isNaN(value) || Double.isInfinite(value)) return NaN;
        long bits = Double.doubleToRawLongBits(value);
        int expBits = (int) ((bits >>> 52) & 0x7FF);
        long mantBits = bits & 0xFFFF_FFFF_FFFFFL;
        if (expBits == 0) {
            // 次正规: mantBits × 2^-1074
            if (mantBits == 0) return ZERO;
            return of(UInt256.of(mantBits)).scaleExp(-1074);
        }
        if (expBits == 0x7FF) return NaN;
        // 正规: (2^52 | mantBits) × 2^(expBits - 1023 - 52)
        UInt256 mant = UInt256.of(0, 0, 0, mantBits).or(UInt256.of(0, 0, 0, 1L << 52));
        return of(mant).scaleExp(expBits - 1075);
    }

    /** 从无符号 long（精确） */
    public static UFloat256 of(long value) {
        if (value == 0) return ZERO;
        return of(UInt256.of(value));
    }

    /** 从 UInt256（精确） */
    public static UFloat256 of(UInt256 value) {
        if (value.isZero()) return ZERO;
        int bitLen = value.bitLength();
        UInt256 mant = value.shiftLeft(MANT_IMPLIED - bitLen); // 最高位 → bit192（隐含位）
        long exp = EXPONENT_BIAS + bitLen - 1;
        return make(exp, mant.b, mant.c, mant.d);
    }

    /** 从 Int256（要求非负） */
    public static UFloat256 of(Int256 value) {
        if (value.isNegative())
            throw new IllegalArgumentException("UFloat256 cannot be negative: " + value);
        if (value.isZero()) return ZERO;
        return of(UInt256.fromInt256(value));
    }

    /** 从 BigInteger（要求非负；超 256 bit 时截断高 256 bit 并补偿指数） */
    public static UFloat256 of(BigInteger value) {
        if (value.signum() < 0) throw new IllegalArgumentException("UFloat256 cannot be negative");
        if (value.signum() == 0) return ZERO;
        int bitLen = value.bitLength();
        if (bitLen <= 256) {
            return of(UInt256.of(value));
        }
        UInt256 top = UInt256.of(value.shiftRight(bitLen - 256));
        return of(top).scaleExp(bitLen - 256);
    }

    /** 从 BigDecimal（scale 按 log2(10) 近似调整指数） */
    public static UFloat256 of(BigDecimal value) {
        if (value.signum() <= 0) return ZERO;
        BigInteger unscaled = value.unscaledValue().abs();
        int scale = value.scale();

        if (scale == 0) return of(unscaled);
        if (scale < 0) {
            BigInteger intVal = unscaled.multiply(BigInteger.TEN.pow(-scale));
            return of(intVal);
        }

        BigInteger pow10 = BigInteger.TEN.pow(scale);
        int targetBits = 240;
        long K = (long) targetBits - unscaled.bitLength() + pow10.bitLength();

        BigInteger num, denom;
        if (K >= 0) {
            num = unscaled.shiftLeft((int) K);
            denom = pow10;
        } else {
            num = unscaled;
            denom = pow10.shiftLeft((int) -K);
        }

        BigInteger[] qr = num.divideAndRemainder(denom);
        BigInteger q = qr[0];
        boolean exact = qr[1].signum() == 0;
        if (q.signum() == 0) return ZERO;

        while (q.bitLength() > 250) {
            int drop = q.bitLength() - 250;
            BigInteger low = q.and(BigInteger.ONE.shiftLeft(drop).subtract(BigInteger.ONE));
            q = q.shiftRight(drop);
            K -= drop;
            if (low.signum() != 0) exact = false;
        }

        BigInteger qSticky = exact ? q.shiftLeft(1) : q.shiftLeft(1).or(BigInteger.ONE);
        UInt256 qInt = UInt256.of(qSticky);

        // UFloat256 的隐含位在 bit192，约定不同：
        // 值 = M_implied × 2^(exp - BIAS - 192)，M_implied 是 193 位
        // 若用 qSticky（241 位）和 K_eff = K+1：exp = BIAS + 192 - K_eff = BIAS + 191 - K
        long exp = EXPONENT_BIAS + 191 - K;
        if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;

        return roundAndPack(exp, qInt);
    }

    /** 指数加 delta（有符号 long），下溢→0，上溢→Inf */
    private UFloat256 scaleExp(long delta) {
        if (delta == 0) return this;
        long exp = a + delta; // 补码回绕
        if (delta > 0) {
            if (Long.compareUnsigned(exp, a) < 0) return INF; // 回绕 → 上溢
            if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
        } else {
            if (Long.compareUnsigned(exp, a) > 0) return ZERO; // 回绕 → 数学负 → 极小
        }
        return make(exp, b, c, d);
    }

    // ═══════════ 舍入核心 (GRS + RN) ═══════════

    /** 将 mant（UInt256）舍入到 193 bit（含隐含位），RN 向偶舍入，打包为 UFloat256 */
    private static UFloat256 roundAndPack(long exp, UInt256 mant) {
        if (mant.isZero()) return ZERO;

        int bitLen = mant.bitLength();
        int shift = bitLen - MANT_IMPLIED;

        if (shift > 0) {
            long G = mant.testBit(shift - 1) ? 1L : 0L;
            long R = shift >= 2 && mant.testBit(shift - 2) ? 1L : 0L;
            long S = (shift >= 2 && !mant.and(mant.maskBelow(shift - 2)).isZero()) ? 1L : 0L;
            mant = mant.shiftRight(shift);
            // 归一化：指数 + shift（64-bit 无符号）
            exp += shift;
            if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
            boolean increment = G == 1 && (R == 1 || S == 1 || mant.lowBit() == 1);
            if (increment) {
                mant = mant.add(UInt256.ONE);
                if (mant.bitLength() > MANT_IMPLIED) {
                    mant = mant.shiftRight(1);
                    exp++;
                    if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
                }
            }
        } else if (shift < 0) {
            // 理论上不会发生（调用方保证归一化），防御性左移
            mant = mant.shiftLeft(-shift);
            exp += shift; // 负
            if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return ZERO; // 回绕下溢
        }

        // 去掉隐含位：保留低 192 bit
        mant = mant.and(mant.maskBelow(MANT_BITS));

        // 上溢 → Inf（exp 编码 ≥ 2^64-1）
        if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
        // exp 编码是 64-bit 无符号，任意值（含 exp < BIAS 的小数指数）都合法

        return make(exp, mant.b, mant.c, mant.d);
    }

    // ═══════════ 加法 ═══════════

    public UFloat256 add(UFloat256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (isZero()) return o;
        if (o.isZero()) return this;
        if (isInfinity() || o.isInfinity()) return INF;

        long exp1 = exponent();
        long exp2 = o.exponent();

        UInt256 M1 = mantissaWithImplied();
        UInt256 M2 = o.mantissaWithImplied();

        // 对齐指数（无符号比较）
        int cmp = Long.compareUnsigned(exp1, exp2);
        if (cmp > 0) {
            long diff = exp1 - exp2;
            M2 = diff > 300 ? UInt256.ZERO : M2.shiftRight((int) diff);
        } else if (cmp < 0) {
            long diff = exp2 - exp1;
            M1 = diff > 300 ? UInt256.ZERO : M1.shiftRight((int) diff);
            exp1 = exp2;
        }

        return roundAndPack(exp1, M1.add(M2));
    }

    // ═══════════ 减法（无符号，下溢归零） ═══════════

    public UFloat256 subtract(UFloat256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (o.isZero()) return this;
        if (isZero()) return ZERO;
        if (isInfinity() && o.isInfinity()) return NaN;
        if (isInfinity()) return INF;
        if (this.compareTo(o) < 0) return ZERO;

        long exp1 = exponent();
        long exp2 = o.exponent();

        UInt256 M1 = mantissaWithImplied();
        UInt256 M2 = o.mantissaWithImplied();

        int cmp = Long.compareUnsigned(exp1, exp2);
        if (cmp > 0) {
            long diff = exp1 - exp2;
            M2 = diff > 300 ? UInt256.ZERO : M2.shiftRight((int) diff);
        } else if (cmp < 0) {
            long diff = exp2 - exp1;
            M1 = diff > 300 ? UInt256.ZERO : M1.shiftRight((int) diff);
            exp1 = exp2;
        }

        return roundAndPack(exp1, M1.subtract(M2));
    }

    /** 尾数 + 隐含位（UInt256，隐含位在 bit192 = a 的 bit0） */
    private UInt256 mantissaWithImplied() {
        return UInt256.of(0L, b, c, d).or(UInt256.of(1L, 0L, 0L, 0L));
    }

    // ═══════════ 乘法 ═══════════

    public UFloat256 multiply(UFloat256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (isZero() || o.isZero()) return ZERO;
        if (isInfinity() || o.isInfinity()) return INF;

        // E_in = re1 + re2 + BIAS（realExp 域运算，防 64-bit 无符号加法回绕）
        long re1 = a - EXPONENT_BIAS; // 补码 realExp
        long re2 = o.a - EXPONENT_BIAS;
        long re;
        try {
            re = Math.addExact(re1, re2);
        } catch (ArithmeticException ex) {
            // 同号溢出：同负 → 下溢 ZERO；同正 → 上溢 INF
            return (re1 < 0) ? ZERO : INF;
        }
        if (re < -(EXPONENT_BIAS)) return ZERO; // E_in < 0 → 极小值归零
        long exp = re + EXPONENT_BIAS; // 无符号 64-bit（re ≤ 2^63-1 → exp ≤ 2^64-2）

        UInt256 M1 = mantissaWithImplied();
        UInt256 M2 = o.mantissaWithImplied();
        // 🔧 修复：193×193-bit 乘积需要 386 bit，超 UInt256 256-bit 容量。
        // 用 long[7]（448 bit）小端累加器 + 专用 roundAndPackUF（GRS 舍入）
        long[] x = {M1.d, M1.c, M1.b, M1.a}; // 小端 4 limb（隐含位在 a bit0）
        long[] y = {M2.d, M2.c, M2.b, M2.a};
        long[] r = new long[8]; // 4×4 → 最大 idx+1=7，需 8 limb（512 bit）
        for (int i = 0; i < 4; i++) {
            long xi = x[i];
            if (xi == 0L) continue;
            for (int j = 0; j < 4; j++) {
                long yj = y[j];
                if (yj == 0L) continue;
                addTo7(r, i + j, xi * yj, Math.unsignedMultiplyHigh(xi, yj));
            }
        }
        return roundAndPackUF(r, exp);
    }

    // ═══════════ 448-bit 乘法累加（零 GC） ═══════════

    private static void addTo7(long[] r, int idx, long lo, long hi) {
        long carry = addToLimb7(r, idx, lo);
        long h = hi + carry;
        long extra = (Long.compareUnsigned(h, hi) < 0) ? 1L : 0L;
        long total = addToLimb7(r, idx + 1, h) + extra;
        for (int k = idx + 2; total != 0 && k < r.length; k++) {
            total = addToLimb7(r, k, total);
        }
    }

    private static long addToLimb7(long[] r, int k, long add) {
        long old = r[k];
        long s = old + add;
        r[k] = s;
        return (Long.compareUnsigned(s, old) < 0) ? 1L : 0L;
    }

    private static long bitAt7(long[] r, int bit) {
        if (bit < 0) return 0L;
        return (r[bit >> 6] >>> (bit & 63)) & 1L;
    }

    private static boolean hasBitsBelow7(long[] r, int limit) {
        if (limit <= 0) return false;
        int w = limit >> 6;
        int b = limit & 63;
        for (int i = 0; i < w && i < r.length; i++) {
            if (r[i] != 0L) return true;
        }
        if (w < r.length && b > 0) {
            long mask = (1L << b) - 1;
            if ((r[w] & mask) != 0L) return true;
        }
        return false;
    }

    /** 448-bit 右移 shift 位，取低 4 limb（小端，隐含位在 limb3 bit0） */
    private static long[] shr4(long[] r, int shift) {
        long[] out = new long[4];
        int w = shift >> 6, b = shift & 63;
        for (int i = 0; i < 4; i++) {
            int hi = i + w;
            long high = hi < r.length ? r[hi] : 0L;
            long low = (hi + 1 < r.length) ? r[hi + 1] : 0L;
            out[i] = (b == 0) ? high : (high >>> b) | (low << (64 - b));
        }
        return out;
    }

    /**
     * 乘积舍入打包：r（小端 448-bit）→ UFloat256，RN 向偶。 🔧 exp' = E_in + shift - MANT_BITS（乘积是 384/385 bit
     * 隐含位基数，需减 192 补偿）
     */
    private static UFloat256 roundAndPackUF(long[] r, long exp) {
        int bitLen = 0;
        for (int i = 6; i >= 0; i--) {
            if (r[i] != 0L) {
                bitLen = (i << 6) + (64 - Long.numberOfLeadingZeros(r[i]));
                break;
            }
        }
        if (bitLen == 0) return ZERO;
        int shift = bitLen - MANT_IMPLIED; // 归一化：通常 192 或 193
        long G = bitAt7(r, shift - 1);
        long R = (shift >= 2) ? bitAt7(r, shift - 2) : 0L;
        long S = (shift >= 2 && hasBitsBelow7(r, shift - 2)) ? 1L : 0L;
        long[] m = shr4(r, shift);
        // exp 补偿：E_in + shift - MANT_BITS
        exp += shift - MANT_BITS;
        if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
        boolean increment = G == 1 && (R == 1 || S == 1 || (m[0] & 1L) == 1);
        if (increment) {
            m[0] += 1;
            boolean carry = m[0] == 0;
            if (carry) {
                m[1] += 1;
                carry = m[1] == 0;
            }
            if (carry) {
                m[2] += 1;
                carry = m[2] == 0;
            }
            if (carry) {
                m[3] += 1;
            }
            // 舍入进位使 mantissa 变 2^193 → 右移 1 位，指数 +1
            if ((m[3] & 0x2L) != 0L) { // bit193 set（limb3 bit1）
                m[3] = (m[3] >>> 1) | (m[2] << 63);
                m[2] = (m[2] >>> 1) | (m[1] << 63);
                m[1] = (m[1] >>> 1) | (m[0] << 63);
                m[0] >>>= 1;
                exp++;
                if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
            }
        }
        if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;
        return make(exp, m[2], m[1], m[0]);
    }

    // ═══════════ 除法 ═══════════

    public UFloat256 divide(UFloat256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (o.isZero()) return INF;
        if (isZero()) return ZERO;
        if (isInfinity()) {
            if (o.isInfinity()) return NaN;
            return INF;
        }

        // E_in = re1 - re2 + BIAS - 1
        long re1 = a - EXPONENT_BIAS; // 补码 realExp
        long re2 = o.a - EXPONENT_BIAS;
        long re;
        try {
            re = Math.subtractExact(re1, re2);
        } catch (ArithmeticException ex) {
            // 异号溢出：re1 负且 re2 正 → 极小 ZERO；re1 正且 re2 负 → 巨大 INF
            return (re1 < 0) ? ZERO : INF;
        }
        if (re < 1 - EXPONENT_BIAS) return ZERO; // E_in < 0 → 极小值归零
        long exp = re + EXPONENT_BIAS - 1; // 无符号 64-bit
        if (Long.compareUnsigned(exp, EXPONENT_ALL_ONES) >= 0) return INF;

        UInt256 M1 = mantissaWithImplied();
        UInt256 M2 = o.mantissaWithImplied();
        // 🔧 修复：商 = (M1 << 193) / M2 需 386-bit 中间值，UInt256 只有 256-bit 会截断。
        // 用 BigInteger 精确除法，商 ≤ 194 bit 可装回 UInt256
        BigInteger num = M1.toBigInteger().shiftLeft(MANT_IMPLIED);
        BigInteger den = M2.toBigInteger();
        UInt256 quot = UInt256.of(num.divide(den));

        return roundAndPack(exp, quot);
    }

    // ═══════════ 比较 ═══════════

    @Override
    public int compareTo(UFloat256 o) {
        if (isNaN() || o.isNaN()) return 0;
        if (isZero() && o.isZero()) return 0;
        int cmp = Long.compareUnsigned(exponent(), o.exponent());
        if (cmp != 0) return cmp;
        cmp = Long.compareUnsigned(mantissaHi(), o.mantissaHi());
        if (cmp != 0) return cmp;
        cmp = Long.compareUnsigned(mantissaMid(), o.mantissaMid());
        if (cmp != 0) return cmp;
        return Long.compareUnsigned(mantissaLo(), o.mantissaLo());
    }

    // ═══════════ 转换 ═══════════

    /** 直接位转换：mant 192→176 bit（GRS 舍入）+ 指数 re-bias（2^78-2^63） */
    public Float256 toFloat256() {
        if (isZero()) return Float256.ZERO;
        if (isNaN()) return Float256.NaN;
        if (isInfinity()) return Float256.POS_INF;

        UInt256 mant = mantissaWithImplied();
        // 192+1 → 176+1：右移 16 bit
        long G = mant.testBit(15) ? 1L : 0L;
        long R = mant.testBit(14) ? 1L : 0L;
        long S = !mant.and(mant.maskBelow(14)).isZero() ? 1L : 0L;
        mant = mant.shiftRight(16);
        boolean inc = G == 1 && (R == 1 || S == 1 || mant.lowBit() == 1);
        boolean carryExp = false;
        if (inc) {
            mant = mant.add(UInt256.ONE);
            if (mant.bitLength() > 177) {
                mant = mant.shiftRight(1);
                carryExp = true;
            }
        }
        // 指数 re-bias: e' = e + 2^78 - 2^63（79-bit (hi, lo) 表示）
        long hi = (a >>> 16) + 0x3FFF_8000_0000_0000L;
        long lo = a & 0xFFFF;
        if (carryExp) {
            lo++;
            if (lo > 0xFFFF) {
                lo = 0;
                hi++;
                if (hi > 0x7FFF_FFFF_FFFF_FFFFL) return Float256.POS_INF;
            }
        }
        return Float256.make(hi, lo, mant.b & 0x0000_FFFF_FFFF_FFFFL, mant.c, mant.d, 1);
    }

    public UInt256 toUInt256() {
        if (isZero()) return UInt256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        // 🔧 修复：值 = mant × 2^(realExp - 192)，需按基数 192 修正移位
        UInt256 mant = mantissaWithImplied();
        long realExp = realExponent();
        if (realExp >= MANT_BITS) {
            if (realExp >= 255) throw new ArithmeticException("UFloat256 out of UInt256 range");
            return mant.shiftLeft((int) (realExp - MANT_BITS));
        }
        if (realExp < 0) return UInt256.ZERO;
        return mant.shiftRight((int) (MANT_BITS - realExp));
    }

    public BigDecimal toBigDecimal() {
        if (isZero()) return BigDecimal.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        BigInteger mant = mantissaWithImplied().toBigInteger();
        long realExp = realExponent() - MANT_BITS;

        BigDecimal dec;
        if (realExp == 0) {
            dec = new BigDecimal(mant, 0);
        } else if (realExp > 0) {
            if (realExp > Integer.MAX_VALUE) {
                throw new ArithmeticException("UFloat256 exponent too large for BigDecimal");
            }
            dec = new BigDecimal(mant.shiftLeft((int) realExp), 0);
        } else {
            long negExp = -realExp;
            if (negExp > Integer.MAX_VALUE) {
                throw new ArithmeticException("UFloat256 exponent too small for BigDecimal");
            }
            BigInteger denom = BigInteger.ONE.shiftLeft((int) negExp);
            dec = new BigDecimal(mant).divide(new BigDecimal(denom), MathContext.UNLIMITED);
        }
        return dec;
    }

    // ═══════════ 精确取整（返回 UInt256，零损失） ═══════════

    /** 向零截断（无符号下即向下取整） */
    // 🔧 修复：移位基数 192 修正
    public UInt256 truncate() {
        if (isZero()) return UInt256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        UInt256 mant = mantissaWithImplied();
        long realExp = realExponent();
        if (realExp >= MANT_BITS) {
            if (realExp >= 255) throw new ArithmeticException("UFloat256 too large for UInt256");
            return mant.shiftLeft((int) (realExp - MANT_BITS));
        }
        if (realExp < 0) return UInt256.ZERO; // 极小值 → 0
        return mant.shiftRight((int) (MANT_BITS - realExp));
    }

    /** 向下取整（无符号下即截断） */
    public UInt256 floor() {
        return truncate();
    }

    /** 向上取整（+∞ 方向） */
    // 🔧 修复：移位基数 192 修正；realExp<0 时 0<值<1 → ceil=1
    public UInt256 ceil() {
        if (isZero()) return UInt256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        UInt256 mant = mantissaWithImplied();
        long realExp = realExponent();
        if (realExp >= MANT_BITS) {
            if (realExp >= 255) throw new ArithmeticException("UFloat256 too large for UInt256");
            return mant.shiftLeft((int) (realExp - MANT_BITS));
        }
        if (realExp < 0) return UInt256.ONE; // 0 < 值 < 1 → ceil = 1
        UInt256 abs = mant.shiftRight((int) (MANT_BITS - realExp));
        boolean dropped = !mant.and(mant.maskBelow((int) (MANT_BITS - realExp))).isZero();
        return dropped ? abs.add(UInt256.ONE) : abs;
    }

    /** 四舍五入（half-up） */
    public UInt256 round() {
        return this.add(UFloat256.of(0.5)).floor();
    }

    @Override
    public String toString() {
        if (isNaN()) return "NaN";
        if (isZero()) return "0";
        if (isInfinity()) return "Infinity";
        return toBigDecimal().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UFloat256 other)) return false;
        if (isNaN() || other.isNaN()) return false;
        if (isZero() && other.isZero()) return true;
        return a == other.a && b == other.b && c == other.c && d == other.d;
    }

    @Override
    public int hashCode() {
        if (hash == HASH_NOT_CACHED) hash = (int) (a ^ b ^ c ^ d);
        return hash;
    }

    // ═══════════════════════ 测试 ═══════════════════════

    public static void main(String[] args) {
        System.out.println("=== UFloat256 验证 (含舍入) ===");
        UFloat256 a = UFloat256.of(1);
        UFloat256 b = UFloat256.of(3);
        System.out.println("1   = " + ONE);
        System.out.println("2   = " + TWO);
        System.out.println("1/3 = " + a.divide(b));
        System.out.println("1/3*3 = " + a.divide(b).multiply(UFloat256.of(3)));
        System.out.println("1+2 = " + a.add(UFloat256.of(2)));
        System.out.println("2^100 = " + UFloat256.of(UInt256.ONE.shiftLeft(100)));
        System.out.println("0.1+0.2 = " + of(0.1).add(of(0.2)));
        System.out.println("toFloat256(1.5) = " + of(1.5).toFloat256());
        System.out.println("toUInt256(2^100) = " + of(UInt256.ONE.shiftLeft(100)).toUInt256());
        System.out.println("MAX+1 = " + INF.add(UFloat256.of(1)));
        System.out.println("1/0 = " + ONE.divide(ZERO));
        System.out.println("0/0 = " + ZERO.divide(ZERO));
    }
}
