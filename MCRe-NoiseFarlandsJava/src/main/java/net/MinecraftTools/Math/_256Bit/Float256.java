package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.MathContext;

/**
 * Float256 — 有符号 256-bit 浮点数 (IEEE 754 风格)
 *
 * <p>布局: 1 符号 + 79 指数 + 176 尾数，偏置 2^78-1，RN(GRS) 向偶舍入 范围: [-2^(2^77), 2^(2^77)] 精度: 176 bit ≈ 52
 * 位十进制
 *
 * <p>位映射（连续）:
 *
 * <pre>
 *   a = [sign:1][expHi:63]
 *   b = [expLo:16][mantHi:48]
 *   c = [mantMid:64]
 *   d = [mantLo:64]
 * </pre>
 *
 * 指数 = (expHi &lt;&lt; 16) | expLo (79 bit)，79-bit 运算用 (expHi, expLo) 对手写，零 GC。
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class Float256 extends Number implements Comparable<Float256> {

    // ═══════════ 位布局常量 ═══════════
    private static final int EXP_BITS = 79;
    private static final int MANT_BITS = 176;
    private static final int MANT_IMPLIED = MANT_BITS + 1; // 177（含隐含位）

    private static final long SIGN_MASK = 0x8000_0000_0000_0000L; // a 的 bit63
    private static final long EXP_HI_MASK = 0x7FFF_FFFF_FFFF_FFFFL; // a 低 63 bit
    private static final long EXP_LO_MASK = 0xFFFF_0000_0000_0000L; // b 高 16 bit
    private static final long MANT_HI_MASK = 0x0000_FFFF_FFFF_FFFFL; // b 低 48 bit

    // 偏置 2^78-1 = 78 个 1 → (expHi=62 个 1, expLo=16 个 1)
    private static final long BIAS_HI = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long BIAS_LO = 0xFFFFL;
    // 指数全 1（79 个 1）= NaN/Inf 哨兵
    private static final long EXP_ALL_HI = 0x7FFF_FFFF_FFFF_FFFFL;
    private static final long EXP_ALL_LO = 0xFFFFL;

    // ──────── 内部存储 ────────
    final long a; // [sign:1][expHi:63]
    final long b; // [expLo:16][mantHi:48]
    final long c; // [mantMid:64]
    final long d; // [mantLo:64]

    // ──────── 缓存 ────────
    private transient int hash;
    private static final int HASH_NOT_CACHED = Integer.MIN_VALUE;
    private transient BigDecimal cachedBigDecimal;

    // ──────── 常量 ────────
    public static final Float256 ZERO = new Float256(0L, 0L, 0L, 0L);
    public static final Float256 ONE = make(BIAS_HI, BIAS_LO, 0L, 0L, 0L, 1);
    // 2 = 1.0 × 2^1，指数 = BIAS+1 = 2^78 → (expHi=2^62, expLo=0)
    public static final Float256 TWO = make(0x4000_0000_0000_0000L, 0L, 0L, 0L, 0L, 1);
    public static final Float256 THREE = of(3L);
    public static final Float256 TEN = of(10L);
    public static final Float256 MINUS_ONE = of(-1L);
    public static final Float256 NaN = make(EXP_ALL_HI, EXP_ALL_LO, 0L, 0L, 1L, 1);
    public static final Float256 POS_INF = make(EXP_ALL_HI, EXP_ALL_LO, 0L, 0L, 0L, 1);
    public static final Float256 NEG_INF = make(EXP_ALL_HI, EXP_ALL_LO, 0L, 0L, 0L, -1);

    // ──────── 构造 ────────
    private Float256(long a, long b, long c, long d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.hash = HASH_NOT_CACHED;
    }

    /** 底层构造：raw 位模式（sign: ±1，expHi/expLo 为 79-bit 指数的拆对） */
    static Float256 make(long expHi, long expLo, long mantHi, long mantMid, long mantLo, int sign) {
        long a = (sign < 0 ? SIGN_MASK : 0L) | (expHi & EXP_HI_MASK);
        long b = ((expLo & 0xFFFF) << 48) | (mantHi & MANT_HI_MASK);
        return new Float256(a, b, mantMid, mantLo);
    }

    // ═══════════ 字段提取 ═══════════

    private long expHi() {
        return a & EXP_HI_MASK;
    } // 63 bit

    private long expLo() {
        return b >>> 48;
    } // 16 bit

    private long mantHi() {
        return b & MANT_HI_MASK;
    } // 48 bit

    private long mantMid() {
        return c;
    }

    private long mantLo() {
        return d;
    }

    public int signum() {
        return (a & SIGN_MASK) != 0 ? -1 : 1;
    }

    /** 实际指数（有符号，64-bit 补码）：exp - BIAS，即 2 的幂次 */
    private long realExponent() {
        long dLo = expLo() - BIAS_LO;
        long borrow = (dLo < 0) ? 1L : 0L;
        long dHi = expHi() - BIAS_HI - borrow;
        return (dHi << 16) | (dLo & 0xFFFF);
    }

    private boolean expIsAll() {
        return expHi() == EXP_ALL_HI && expLo() == EXP_ALL_LO;
    }

    public boolean isZero() {
        return a == 0L && b == 0L && c == 0L && d == 0L;
    }

    public boolean isNaN() {
        return expIsAll() && (mantHi() != 0 || mantMid() != 0 || mantLo() != 0);
    }

    public boolean isInfinity() {
        return expIsAll() && mantHi() == 0 && mantMid() == 0 && mantLo() == 0;
    }

    public boolean isFinite() {
        return !isNaN() && !isInfinity();
    }

    // ═══════════ 79-bit 指数算术（零分配） ═══════════

    /** exp1 + exp2（79-bit 无符号加法，溢出自然进位到 long 高位） */
    private static long[] expAdd(long hi1, long lo1, long hi2, long lo2) {
        long lo = lo1 + lo2;
        long hi = hi1 + hi2 + (lo >>> 16);
        return new long[]{hi, lo & 0xFFFF};
    }

    /** exp1 - exp2（要求 exp1 >= exp2） */
    private static long[] expSub(long hi1, long lo1, long hi2, long lo2) {
        long lo = lo1 - lo2;
        long borrow = (lo < 0) ? 1L : 0L;
        long hi = hi1 - hi2 - borrow;
        return new long[]{hi, lo & 0xFFFF};
    }

    /** 79-bit 无符号比较：返回 -1/0/1 */
    private static int expCmp(long hi1, long lo1, long hi2, long lo2) {
        if (hi1 != hi2) return Long.compareUnsigned(hi1, hi2) < 0 ? -1 : 1;
        if (lo1 != lo2) return Long.compareUnsigned(lo1, lo2) < 0 ? -1 : 1;
        return 0;
    }

    /** 指数加 delta（有符号 long，模 2^64 借位自动传播），下溢→0，上溢→Inf */
    private Float256 scaleExp(long delta) {
        long lo = expLo() + (delta & 0xFFFF);
        long hi = expHi() + (delta >> 16) + (lo >>> 16);
        lo &= 0xFFFF;
        if (hi < 0) return ZERO;
        if (hi > EXP_ALL_HI || (hi == EXP_ALL_HI && lo >= EXP_ALL_LO)) {
            return signum() < 0 ? NEG_INF : POS_INF;
        }
        return make(hi, lo, mantHi(), mantMid(), mantLo(), signum());
    }

    // ═══════════ 工厂方法 ═══════════

    /** 从 double（精确：IEEE 754 位模式转换，53-bit 尾数完整保留） */
    public static Float256 of(double value) {
        if (value == 0.0) return ZERO;
        if (Double.isNaN(value)) return NaN;
        if (Double.isInfinite(value)) return value > 0 ? POS_INF : NEG_INF;
        long bits = Double.doubleToRawLongBits(value);
        boolean neg = (bits >>> 63) != 0;
        int expBits = (int) ((bits >>> 52) & 0x7FF);
        long mantBits = bits & 0xFFFF_FFFF_FFFFFL;
        if (expBits == 0) {
            // 次正规: mantBits × 2^-1074
            if (mantBits == 0) return ZERO;
            Float256 f = of(Int256.of(mantBits));
            if (neg) f = f.negate();
            return f.scaleExp(-1074);
        }
        if (expBits == 0x7FF) {
            return mantBits == 0 ? (neg ? NEG_INF : POS_INF) : NaN;
        }
        // 正规: (2^52 | mantBits) × 2^(expBits - 1023 - 52)
        Int256 mant = Int256.of(0, 0, 0, mantBits).or(Int256.of(0, 0, 0, 1L << 52));
        Float256 f = of(mant);
        if (neg) f = f.negate();
        return f.scaleExp(expBits - 1075);
    }

    /** 从 long（精确） */
    public static Float256 of(long value) {
        if (value == 0) return ZERO;
        return of(Int256.of(value));
    }

    /**
     * 从 Int256 转换（GRS 向偶舍入，不直接截断）。 bitLen ≤ 177 时左对齐隐含位；bitLen > 177 时交给 roundAndPack 内部按 shift
     * 补偿指数。 MIN_VALUE (-2^255) 的 abs 会回绕成位模式 1000...0，需按无符号语义取 bitLen = 256。
     */
    public static Float256 of(Int256 value) {
        if (value.isZero()) return ZERO;
        boolean neg = value.isNegative();
        Int256 abs = neg ? value.negate() : value;

        // MIN_VALUE.negate() 回绕后仍是位模式 1000...0，isNegative() 依然为 true。
        // 此时 abs 的无符号有效位是 256 位，而 Int256.bitLength() 走补码语义只返回 255。
        int bitLen;
        if (neg && abs.isNegative()) {
            bitLen = 256;
        } else {
            bitLen = abs.bitLength();
        }

        Int256 mant;
        long actualExp;
        if (bitLen <= MANT_IMPLIED) {
            mant = abs.shiftLeft(MANT_IMPLIED - bitLen);
            actualExp = bitLen - 1;
        } else {
            // bitLen > 177：roundAndPack 会内部右移 shift = bitLen - 177 位并补 exp，
            // 传入的 exp 必须是 BIAS + 176，不能是 BIAS + (bitLen - 1)
            mant = abs;
            actualExp = MANT_IMPLIED - 1; // = 176
        }

        long offLo = actualExp & 0xFFFFL;
        long offHi = actualExp >> 16;
        long eLo = (BIAS_LO + offLo) & 0xFFFFL;
        long eHi = BIAS_HI + offHi + ((BIAS_LO + offLo) >>> 16);

        return roundAndPack(eHi, eLo, mant, neg ? -1 : 1);
    }

    /** 从 UInt256 转换（GRS 向偶舍入，bitLen > 177 时不再截断）。 */
    public static Float256 of(UInt256 value) {
        if (value.isZero()) return ZERO;
        int bitLen = value.bitLength();
        int shift = bitLen - MANT_IMPLIED; // 需要丢弃的位数

        long expHi, expLo;
        UInt256 mant;

        if (shift <= 0) {
            // 无需舍入：左对齐隐含位到 bit176
            mant = value.shiftLeft(-shift);
            // exp 编码 = BIAS + (bitLen - 1)
            if (bitLen == 1) {
                expHi = BIAS_HI;
                expLo = BIAS_LO;
            } else {
                expHi = 0x4000_0000_0000_0000L;
                expLo = bitLen - 2;
            }
        } else {
            // GRS 向偶舍入
            long G = value.testBit(shift - 1) ? 1L : 0L;
            long R = (shift >= 2 && value.testBit(shift - 2)) ? 1L : 0L;
            long S = 0L;
            if (shift >= 2) {
                S = value.and(value.maskBelow(shift - 2)).isZero() ? 0L : 1L;
            }
            mant = value.shiftRight(shift);
            // exp 编码 = BIAS + (bitLen - 1) = BIAS + shift + 176
            long expVal = BIAS_LO + 176L + shift;
            expLo = expVal & 0xFFFFL;
            expHi = BIAS_HI + (expVal >>> 16);

            boolean increment = G == 1 && (R == 1 || S == 1 || mant.lowBit() == 1);
            if (increment) {
                mant = mant.add(UInt256.ONE);
                if (mant.bitLength() > MANT_IMPLIED) {
                    mant = mant.shiftRight(1);
                    expLo++;
                    if (expLo > 0xFFFF) {
                        expLo = 0;
                        expHi++;
                    }
                }
            }
        }

        // 丢弃隐含位，只保留低 176 位
        mant = mant.and(mant.maskBelow(MANT_BITS));

        if (expHi > EXP_ALL_HI || (expHi == EXP_ALL_HI && expLo >= EXP_ALL_LO)) {
            return POS_INF;
        }
        return make(expHi, expLo, mant.b & MANT_HI_MASK, mant.c, mant.d, 1);
    }

    /** 从 BigInteger 转换（不再用 Int256 装载，避免 ≥2^255 时符号翻转） */
    public static Float256 of(BigInteger value) {
        if (value.signum() == 0) return ZERO;
        boolean neg = value.signum() < 0;
        BigInteger abs = value.abs();
        int bitLen = abs.bitLength();

        if (bitLen <= 256) {
            // 用 UInt256 装载高位，再转 Float256（保留符号）
            byte[] mag = abs.toByteArray();
            byte[] buf = new byte[32];
            if (mag.length > 32) {
                System.arraycopy(mag, mag.length - 32, buf, 0, 32);
            } else {
                System.arraycopy(mag, 0, buf, 32 - mag.length, mag.length);
            }
            UInt256 u = UInt256.of(buf);
            Float256 f = of(u);
            return neg ? f.negate() : f;
        }

        // bitLen > 256：取高 256 bit 并补偿指数
        Int256 top = Int256.of(abs.shiftRight(bitLen - 256));
        // 这里 top 是正数（因为 abs.shiftRight 的最高位仍可能被当负数，
        // 但当 bitLen > 256 时，取高 256 bit 后最高位是 abs 的 bit(bitLen-1)，仍是 1，
        // 会被 Int256 当作负——但我们只关心位模式，转入 UInt256 后按无符号处理）
        UInt256 uTop = UInt256.fromInt256(top);
        Float256 f = of(uTop);
        if (neg) f = f.negate();
        return f.scaleExp(bitLen - 256);
    }

    /** 从 BigDecimal（scale 按 log2(10) 近似调整指数） */
    /**
     * 从 BigDecimal 精确转换到 Float256。 值 = unscaled × 10^(-scale)，按 2 的幂次拆分精确计算，最后 GRS 向偶舍入。 修复：原实现用
     * (long)(scale * log2(10)) 截断，scale=1..3 时相对误差可达 2 倍。
     */
    public static Float256 of(BigDecimal value) {
        if (value.signum() == 0) return ZERO;
        boolean neg = value.signum() < 0;
        BigInteger unscaled = value.unscaledValue().abs();
        int scale = value.scale();

        if (scale == 0) {
            Float256 f = of(unscaled);
            return neg ? f.negate() : f;
        }

        if (scale < 0) {
            // 值为整数：unscaled × 10^|scale|
            BigInteger intVal = unscaled.multiply(BigInteger.TEN.pow(-scale));
            Float256 f = of(intVal);
            return neg ? f.negate() : f;
        }

        // scale > 0：value = unscaled / 10^scale = unscaled / (2^scale × 5^scale)
        // 用 q × 2^(-K) 逼近 value，其中 q 约 targetBits 位
        BigInteger pow10 = BigInteger.TEN.pow(scale);
        int targetBits = 240;
        long K = (long) targetBits - unscaled.bitLength() + pow10.bitLength();

        BigInteger num, denom;
        if (K >= 0) {
            num = unscaled.shiftLeft((int) K);
            denom = pow10;
        } else {
            // K < 0：值本身远大于 2^240 位，用右移保证除数不出界
            num = unscaled;
            denom = pow10.shiftLeft((int) -K);
        }

        BigInteger[] qr = num.divideAndRemainder(denom);
        BigInteger q = qr[0];
        boolean exact = qr[1].signum() == 0;

        if (q.signum() == 0) return ZERO;

        // q 太宽时压缩到 ≤ 250 位（可安全装进 Int256）
        while (q.bitLength() > 250) {
            int drop = q.bitLength() - 250;
            BigInteger low = q.and(BigInteger.ONE.shiftLeft(drop).subtract(BigInteger.ONE));
            q = q.shiftRight(drop);
            K -= drop;
            if (low.signum() != 0) exact = false;
        }

        // Sticky 位：把余数信息编码进最低位，让 roundAndPack 的 GRS 正确工作
        BigInteger qSticky = exact ? q.shiftLeft(1) : q.shiftLeft(1).or(BigInteger.ONE);
        Int256 qInt = Int256.of(qSticky);

        // E = BIAS + 175 - K（K_sticky = K+1，见 roundAndPack 约定）
        long offset = 175L - K;
        long offLo = offset & 0xFFFFL;
        long offHi = offset >> 16;
        long eLo = (BIAS_LO + offLo) & 0xFFFFL;
        long eHi = BIAS_HI + offHi + ((BIAS_LO + offLo) >>> 16);

        if (eHi < 0) return ZERO;
        if (eHi > EXP_ALL_HI || (eHi == EXP_ALL_HI && eLo >= EXP_ALL_LO)) {
            return neg ? NEG_INF : POS_INF;
        }

        Float256 result = roundAndPack(eHi, eLo, qInt, 1);
        return neg ? result.negate() : result;
    }

    // ═══════════ 舍入核心 (GRS + RN) ═══════════

    /** 将 mant（任意精度整数）舍入到 177 bit（含隐含位），RN 向偶舍入，打包为 Float256 */
    private static Float256 roundAndPack(long expHi, long expLo, Int256 mant, int sign) {
        if (mant.isZero()) return ZERO;

        int bitLen = mant.bitLength();
        int shift = bitLen - MANT_IMPLIED; // 需要右移的位数

        if (shift > 0) {
            // GRS 提取
            long G = mant.testBit(shift - 1) ? 1L : 0L;
            long R = shift >= 2 && mant.testBit(shift - 2) ? 1L : 0L;
            long S = (shift >= 2 && !mant.and(mant.maskBelow(shift - 2)).isZero()) ? 1L : 0L;
            mant = mant.shiftRight(shift);
            // 归一化：指数 + shift（79-bit）
            expLo += shift;
            expHi += expLo >>> 16;
            expLo &= 0xFFFF;
            // RN 向偶舍入
            boolean increment = G == 1 && (R == 1 || S == 1 || mant.lowBit() == 1);
            if (increment) {
                mant = mant.add(Int256.ONE);
                if (mant.bitLength() > MANT_IMPLIED) {
                    mant = mant.shiftRight(1);
                    // exp++
                    expLo++;
                    if (expLo > 0xFFFF) {
                        expLo = 0;
                        expHi++;
                    }
                }
            }
        } else if (shift < 0) {
            // 理论上不会发生（调用方保证归一化），防御性左移
            mant = mant.shiftLeft(-shift);
            expLo += shift; // shift 为负
            if (expLo < 0) {
                expHi--;
                expLo += 0x10000;
            }
            expLo &= 0xFFFF;
        }

        // 去掉隐含位：只保留低 176 bit
        mant = mant.and(mant.maskBelow(MANT_BITS));

        // 指数上溢 → Inf（exp 编码 ≥ 2^79-1）
        if (expHi > EXP_ALL_HI || (expHi == EXP_ALL_HI && expLo >= EXP_ALL_LO)) {
            return sign < 0 ? NEG_INF : POS_INF;
        }
        // exp 编码是 79-bit 无符号，任意值（含 exp < BIAS 的小数指数）都合法，无需次正规处理

        return make(expHi, expLo, mant.b & MANT_HI_MASK, mant.c, mant.d, sign);
    }

    // ═══════════ 加减法 ═══════════

    public Float256 add(Float256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (isZero()) return o;
        if (o.isZero()) return this;
        if (isInfinity() || o.isInfinity()) {
            if (isInfinity() && o.isInfinity() && signum() != o.signum()) return NaN;
            return isInfinity() ? this : o;
        }

        long expHi1 = expHi(), expLo1 = expLo();
        long expHi2 = o.expHi(), expLo2 = o.expLo();
        int sign1 = signum();
        int sign2 = o.signum();

        Int256 M1 = mantissaWithImplied();
        Int256 M2 = o.mantissaWithImplied();

        // 对齐指数
        int cmp = expCmp(expHi1, expLo1, expHi2, expLo2);
        if (cmp > 0) {
            int diff = expDiff(expHi1, expLo1, expHi2, expLo2);
            M2 = diff > 300 ? Int256.ZERO : M2.shiftRight(diff);
        } else if (cmp < 0) {
            int diff = expDiff(expHi2, expLo2, expHi1, expLo1);
            M1 = diff > 300 ? Int256.ZERO : M1.shiftRight(diff);
            expHi1 = expHi2;
            expLo1 = expLo2;
        }

        Int256 resultMant;
        int resultSign;
        if (sign1 == sign2) {
            resultMant = M1.add(M2);
            resultSign = sign1;
        } else {
            int mc = M1.compareTo(M2);
            if (mc >= 0) {
                resultMant = M1.subtract(M2);
                resultSign = sign1;
            } else {
                resultMant = M2.subtract(M1);
                resultSign = sign2;
            }
        }

        return roundAndPack(expHi1, expLo1, resultMant, resultSign);
    }

    public Float256 subtract(Float256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        return add(o.negate());
    }

    /** |exp1 - exp2|，截断为 int（>300 时调用方按 0 处理） */
    // 🔧 修复：hi 跨 2^16 边界进位（如 BIAS=0x3FFF:FFFF → BIAS+1=0x4000:0000）时 hi 不同但差很小，
    // 不能直接 return 301，必须算完整 79-bit 差
    private static int expDiff(long hi1, long lo1, long hi2, long lo2) {
        long dLo = lo1 - lo2;
        long borrow = (dLo < 0) ? 1L : 0L;
        long dHi = hi1 - hi2 - borrow; // 无符号 63-bit 差
        if (dHi != 0) return 301; // ≥ 2^16 > 300
        long diff = dLo & 0xFFFF;
        return diff > 300 ? 301 : (int) diff;
    }

    /** 尾数 + 隐含位（Int256，隐含位在 bit176） */
    private Int256 mantissaWithImplied() {
        return Int256.of(0L, mantHi(), mantMid(), mantLo())
                .or(Int256.of(0L, 1L << 48, 0L, 0L));
    }

    // ═════════ 乘法 ═════════
    public Float256 multiply(Float256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (isZero() || o.isZero()) return ZERO;
        if (isInfinity() || o.isInfinity()) {
            if (isZero() || o.isZero()) return NaN;
            return (signum() == o.signum()) ? POS_INF : NEG_INF;
        }

        // E_in = E1 + E2 - BIAS（80-bit 无符号运算，roundAndPack 自动补偿归一化 shift）
        long tLo = expLo() + o.expLo();
        long tHi = expHi() + o.expHi() + (tLo >>> 16); // 64-bit 无符号（可含 79-bit 溢出位）
        tLo &= 0xFFFF;
        // T < BIAS → 下溢（极小值归零）
        if (Long.compareUnsigned(tHi, BIAS_HI) < 0) return ZERO;
        if (tHi == BIAS_HI && Long.compareUnsigned(tLo, BIAS_LO) < 0) return ZERO;
        long eLo = tLo - BIAS_LO;
        long borrow = (eLo < 0) ? 1L : 0L;
        eLo &= 0xFFFF;
        long eHi = tHi - BIAS_HI - borrow; // 无符号（≥0）
        if (eHi < 0) { // 无符号 ≥ 2^63 → E ≥ 2^79 → 上溢
            return (signum() != o.signum()) ? NEG_INF : POS_INF;
        }

        int resultSign = signum() * o.signum();

        // 🔧 修复：177×177-bit 乘积需要 354 bit，超 Int256 256-bit 容量。
        // 用 long[6]（384 bit）小端累加器 + 专用 roundAndPackMul（GRS 舍入）
        Int256 M1 = mantissaWithImplied();
        Int256 M2 = o.mantissaWithImplied();
        long[] x = {M1.d, M1.c, M1.b}; // 小端 3 limb（隐含位在 M1.b bit48）
        long[] y = {M2.d, M2.c, M2.b};
        long[] r = new long[6];
        for (int i = 0; i < 3; i++) {
            long xi = x[i];
            if (xi == 0L) continue;
            for (int j = 0; j < 3; j++) {
                long yj = y[j];
                if (yj == 0L) continue;
                addTo6(r, i + j, xi * yj, Math.unsignedMultiplyHigh(xi, yj));
            }
        }
        return roundAndPackMul(r, eHi, eLo, resultSign);
    }

    // ═══════════ 384-bit 乘法累加（零 GC） ═══════════

    /** 384-bit 累加器：r[idx..idx+1] += (hi << 64 | lo)，进位向高位传播 */
    private static void addTo6(long[] r, int idx, long lo, long hi) {
        long carry = addToLimb6(r, idx, lo);
        long h = hi + carry;
        long extra = (Long.compareUnsigned(h, hi) < 0) ? 1L : 0L; // hi+carry 溢出 → 额外进位
        long total = addToLimb6(r, idx + 1, h) + extra;
        for (int k = idx + 2; total != 0 && k < r.length; k++) {
            total = addToLimb6(r, k, total);
        }
    }

    private static long addToLimb6(long[] r, int k, long add) {
        long old = r[k];
        long s = old + add;
        r[k] = s;
        return (Long.compareUnsigned(s, old) < 0) ? 1L : 0L;
    }

    /** 取 384-bit 累加器的第 bit 位（bit < 0 → 0） */
    private static long bitAt(long[] r, int bit) {
        if (bit < 0) return 0L;
        return (r[bit >> 6] >>> (bit & 63)) & 1L;
    }

    /** 检查 r 的 bit < limit 是否存在非零位 */
    private static boolean hasBitsBelow(long[] r, int limit) {
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

    /** 384-bit 右移 shift 位，取低 3 limb（小端） */
    private static long[] shr3(long[] r, int shift) {
        long[] out = new long[3];
        int w = shift >> 6, b = shift & 63;
        for (int i = 0; i < 3; i++) {
            int hi = i + w;
            long high = hi < r.length ? r[hi] : 0L;
            long low = (hi + 1 < r.length) ? r[hi + 1] : 0L;
            out[i] = (b == 0) ? high : (high >>> b) | (low << (64 - b));
        }
        return out;
    }

    /**
     * 乘积舍入打包：r（小端 384-bit）→ Float256，RN 向偶。 🔧 exp' = E_in + shift - MANT_BITS（乘积是 352/353 bit
     * 隐含位基数，需减 176 补偿）
     */
    private static Float256 roundAndPackMul(long[] r, long expHi, long expLo, int sign) {
        int bitLen = 0;
        for (int i = 5; i >= 0; i--) {
            if (r[i] != 0L) {
                bitLen = (i << 6) + (64 - Long.numberOfLeadingZeros(r[i]));
                break;
            }
        }
        if (bitLen == 0) return ZERO;
        int shift = bitLen - MANT_IMPLIED; // 归一化：通常 176 或 177
        // GRS
        long G = bitAt(r, shift - 1);
        long R = (shift >= 2) ? bitAt(r, shift - 2) : 0L;
        long S = (shift >= 2 && hasBitsBelow(r, shift - 2)) ? 1L : 0L;
        // 右移 shift 位 → 177-bit（小端 3 limb，隐含位在 limb2 bit48）
        long[] m = shr3(r, shift);
        // exp 补偿：E_in + shift - MANT_BITS
        expLo += shift - MANT_BITS;
        expHi += expLo >>> 16;
        expLo &= 0xFFFF;
        // RN 向偶
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
            }
            // 舍入进位使 mantissa 变 2^177 → 右移 1 位，指数 +1
            if ((m[2] & 0x0002_0000_0000_0000L) != 0L) { // bit177 set
                m[2] = (m[2] >>> 1) | (m[1] << 63);
                m[1] = (m[1] >>> 1) | (m[0] << 63);
                m[0] >>>= 1;
                expLo++;
                if (expLo > 0xFFFF) {
                    expLo = 0;
                    expHi++;
                }
            }
        }
        // exp 上溢 → Inf
        if (expHi > EXP_ALL_HI || (expHi == EXP_ALL_HI && expLo >= EXP_ALL_LO)) {
            return sign < 0 ? NEG_INF : POS_INF;
        }
        return make(expHi, expLo, m[2] & MANT_HI_MASK, m[1], m[0], sign);
    }

    // ═════════ 除法 ═════════
    public Float256 divide(Float256 o) {
        if (isNaN() || o.isNaN()) return NaN;
        if (o.isZero()) return signum() == o.signum() ? POS_INF : NEG_INF;
        if (isZero()) return ZERO;
        if (isInfinity()) {
            if (o.isInfinity()) return NaN;
            return signum() == o.signum() ? POS_INF : NEG_INF;
        }

        // E_in = E1 - E2 + BIAS - 1（分 E1≥E2 / E1<E2 两路，防 64-bit 溢出误判）
        int resultSign = signum() * o.signum();
        long eHi, eLo;
        if (expCmp(expHi(), expLo(), o.expHi(), o.expLo()) >= 0) {
            // E1 >= E2：E = (E1-E2) + BIAS - 1，只可能上溢
            long dLo = expLo() - o.expLo();
            long borrow = (dLo < 0) ? 1L : 0L;
            dLo &= 0xFFFF;
            long dHi = expHi() - o.expHi() - borrow; // 无符号差
            eLo = dLo + BIAS_LO - 1;
            long carry = eLo >>> 16;
            eLo &= 0xFFFF;
            eHi = dHi + BIAS_HI + carry;
            if (Long.compareUnsigned(eHi, EXP_ALL_HI) > 0) { // E ≥ 2^79 → 上溢
                return resultSign < 0 ? NEG_INF : POS_INF;
            }
        } else {
            // E1 < E2：E = BIAS - 1 - (E2-E1)，只可能下溢
            long dLo = o.expLo() - expLo();
            long borrow = (dLo < 0) ? 1L : 0L;
            dLo &= 0xFFFF;
            long dHi = o.expHi() - expHi() - borrow; // 无符号差
            // 下溢: E2-E1 > BIAS-1 = 2^78-2
            if (Long.compareUnsigned(dHi, BIAS_HI) > 0) return ZERO;
            if (dHi == BIAS_HI && Long.compareUnsigned(dLo, BIAS_LO - 1) > 0) return ZERO;
            eLo = BIAS_LO - 1 - dLo;
            long borrow2 = (eLo < 0) ? 1L : 0L;
            eLo &= 0xFFFF;
            eHi = BIAS_HI - dHi - borrow2;
        }

        Int256 M1 = mantissaWithImplied();
        Int256 M2 = o.mantissaWithImplied();
        // 🔧 修复：商 = (M1 << 177) / M2 需 354-bit 中间值，Int256 只有 256-bit 会截断。
        // 用 BigInteger 精确除法，商 ≤ 178 bit 可装回 Int256
        BigInteger num = M1.toBigInteger().shiftLeft(MANT_IMPLIED);
        BigInteger den = M2.toBigInteger();
        Int256 quot = Int256.of(num.divide(den));

        return roundAndPack(eHi, eLo, quot, resultSign);
    }

    // ═════════ 取负 / 绝对值 ═════════
    public Float256 negate() {
        if (isZero()) return ZERO;
        if (isNaN()) return NaN;
        return new Float256(a ^ SIGN_MASK, b, c, d);
    }

    public Float256 abs() {
        if (isZero()) return ZERO;
        if (isNaN()) return NaN;
        return new Float256(a & ~SIGN_MASK, b, c, d);
    }

    /** 直接位转换：尾数 176→192 bit（左移 16 扩充）+ 指数 re-bias（-2^78+2^63） */
    public UFloat256 toUFloat256() {
        if (isZero()) return UFloat256.ZERO;
        if (isNaN()) return UFloat256.NaN;
        if (isInfinity()) return signum() < 0 ? UFloat256.NaN : UFloat256.INF;
        if (signum() < 0) throw new IllegalArgumentException("UFloat256 cannot be negative");
        // 指数 re-bias: e_uf = e_f - (2^78 - 2^63)，2^78-2^63 = 0x3FFF_8000_0000_0000_0000
        long hi = expHi() - 0x3FFF_8000_0000_0000L; // 常数高 63 位
        if (hi < 0) return UFloat256.ZERO; // 指数过小 → 0
        if (hi > 0xFFFF_FFFF_FFFFL) return UFloat256.INF; // e_uf ≥ 2^64
        long euf = (hi << 16) | expLo(); // 64-bit 无符号
        // 尾数 176 → 192：左移 16 bit 零损失扩充
        long mHi = (mantHi() << 16) | (mantMid() >>> 48);
        long mMid = (mantMid() << 16) | (mantLo() >>> 48);
        long mLo = mantLo() << 16;
        return UFloat256.make(euf, mHi, mMid, mLo);
    }

    // ═════════ 平方根 ═════════
    /** 平方根（高精度）。 修复：原实现 of(Math.sqrt(doubleValue())) 精度只有 53 bit。 */
    public Float256 sqrt() {
        if (isNaN()) return NaN;
        if (isZero()) return ZERO;
        if (signum() < 0) return NaN;
        if (isInfinity()) return POS_INF;

        // value = M × 2^e，M 是 177 位（隐含位在 bit176），e = realExponent - 176
        long e = realExponent() - MANT_BITS;
        Int256 M = mantissaWithImplied();

        // 保证 e 为偶数
        if ((e & 1L) != 0) {
            M = M.shiftLeft(1);
            e -= 1;
        }

        // 为提高精度，M 左移 2k 位，sqrt(M × 2^2k) = sqrt(M) × 2^k
        final int k = 90;
        BigInteger M2 = M.toBigInteger().shiftLeft(2 * k);
        BigInteger s = M2.sqrt(); // ≈ sqrt(M) × 2^k，约 178 位

        // 结果 = s × 2^(e/2 - k)
        // 用尾数 s 和指数 E 调 roundAndPack，使 s × 2^(E - BIAS - 176) = s × 2^(e/2 - k)
        // 即 E = BIAS + 176 + e/2 - k
        long offset = 176L + e / 2 - k;
        long offLo = offset & 0xFFFFL;
        long offHi = offset >> 16;
        long eLo = (BIAS_LO + offLo) & 0xFFFFL;
        long eHi = BIAS_HI + offHi + ((BIAS_LO + offLo) >>> 16);

        if (eHi < 0) return ZERO;
        if (eHi > EXP_ALL_HI || (eHi == EXP_ALL_HI && eLo >= EXP_ALL_LO)) return POS_INF;

        return roundAndPack(eHi, eLo, Int256.of(s), 1);
    }

    // ═════════ 比较 ═════════
    @Override
    public int compareTo(Float256 o) {
        if (isNaN() || o.isNaN()) return 0;
        if (isZero() && o.isZero()) return 0;
        if (isInfinity() && o.isInfinity())
            return signum() < 0 ? (o.signum() < 0 ? 0 : -1) : (o.signum() < 0 ? 1 : 0);
        if (isInfinity()) return signum() < 0 ? -1 : 1;
        if (o.isInfinity()) return o.signum() < 0 ? 1 : -1;
        if (signum() != o.signum()) return signum() < 0 ? -1 : 1;
        boolean isNeg = signum() < 0;
        int cmp = expCmp(expHi(), expLo(), o.expHi(), o.expLo());
        if (cmp != 0) return isNeg ? -cmp : cmp;
        cmp = Long.compareUnsigned(mantHi(), o.mantHi());
        if (cmp != 0) return isNeg ? -cmp : cmp;
        cmp = Long.compareUnsigned(mantMid(), o.mantMid());
        if (cmp != 0) return isNeg ? -cmp : cmp;
        cmp = Long.compareUnsigned(mantLo(), o.mantLo());
        return isNeg ? -cmp : cmp;
    }

    // ═════════ 转换 ═════════
    @Override
    public double doubleValue() {
        if (isZero()) return 0.0;
        if (isNaN()) return Double.NaN;
        if (isInfinity()) return signum() < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        return toBigDecimal().doubleValue();
    }

    @Override
    public long longValue() {
        if (isZero()) return 0;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        long realExp = realExponent();
        // long 有效范围 [-2^63, 2^63-1]，指数 ≥ 63 就一定超
        if (realExp >= 63) throw new ArithmeticException("Float256 out of long range");
        if (realExp < 0) return 0;
        Int256 shifted = mantissaWithImplied().shiftRight((int) (MANT_BITS - realExp));
        long result = shifted.longValue();
        return signum() < 0 ? -result : result;
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

/** 精确十进制转换（不做有效位数截断）。 修复：原实现 realExp<0 时用 MathContext.DECIMAL128，每次转换丢约 19 位有效数字。 */
    public BigDecimal toBigDecimal() {
        if (isZero()) return BigDecimal.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        BigInteger mant = mantissaWithImplied().toBigInteger();
        long realExp = realExponent() - MANT_BITS; // 值 = mant × 2^realExp

        BigDecimal dec;
        if (realExp == 0) {
            dec = new BigDecimal(mant, 0);
        } else if (realExp > 0) {
            // 值 = mant × 2^realExp，直接左移构造精确分子
            if (realExp > Integer.MAX_VALUE) {
                throw new ArithmeticException("Float256 exponent too large for BigDecimal");
            }
            BigInteger scaled = mant.shiftLeft((int) realExp);
            dec = new BigDecimal(scaled, 0);
        } else {
            // realExp < 0：值 = mant / 2^|realExp|，用 BigInteger 移位构造分母，不截断
            long negExp = -realExp;
            if (negExp > Integer.MAX_VALUE) {
                throw new ArithmeticException("Float256 exponent too small for BigDecimal");
            }
            BigInteger denom = BigInteger.ONE.shiftLeft((int) negExp);
            dec = new BigDecimal(mant).divide(new BigDecimal(denom), MathContext.UNLIMITED);
        }
        return signum() < 0 ? dec.negate() : dec;
    }

    // ═══════════ 精确取整（返回 Int256，零损失） ═══════════

    /** 向零截断 */
    public Int256 truncate() {
        if (isZero()) return Int256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        Int256 v = truncateAbs();
        return signum() < 0 ? v.negate() : v;
    }

    /** 向下取整（-∞ 方向） */
    // 🔧 修复：移位基数 MANT_BITS=176；realExp<0 时 |值|<1 直接返回 0/-1
    public Int256 floor() {
        if (isZero()) return Int256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        long realExp = realExponent();
        if (realExp >= MANT_BITS) return truncate();
        if (realExp >= 0) {
            Int256 mant = mantissaWithImplied();
            Int256 abs = mant.shiftRight((int) (MANT_BITS - realExp));
            boolean dropped = !mant.and(mant.maskBelow((int) (MANT_BITS - realExp))).isZero();
            if (dropped && signum() < 0) abs = abs.add(Int256.ONE);
            return signum() < 0 ? abs.negate() : abs;
        }
        return signum() < 0 ? Int256.MINUS_ONE : Int256.ZERO;
    }

    /** 向上取整（+∞ 方向） */
    // 🔧 修复：同 floor；realExp<0 时 ceil 正=1，负=0
    public Int256 ceil() {
        if (isZero()) return Int256.ZERO;
        if (isNaN() || isInfinity()) throw new ArithmeticException("not finite");
        long realExp = realExponent();
        if (realExp >= MANT_BITS) return truncate();
        if (realExp >= 0) {
            Int256 mant = mantissaWithImplied();
            Int256 abs = mant.shiftRight((int) (MANT_BITS - realExp));
            boolean dropped = !mant.and(mant.maskBelow((int) (MANT_BITS - realExp))).isZero();
            if (dropped && signum() > 0) abs = abs.add(Int256.ONE);
            return signum() < 0 ? abs.negate() : abs;
        }
        return signum() > 0 ? Int256.ONE : Int256.ZERO;
    }

    /** 四舍五入（half-up，与 Math.round 一致） */
    public Int256 round() {
        return this.add(Float256.of(0.5)).floor();
    }

    /** 绝对值截断（内部用） */
    // 🔧 修复：值 = mant × 2^(realExp - 176)。realExp ≥ 176 左移(realExp-176)，0≤realExp<176
    // 右移(176-realExp)，<0 归 0
    private Int256 truncateAbs() {
        long realExp = realExponent();
        Int256 mant = mantissaWithImplied();
        if (realExp >= MANT_BITS) {
            if (realExp >= 255) throw new ArithmeticException("Float256 too large for Int256");
            return mant.shiftLeft((int) (realExp - MANT_BITS));
        }
        if (realExp < 0) return Int256.ZERO;
        return mant.shiftRight((int) (MANT_BITS - realExp));
    }

    /**
     * 精确十进制展开：值 = mant × 2^e → mant × 5^(-e) / 10^(-e) 完整保留 53-bit double 尾数（0.1 →
     * 0.1000000000000000055511151231257827021181583404541015625） 用于调试屏幕坐标显示，尽量减少精度损失
     */
    public String toExactString() {
        if (isZero()) return "0";
        if (isNaN()) return "NaN";
        if (isInfinity()) return signum() < 0 ? "-Infinity" : "Infinity";
        BigInteger m = mantissaWithImplied().toBigInteger();
        long e = realExponent() - MANT_BITS; // 值 = m × 2^e
        StringBuilder sb = new StringBuilder(32);
        if (signum() < 0) sb.append('-');
        if (e >= 0) {
            if (e > 1L << 30) return toBigDecimal().toString(); // 超大指数走近似
            sb.append(m.shiftLeft((int) e));
            return sb.toString();
        }
        long negE = -e;
        if (negE > 1L << 30) return toBigDecimal().toString(); // 超小指数走近似
        // m × 5^k / 10^k
        BigInteger scaled = m.multiply(BigInteger.valueOf(5).pow((int) negE));
        String s = scaled.toString();
        int shift = (int) negE;
        if (s.length() <= shift) {
            sb.append("0.");
            for (int i = 0; i < shift - s.length(); i++) sb.append('0');
            sb.append(s);
        } else {
            sb.append(s, 0, s.length() - shift).append('.').append(s, s.length() - shift, s.length());
        }
        // 去掉小数尾随 0
        int len = sb.length();
        while (len > 0 && sb.charAt(len - 1) == '0') len--;
        if (len > 0 && sb.charAt(len - 1) == '.') len--;
        sb.setLength(len);
        return sb.toString();
    }

    @Override
    public String toString() {
        if (isNaN()) return "NaN";
        if (isZero()) return "0";
        if (isInfinity()) return signum() < 0 ? "-Infinity" : "Infinity";
        return toBigDecimal().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Float256 other)) return false;
        if (isNaN() || other.isNaN()) return false;
        if (isZero() && other.isZero()) return true;
        return a == other.a && b == other.b && c == other.c && d == other.d;
    }

    @Override
    public int hashCode() {
        if (hash == HASH_NOT_CACHED) hash = (int) (a ^ b ^ c ^ d);
        return hash;
    }

    // ══════════════════════ 测试 ══════════════════════
    public static void main(String[] args) {
        System.out.println("=== Float256 测试 (含舍入) ===");
        System.out.println("1   = " + ONE);
        System.out.println("2   = " + TWO);
        System.out.println("1+2 = " + ONE.add(TWO));
        System.out.println("1/3 = " + ONE.divide(of(3)));
        System.out.println("1/3*3 = " + ONE.divide(of(3)).multiply(of(3)));
        System.out.println("2^100 = " + of(Int256.ONE.shiftLeft(100)));
        System.out.println("0.1+0.2 = " + of(0.1).add(of(0.2)));
        System.out.println("0.1 exact = " + of(0.1).toExactString());
        System.out.println("1.5 exact = " + of(1.5).toExactString());
        System.out.println("12550821.123456789 exact = " + of(12550821.123456789).toExactString());
        System.out.println("sqrt(2) = " + of(2).sqrt());
        System.out.println("Long.MAX = " + of(Long.MAX_VALUE));
        System.out.println("Long.MAX+1 = " + of(Long.MAX_VALUE).add(ONE));
        System.out.println("(-3).abs() = " + of(-3).abs());
        System.out.println("NaN = " + NaN);
        System.out.println("1/0 = " + ONE.divide(ZERO));
        System.out.println("0/0 = " + ZERO.divide(ZERO));
    }
}
