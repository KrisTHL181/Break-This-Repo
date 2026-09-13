package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;

import java.util.Arrays;
import java.util.Objects;

/**
 * Int256 — 有符号 256-bit 整数（补码）
 *
 * <p>内部: long[4] = {a, b, c, d} = {bits 255..192, 191..128, 127..64, 63..0}，最高位 = 符号 范围: [-2^255,
 * 2^255 - 1] 零 GC，全 long 运算，无 BigInteger 依赖（除 toString）
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class Int256 extends Number implements Comparable<Int256> {

    // ──────── 内部存储 (immutable) ────────
    final long a; // bits 255..192
    final long b; // bits 191..128
    final long c; // bits 127..64
    final long d; // bits 63..0

    // ──────── 缓存 ────────
    private transient Int256 cachedNegate;
    private transient BigInteger cachedBigInteger;
    private transient BigDecimal cachedBigDecimal;
    private transient int hash;
    private static final int HASH_NOT_CACHED = Integer.MIN_VALUE;

    // ──────── 常量 ────────
    public static final Int256 ZERO = new Int256(0L, 0L, 0L, 0L);
    public static final Int256 ONE = new Int256(0L, 0L, 0L, 1L);
    public static final Int256 TWO = new Int256(0L, 0L, 0L, 2L);
    public static final Int256 THREE = new Int256(0L, 0L, 0L, 3L);
    public static final Int256 FOUR = new Int256(0L, 0L, 0L, 4L);
    public static final Int256 FIVE = new Int256(0L, 0L, 0L, 5L);
    public static final Int256 TEN = new Int256(0L, 0L, 0L, 10L);
    public static final Int256 MINUS_ONE = new Int256(-1L, -1L, -1L, -1L);
    public static final Int256 NEG_ONE = MINUS_ONE;

    /** 最大值 2^255 - 1 */
    public static final Int256 MAX_VALUE = new Int256(0x7FFF_FFFF_FFFF_FFFFL, -1L, -1L, -1L);

    /** 最小值 -2^255 */
    public static final Int256 MIN_VALUE = new Int256(0x8000_0000_0000_0000L, 0L, 0L, 0L);

    // ──────── 小值缓存 (-128..127) ────────
    private static final Int256[] SMALL = new Int256[256];
    private static final int SMALL_OFF = 128;

    static {
        for (int i = -128; i <= 127; i++) {
            long fill = (i < 0) ? -1L : 0L;
            SMALL[i + SMALL_OFF] = new Int256(fill, fill, fill, (long) i);
        }
    }

    // ──────── 构造（私有，不可变）────────
    private Int256(long a, long b, long c, long d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.hash = HASH_NOT_CACHED;
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
        // 通过 BigDecimal 转换保持精度
        return toBigDecimal().doubleValue();
    }

    /** 转 BigDecimal（内部用） */
    private BigDecimal toBigDecimal() {
        if (cachedBigDecimal != null) return cachedBigDecimal;
        return cachedBigDecimal = new BigDecimal(toBigInteger());
    }

    // ──────── 工厂 ────────

    /** 从 long（符号扩展） */
    public static Int256 of(long value) {
        if (value >= -128 && value <= 127) return SMALL[(int) value + SMALL_OFF];
        long fill = (value < 0) ? -1L : 0L;
        return new Int256(fill, fill, fill, value);
    }

    /** 从 4 个无符号 long（[0] 最高） */
    public static Int256 of(long w0, long w1, long w2, long w3) {
        return new Int256(w0, w1, w2, w3);
    }

    /** 大端 32 字节（补码） */
    public static Int256 of(byte[] bytes) {
        if (bytes.length != 32) throw new IllegalArgumentException("need 32 bytes");
        return new Int256(aggregate(bytes, 0), aggregate(bytes, 8),
        aggregate(bytes, 16), aggregate(bytes, 24));
    }

    /**
     * 从 BigInteger（取低 256 位，符号扩展）。 修复：原实现对 bit255=1 的正数会错误地变成负数。Int256 有符号上限是 2^255-1，
     * 超过必须抛异常而不是静默回绕。
     */
    public static Int256 of(BigInteger value) {
        if (value.bitLength() <= 63) {
            return of(value.longValue());
        }
        // 有符号上限 2^255 - 1
        if (value.signum() > 0 && value.bitLength() > 255) {
            throw new ArithmeticException("Int256 overflow: " + value.bitLength() + " bits > 255");
        }
        if (value.signum() < 0 && value.bitLength() > 256) {
            // 负下限 -2^255，bitLength() 对负数返回 ~x 的位数，等于 256 时是 -2^255 恰好合法
            throw new ArithmeticException("Int256 underflow: " + value.bitLength() + " bits");
        }

        byte[] mag = value.toByteArray();
        byte[] buf = new byte[32];
        if (mag.length >= 32) {
            System.arraycopy(mag, mag.length - 32, buf, 0, 32);
        } else {
            byte fill = value.signum() < 0 ? (byte) 0xFF : 0x00;
            Arrays.fill(buf, fill);
            System.arraycopy(mag, 0, buf, 32 - mag.length, mag.length);
        }
        return of(buf);
    }

    private static long aggregate(byte[] b, int off) {
        return ((long) (b[off] & 0xFF) << 56) | ((long) (b[off + 1] & 0xFF) << 48)
                | ((long) (b[off + 2] & 0xFF) << 40) | ((long) (b[off + 3] & 0xFF) << 32)
                | ((long) (b[off + 4] & 0xFF) << 24) | ((long) (b[off + 5] & 0xFF) << 16)
                | ((long) (b[off + 6] & 0xFF) << 8) | (long) (b[off + 7] & 0xFF);
    }

    // ──────── 核心运算 ────────

    /* ==================== 加法 ==================== */
    // 🔧 修复：进位方向 d→c→b→a（从最低 limb 向最高传播）
    public Int256 add(Int256 o) {
        long s3 = d + o.d;
        long c3 = (unsignedLessThan(s3, d) ? 1L : 0L);
        long s2 = c + o.c + c3;
        long c2 = (c3 != 0 && unsignedLessOrEqual(s2, c)) || (c3 == 0 && unsignedLessThan(s2, c)) ? 1L : 0L;
        long s1 = b + o.b + c2;
        long c1 = (c2 != 0 && unsignedLessOrEqual(s1, b)) || (c2 == 0 && unsignedLessThan(s1, b)) ? 1L : 0L;
        long s0 = a + o.a + c1;
        return new Int256(s0, s1, s2, s3);
    }

    /* ==================== 减法 ==================== */
    // 🔧 修复：借位方向 d→c→b→a（从最低 limb 向最高传播）
    public Int256 subtract(Int256 o) {
        long s3 = d - o.d;
        long b3 = unsignedGreaterThan(s3, d) ? 1L : 0L;
        long s2 = c - o.c - b3;
        long b2 = (b3 != 0 && unsignedGreaterOrEqual(s2, c)) || (b3 == 0 && unsignedGreaterThan(s2, c)) ? 1L : 0L;
        long s1 = b - o.b - b2;
        long b1 = (b2 != 0 && unsignedGreaterOrEqual(s1, b)) || (b2 == 0 && unsignedGreaterThan(s1, b)) ? 1L : 0L;
        long s0 = a - o.a - b1;
        return new Int256(s0, s1, s2, s3);
    }

    /* ==================== 乘法 ==================== */
    public Int256 multiply(Int256 o) {
        // 4×4 无符号 limb 乘法 → 512-bit 中间结果（小端 r[0..7]），取低 256 bit。
        // 补码乘法的低 256 bit 与无符号乘法一致，天然正确。
        long[] x = {d, c, b, a}; // 小端
        long[] y = {o.d, o.c, o.b, o.a};
        long[] r = new long[8];
        for (int i = 0; i < 4; i++) {
            long xi = x[i];
            if (xi == 0) continue;
            for (int j = 0; j < 4; j++) {
                long yj = y[j];
                if (yj == 0) continue;
                long lo = xi * yj;
                long hi = Math.unsignedMultiplyHigh(xi, yj);
                addTo(r, i + j, lo, hi);
            }
        }
        // 🔧 修复：补码乘法取低 256 bit（512-bit 累加器的 r[3..0]），不是高 256
        return new Int256(r[3], r[2], r[1], r[0]);
    }

    /** 512-bit 累加器：r[idx..idx+1] += (hi << 64 | lo)，进位向高位传播 */
    // 🔧 修复：低位进位必须加到 r[idx+1]，不能折叠传播到 r[idx+2]；hi+carry 溢出单独处理
    private static void addTo(long[] r, int idx, long lo, long hi) {
        long carry = addToLimb(r, idx, lo);
        long h = hi + carry;
        long extra = (Long.compareUnsigned(h, hi) < 0) ? 1L : 0L; // hi+carry 溢出 → 额外进位
        long total = addToLimb(r, idx + 1, h) + extra;
        for (int k = idx + 2; total != 0 && k < r.length; k++) {
            total = addToLimb(r, k, total);
        }
    }

    /** 单 limb 加：r[k] += add，返回进位（0/1） */
    private static long addToLimb(long[] r, int k, long add) {
        long old = r[k];
        long s = old + add;
        r[k] = s;
        return (Long.compareUnsigned(s, old) < 0) ? 1L : 0L;
    }

    /* ==================== 除法 ==================== */
    // 🔧 重写：用 BigInteger 除法避免二分搜索的乘法溢出问题（mid×den ≥ 2^256 时模运算导致比较误判）
    public Int256 divide(Int256 divisor) {
        if (divisor.isZero()) throw new ArithmeticException("/ by zero");
        if (this.isZero()) return ZERO;
        if (divisor.equals(ONE)) return this;
        if (divisor.equals(MINUS_ONE)) return this.negate();
        return Int256.of(this.toBigInteger().divide(divisor.toBigInteger()));
    }

    public Int256 remainder(Int256 divisor) {
        if (divisor.isZero()) throw new ArithmeticException("/ by zero");
        return Int256.of(this.toBigInteger().remainder(divisor.toBigInteger()));
    }

    /* ==================== 取负数 ==================== */
    public Int256 negate() {
        if (cachedNegate != null) return cachedNegate;
        Int256 not = new Int256(~a, ~b, ~c, ~d);
        cachedNegate = not.add(ONE);
        return cachedNegate;
    }

    /* ==================== 绝对值 ==================== */
    public Int256 abs() {
        return isNegative() ? negate() : this;
    }

    /* ==================== 移位 ==================== */
    // 🔧 修复：局部变量 int b → int bits 避免遮蔽字段 b
    public Int256 shiftLeft(int n) {
        if (n == 0) return this;
        if (n < 0) return shiftRight(-n);
        if (n >= 256) return ZERO;

        int w = n / 64;
        int bits = n % 64;
        if (bits == 0) {
            return switch (w) {
                case 0 -> this;
                case 1 -> new Int256(this.b, c, d, 0L);
                case 2 -> new Int256(c, d, 0L, 0L);
                case 3 -> new Int256(d, 0L, 0L, 0L);
                default -> ZERO;
            };
        }
        int r = 64 - bits;
        long[] arr = {0L, 0L, 0L, 0L};
        long[] src = {a, this.b, c, d};
        for (int i = 0; i < 4 - w; i++) {
            arr[i] = src[i + w] << bits;
            if (i + w + 1 < 4) arr[i] |= src[i + w + 1] >>> r;
        }
        return new Int256(arr[0], arr[1], arr[2], arr[3]);
    }

    // 🔧 修复：bits 改名 + 方向 src[i+w] + 越界防护
    public Int256 shiftRight(int n) {
        if (n == 0) return this;
        if (n < 0) return shiftLeft(-n);
        if (n >= 256) return isNegative() ? MINUS_ONE : ZERO;

        long fill = isNegative() ? -1L : 0L;
        int w = n / 64;
        int bits = n % 64;
        if (bits == 0) {
            return switch (w) {
                case 0 -> this;
                case 1 -> new Int256(fill, a, this.b, c);
                case 2 -> new Int256(fill, fill, a, this.b);
                case 3 -> new Int256(fill, fill, fill, a);
                default -> new Int256(fill, fill, fill, fill);
            };
        }
        int l = 64 - bits;
        long[] src = {a, this.b, c, d};
        long[] arr = new long[4];
        for (int i = 0; i < 4; i++) {
            // 🔧 大端 src[0]=最高。右移 w 个整 limb + bits 位：
            //   主部分 = src[i-w]，移入部分 = src[i-w-1]（最高低位进位）
            int hiIdx = i - w;
            int loIdx = hiIdx - 1;
            long high = hiIdx >= 0 ? src[hiIdx] : fill;
            long low = loIdx >= 0 ? src[loIdx] : fill;
            // 🔧 高 limb 用无符号 >>>（b/c/d 是无符号位模式），a（符号位）用 >> 算术
            if (hiIdx == 0) {
                arr[i] = (high >> bits) | (low << l);
            } else {
                arr[i] = (high >>> bits) | (low << l);
            }
        }
        return new Int256(arr[0], arr[1], arr[2], arr[3]);
    }

    /* ==================== 位运算 ==================== */
    public Int256 and(Int256 o) {
        return new Int256(a & o.a, b & o.b, c & o.c, d & o.d);
    }

    public Int256 or(Int256 o) {
        return new Int256(a | o.a, b | o.b, c | o.c, d | o.d);
    }

    public Int256 xor(Int256 o) {
        return new Int256(a ^ o.a, b ^ o.b, c ^ o.c, d ^ o.d);
    }

    public Int256 not() {
        return new Int256(~a, ~b, ~c, ~d);
    }

    /** 低 mask 位全 1 的掩码（mask ≤ 256），超出部分为 0 */
    public Int256 maskBelow(int mask) {
        if (mask <= 0) return ZERO;
        if (mask >= 256) return new Int256(-1L, -1L, -1L, -1L);
        int w = mask / 64;
        int r = mask % 64;
        long m0 = 0L, m1 = 0L, m2 = 0L, m3 = 0L;
        if (w >= 1) m3 = -1L;
        if (w >= 2) m2 = -1L;
        if (w >= 3) m1 = -1L;
        if (r > 0) {
            long low = (1L << r) - 1;
            switch (w) {
                case 0 -> m3 = low;
                case 1 -> m2 = low;
                case 2 -> m1 = low;
                case 3 -> m0 = low;
            }
        }
        return new Int256(m0, m1, m2, m3);
    }

    /** 第 idx 个 64-bit limb（0 = 最高 a，3 = 最低 d） */
    public long component(int idx) {
        return switch (idx) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            case 3 -> d;
            default -> throw new IndexOutOfBoundsException("limb index: " + idx);
        };
    }

    /* ==================== 比较 ==================== */
    @Override
    public int compareTo(Int256 o) {
        if (a != o.a) return a < o.a ? -1 : 1; // 符号位在最高 limb
        if (b != o.b) return Long.compareUnsigned(b, o.b) < 0 ? -1 : 1;
        if (c != o.c) return Long.compareUnsigned(c, o.c) < 0 ? -1 : 1;
        if (d != o.d) return Long.compareUnsigned(d, o.d) < 0 ? -1 : 1;
        return 0;
    }

    public boolean isNegative() {
        return a < 0;
    }

    public boolean isZero() {
        return a == 0 && b == 0 && c == 0 && d == 0;
    }

    public boolean isOne() {
        return a == 0 && b == 0 && c == 0 && d == 1;
    }

    public int signum() {
        return isZero() ? 0 : (a < 0 ? -1 : 1);
    }

    /* ==================== 位判断 ==================== */
    public boolean testBit(int n) {
        if (n < 0 || n >= 256) throw new IndexOutOfBoundsException("bit: " + n);
        return (component(n / 64) & (1L << (n & 63))) != 0;
    }

    /** 最低位（0 或 1） */
    public long lowBit() {
        return d & 1L;
    }

    /** 最低置位位的位置（-1 表示 0） */
    public int getLowestSetBit() {
        if (d != 0) return Long.numberOfTrailingZeros(d);
        if (c != 0) return 64 + Long.numberOfTrailingZeros(c);
        if (b != 0) return 128 + Long.numberOfTrailingZeros(b);
        if (a != 0) return 192 + Long.numberOfTrailingZeros(a);
        return -1;
    }

    /** 补码表示中除去符号位所需的位数（BigInteger.bitLength 语义） 正数: 最高有效位位置 + 1；负数: 最高 0 位位置 + 1；0: 0 */
    public int bitLength() {
        if (a == 0 && b == 0 && c == 0 && d == 0) return 0;
        if (!isNegative()) {
            if (a != 0) return 256 - Long.numberOfLeadingZeros(a);
            if (b != 0) return 192 - Long.numberOfLeadingZeros(b);
            if (c != 0) return 128 - Long.numberOfLeadingZeros(c);
            return 64 - Long.numberOfLeadingZeros(d);
        }
        // 负数: 找最高 0 位（~value 的最高 1 位）。避免 negate 在 MIN_VALUE 时溢出。
        if (a != -1L) return 256 - Long.numberOfLeadingZeros(~a);
        if (b != -1L) return 192 - Long.numberOfLeadingZeros(~b);
        if (c != -1L) return 128 - Long.numberOfLeadingZeros(~c);
        if (d != -1L) return 64 - Long.numberOfLeadingZeros(~d);
        return 0; // -1
    }

    /* ==================== 转换 ==================== */
    // 🔧 修复：高 192 位全 1 且 d≥0 时值超出 long 范围（负值位模式），必须抛异常
    public long longValue() {
        if (a == 0L && b == 0L && c == 0L && d >= 0) return d;
        if (a == -1L && b == -1L && c == -1L && d < 0) return d;
        throw new ArithmeticException("Int256 out of long range");
    }

    public BigInteger toBigInteger() {
        if (cachedBigInteger != null) return cachedBigInteger;
        if (a == 0L && b == 0L && c == 0L && d >= 0) {
            return cachedBigInteger = BigInteger.valueOf(d);
        }
        return cachedBigInteger = new BigInteger(toByteArray());
    }

    public byte[] toByteArray() {
        byte[] buf = new byte[32];
        putLong(buf, 0, a);
        putLong(buf, 8, b);
        putLong(buf, 16, c);
        putLong(buf, 24, d);
        return buf;
    }

    private static void putLong(byte[] arr, int off, long val) {
        arr[off] = (byte) (val >>> 56);
        arr[off + 1] = (byte) (val >>> 48);
        arr[off + 2] = (byte) (val >>> 40);
        arr[off + 3] = (byte) (val >>> 32);
        arr[off + 4] = (byte) (val >>> 24);
        arr[off + 5] = (byte) (val >>> 16);
        arr[off + 6] = (byte) (val >>> 8);
        arr[off + 7] = (byte) (val);
    }

    @Override
    public String toString() {
        return toBigInteger().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Int256 other)) return false;
        return a == other.a && b == other.b && c == other.c && d == other.d;
    }

    @Override
    public int hashCode() {
        if (hash == HASH_NOT_CACHED) hash = (int) (a ^ b ^ c ^ d);
        return hash;
    }

    // ──────────── 内联算术助手 ────────────
    private static boolean unsignedLessThan(long x, long y) {
        return Long.compareUnsigned(x, y) < 0;
    }

    private static boolean unsignedLessOrEqual(long x, long y) {
        return Long.compareUnsigned(x, y) <= 0;
    }

    private static boolean unsignedGreaterThan(long x, long y) {
        return Long.compareUnsigned(x, y) > 0;
    }

    private static boolean unsignedGreaterOrEqual(long x, long y) {
        return Long.compareUnsigned(x, y) >= 0;
    }

    // ══════════════════════ 测试 ══════════════════════
    public static void main(String[] args) {
        System.out.println("=== Int256 测试 ===");
        System.out.println("ZERO = " + ZERO);
        System.out.println("ONE  = " + ONE);
        System.out.println("MAX_LONG + 1 = " + of(Long.MAX_VALUE).add(ONE));
        System.out.println("2 * MAX_LONG = " + of(Long.MAX_VALUE).multiply(TWO));
        System.out.println("2^128 = " + ONE.shiftLeft(128));
        System.out.println("2^128/2 = " + ONE.shiftLeft(128).divide(TWO));
        System.out.println("2^128 * 2^128 = " + ONE.shiftLeft(128).multiply(ONE.shiftLeft(128)));
        System.out.println("MIN_VALUE = " + MIN_VALUE);
        System.out.println("MIN_VALUE.negate() = " + MIN_VALUE.negate());
        System.out.println("MIN_VALUE.bitLength() = " + MIN_VALUE.bitLength());
        System.out.println("-1.bitLength() = " + MINUS_ONE.bitLength());
        System.out.println("-2.bitLength() = " + of(-2).bitLength());
        System.out.println("MAX_VALUE = " + MAX_VALUE);
    }
}
