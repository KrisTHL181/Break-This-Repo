package net.MinecraftTools.Math._256Bit;

import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;

/**
 * UInt256 — 无符号 256-bit 整数
 *
 * <p>内部: long[4] = {a, b, c, d} = {bits 255..192, 191..128, 127..64, 63..0}，纯无符号解释
 * 范围: [0, 2^256 - 1] 零 GC，全 long 运算，无 BigInteger 依赖（除 toString）
 *
 * <p>用途: 块坐标映射、哈希、距离计算、无符号噪声
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class UInt256 extends Number implements Comparable<UInt256> {

    // ──────── 内部存储 (immutable) ────────
    final long a; // bits 255..192
    final long b; // bits 191..128
    final long c; // bits 127..64
    final long d; // bits 63..0

    // ──────── 缓存 ────────
    private transient BigInteger cachedBigInteger;
    private transient BigDecimal cachedBigDecimal;
    private transient int hash;
    private static final int HASH_NOT_CACHED = Integer.MIN_VALUE;

    // ──────── 常量 ────────
    public static final UInt256 ZERO = new UInt256(0L, 0L, 0L, 0L);
    public static final UInt256 ONE = new UInt256(0L, 0L, 0L, 1L);
    public static final UInt256 TWO = new UInt256(0L, 0L, 0L, 2L);
    public static final UInt256 THREE = new UInt256(0L, 0L, 0L, 3L);
    public static final UInt256 FOUR = new UInt256(0L, 0L, 0L, 4L);
    public static final UInt256 FIVE = new UInt256(0L, 0L, 0L, 5L);
    public static final UInt256 TEN = new UInt256(0L, 0L, 0L, 10L);
    public static final UInt256 MAX_VALUE = new UInt256(-1L, -1L, -1L, -1L);

    // ──────── 小值缓存 ────────
    private static final UInt256[] SMALL = new UInt256[256];

    static {
        for (int i = 0; i < 256; i++) {
            SMALL[i] = new UInt256(0L, 0L, 0L, (long) i);
        }
    }

    // ──────── 构造（私有）────────
    private UInt256(long a, long b, long c, long d) {
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
        return toBigDecimal().doubleValue();
    }

    private BigDecimal toBigDecimal() {
        if (cachedBigDecimal != null) return cachedBigDecimal;
        return cachedBigDecimal = new BigDecimal(toBigInteger());
    }

    // ──────── 工厂 ────────

    /** 从无符号 long（零扩展） */
    public static UInt256 of(long value) {
        if (value >= 0 && value < 256) return SMALL[(int) value];
        return new UInt256(0L, 0L, 0L, value);
    }

    /** 从 4 个无符号 long（[0] 最高） */
    public static UInt256 of(long a, long b, long c, long d) {
        return new UInt256(a, b, c, d);
    }

    /** 大端 32 字节 */
    public static UInt256 of(byte[] bytes) {
        if (bytes.length != 32) throw new IllegalArgumentException("need 32 bytes");
        return new UInt256(aggregate(bytes, 0), aggregate(bytes, 8),
                aggregate(bytes, 16), aggregate(bytes, 24));
    }

    /** 从 BigInteger（取低 256 位，无符号截断） */
    public static UInt256 of(BigInteger value) {
        // 🔧 MCRe：无符号 long 范围快路径——非负且 bitLength≤64 必在 [0, 2^64) 内，
        // longValue() 按无符号位模式解释（≥2^63 时为负 long 也正确）
        if (value.signum() >= 0 && value.bitLength() <= 64) {
            return of(value.longValue());
        }
        byte[] mag = value.toByteArray();
        byte[] buf = new byte[32];
        if (mag.length >= 32) {
            System.arraycopy(mag, mag.length - 32, buf, 0, 32);
        } else {
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

    /** 从 Int256 按位模式转换（符号位变数值） */
    public static UInt256 fromInt256(Int256 val) {
        return new UInt256(val.a, val.b, val.c, val.d);
    }

    // ──────── 核心运算 ────────

    /* ==================== 加法 ==================== */
    public UInt256 add(UInt256 o) {
        long d0 = d + o.d;
        long c0 = (Long.compareUnsigned(d0, d) < 0) ? 1L : 0L;
        long c1 = c + o.c + c0;
        long c2 = (c0 != 0 && Long.compareUnsigned(c1, c) <= 0) ||
                (c0 == 0 && Long.compareUnsigned(c1, c) < 0) ? 1L : 0L;
        long b1 = b + o.b + c2;
        long c3 = (c2 != 0 && Long.compareUnsigned(b1, b) <= 0) ||
                (c2 == 0 && Long.compareUnsigned(b1, b) < 0) ? 1L : 0L;
        long a1 = a + o.a + c3;
        return new UInt256(a1, b1, c1, d0);
    }

    /* ==================== 减法 ==================== */
    public UInt256 subtract(UInt256 o) {
        long d0 = d - o.d;
        long b0 = Long.compareUnsigned(d, o.d) < 0 ? 1L : 0L;
        long c1 = c - o.c - b0;
        long b1 = (b0 != 0 && Long.compareUnsigned(c, c1) <= 0) ||
                (b0 == 0 && Long.compareUnsigned(c, c1) < 0) ? 1L : 0L;
        long b1val = b - o.b - b1;
        long b2 = (b1 != 0 && Long.compareUnsigned(b, b1val) <= 0) ||
                (b1 == 0 && Long.compareUnsigned(b, b1val) < 0) ? 1L : 0L;
        long a1 = a - o.a - b2;
        return new UInt256(a1, b1val, c1, d0);
    }

    /* ==================== 乘法 ==================== */
    public UInt256 multiply(UInt256 o) {
        // 4×4 无符号 limb 乘法 → 512-bit 中间结果（小端 r[0..7]），取低 256 bit。
        long[] x = {d, c, b, a};   // 小端
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
        return new UInt256(r[3], r[2], r[1], r[0]);
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
    // 🔧 重写：用 BigInteger 除法避免二分搜索的乘法溢出问题
    public UInt256 divide(UInt256 divisor) {
        if (divisor.isZero()) throw new ArithmeticException("/ by zero");
        if (this.isZero()) return ZERO;
        if (divisor.equals(ONE)) return this;
        return UInt256.of(this.toBigInteger().divide(divisor.toBigInteger()));
    }

    public UInt256 remainder(UInt256 divisor) {
        if (divisor.isZero()) throw new ArithmeticException("/ by zero");
        return UInt256.of(this.toBigInteger().remainder(divisor.toBigInteger()));
    }

    /* ==================== 移位 ==================== */
    // 🔧 修复：局部变量 int b → int bits 避免遮蔽字段 b
    public UInt256 shiftLeft(int n) {
        if (n == 0) return this;
        if (n < 0) return shiftRight(-n);
        if (n >= 256) return ZERO;
        int w = n / 64;
        int bits = n % 64;
        if (bits == 0) {
            return switch (w) {
                case 0 -> this;
                case 1 -> new UInt256(this.b, c, d, 0L);
                case 2 -> new UInt256(c, d, 0L, 0L);
                case 3 -> new UInt256(d, 0L, 0L, 0L);
                default -> ZERO;
            };
        }
        int r = 64 - bits;
        long[] arr = new long[4];
        long[] src = {a, this.b, c, d};
        for (int i = 0; i < 4 - w; i++) {
            arr[i] = src[i + w] << bits;
            if (i + w + 1 < 4) arr[i] |= src[i + w + 1] >>> r;
        }
        return new UInt256(arr[0], arr[1], arr[2], arr[3]);
    }

    // 🔧 修复：bits 改名 + 方向 src[i+w] + 越界防护
    public UInt256 shiftRight(int n) {
        if (n == 0) return this;
        if (n < 0) return shiftLeft(-n);
        if (n >= 256) return ZERO;
        int w = n / 64;
        int bits = n % 64;
        if (bits == 0) {
            return switch (w) {
                case 0 -> this;
                case 1 -> new UInt256(0L, a, this.b, c);
                case 2 -> new UInt256(0L, 0L, a, this.b);
                case 3 -> new UInt256(0L, 0L, 0L, a);
                default -> ZERO;
            };
        }
        int l = 64 - bits;
        long[] src = {a, this.b, c, d};
        long[] arr = new long[4];
        for (int i = 0; i < 4; i++) {
            // 🔧 大端 src[0]=最高。右移 w 个整 limb + bits 位：
            //   主部分 = src[i-w]，移入部分 = src[i-w-1]（最高低位进位），无符号 >>> 
            int hiIdx = i - w;
            int loIdx = hiIdx - 1;
            long high = hiIdx >= 0 ? src[hiIdx] : 0L;
            long low = loIdx >= 0 ? src[loIdx] : 0L;
            arr[i] = (high >>> bits) | (low << l);
        }
        return new UInt256(arr[0], arr[1], arr[2], arr[3]);
    }

    /* ==================== 位运算 ==================== */
    public UInt256 and(UInt256 o) {
        return new UInt256(a & o.a, b & o.b, c & o.c, d & o.d);
    }

    public UInt256 or(UInt256 o) {
        return new UInt256(a | o.a, b | o.b, c | o.c, d | o.d);
    }

    public UInt256 xor(UInt256 o) {
        return new UInt256(a ^ o.a, b ^ o.b, c ^ o.c, d ^ o.d);
    }

    public UInt256 not() {
        return new UInt256(~a, ~b, ~c, ~d);
    }

    /** 低 mask 位全 1 的掩码（mask ≤ 256），超出部分为 0 */
    public UInt256 maskBelow(int mask) {
        if (mask <= 0) return ZERO;
        if (mask >= 256) return MAX_VALUE;
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
        return new UInt256(m0, m1, m2, m3);
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
    public int compareTo(UInt256 o) {
        int c = Long.compareUnsigned(a, o.a);
        if (c != 0) return c;
        c = Long.compareUnsigned(b, o.b);
        if (c != 0) return c;
        c = Long.compareUnsigned(c, o.c);
        if (c != 0) return c;
        return Long.compareUnsigned(d, o.d);
    }

    public boolean isZero() {
        return a == 0 && b == 0 && c == 0 && d == 0;
    }

    public boolean isOne() {
        return a == 0 && b == 0 && c == 0 && d == 1;
    }

    /** 无符号比较的 signum（0 或 1） */
    public int signum() {
        return isZero() ? 0 : 1;
    }

    /* ==================== 位判断 ==================== */
    public boolean testBit(int n) {
        if (n < 0 || n >= 256) throw new IndexOutOfBoundsException("bit: " + n);
        return (component(n / 64) & (1L << (n & 63))) != 0;
    }

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

    /** 无符号 bitLength：最高非零位位置 + 1 */
    public int bitLength() {
        if (a != 0) return 256 - Long.numberOfLeadingZeros(a);
        if (b != 0) return 192 - Long.numberOfLeadingZeros(b);
        if (c != 0) return 128 - Long.numberOfLeadingZeros(c);
        if (d != 0) return 64 - Long.numberOfLeadingZeros(d);
        return 0;
    }

    /* ==================== 转换 ==================== */
    public long longValue() {
        if (a == 0 && b == 0 && c == 0) return d;
        throw new ArithmeticException("UInt256 out of long range");
    }

    public Int256 toInt256() {
        return Int256.of(a, b, c, d);
    }

    public BigInteger toBigInteger() {
        if (cachedBigInteger != null) return cachedBigInteger;
        if (a == 0L && b == 0L && c == 0L && d >= 0) {
            return cachedBigInteger = BigInteger.valueOf(d);
        }
        return cachedBigInteger = new BigInteger(1, toByteArray());
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
        if (!(o instanceof UInt256 other)) return false;
        return a == other.a && b == other.b && c == other.c && d == other.d;
    }

    @Override
    public int hashCode() {
        if (hash == HASH_NOT_CACHED) hash = (int) (a ^ b ^ c ^ d);
        return hash;
    }

    // ══════════════════════ 测试 ══════════════════════
    public static void main(String[] args) {
        System.out.println("=== UInt256 测试 ===");
        System.out.println("ZERO = " + ZERO);
        System.out.println("MAX  = " + MAX_VALUE);
        System.out.println("MAX + 1 = " + MAX_VALUE.add(ONE));
        System.out.println("2^128   = " + ONE.shiftLeft(128));
        System.out.println("2^128/2 = " + ONE.shiftLeft(128).divide(TWO));
        System.out.println("2^128 * 2^128 = " + ONE.shiftLeft(128).multiply(ONE.shiftLeft(128)));
        System.out.println("1000 * 1000 = " + UInt256.of(1000).multiply(UInt256.of(1000)));
        System.out.println("0xFFFF_FFFF.bitLength = " + UInt256.of(0xFFFF_FFFFL).bitLength());
        System.out.println("-1L 作为无符号 = " + UInt256.of(-1L));
        System.out.println("(-1L 无符号).bitLength = " + UInt256.of(-1L).bitLength());
    }
}
