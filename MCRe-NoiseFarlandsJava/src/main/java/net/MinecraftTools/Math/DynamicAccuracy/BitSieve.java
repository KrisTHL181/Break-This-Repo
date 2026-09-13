package net.MinecraftTools.Math.DynamicAccuracy;

/**
 * BitSieve — 素数候选筛
 *
 * <p>OpenJDK 25.0.3 移植版 + MCRe NoiseFarlands 性能优化： 1. retrieve() 用 Long.numberOfTrailingZeros 批量跳 0
 * — 省掉 O(n) 空转 2. 小 BigInteger 缓存 — 避免循环内重复分配 3. ThreadLocal MutableBigInteger 复用 — 减少构造时 GC
 */
final class BitSieve {

    private final long[] bits;
    private final int length;

    private static final BitSieve smallSieve = new BitSieve();

    // ──────────────── 小值缓存 ────────────────
    private static final int CACHE_BOUND = 384; // 覆盖常见 offset 范围
    private static final BigInteger[] OFFSET_CACHE = new BigInteger[CACHE_BOUND];

    static {
        for (int i = 0; i < CACHE_BOUND; i++) {
            OFFSET_CACHE[i] = BigInteger.valueOf(i);
        }
    }

    private static BigInteger cachedValueOf(long val) {
        if (val >= 0 && val < CACHE_BOUND) {
            return OFFSET_CACHE[(int) val];
        }
        return BigInteger.valueOf(val);
    }

    // ──────────────── ThreadLocal MutableBigInteger ────────────────
    private static final ThreadLocal<
            MutableBigInteger> TL_B = ThreadLocal.withInitial(MutableBigInteger::new);
    private static final ThreadLocal<
            MutableBigInteger> TL_Q = ThreadLocal.withInitial(MutableBigInteger::new);

    // ──────────────── 静态小筛构造 ────────────────
    private BitSieve() {
        length = 150 * 64;
        bits = new long[(unitIndex(length - 1) + 1)];

        set(0); // 1 is not prime
        int nextIndex = 1;
        int nextPrime = 3;

        do {
            sieveSingleInternal(length, nextIndex + nextPrime, nextPrime);
            nextIndex = sieveSearch(length, nextIndex + 1);
            nextPrime = 2 * nextIndex + 1;
        } while ((nextIndex > 0) && (nextPrime < length));
    }

    // ──────────────── public constructor ────────────────
    BitSieve(BigInteger base, int searchLen) {
        bits = new long[(unitIndex(searchLen - 1) + 1)];
        length = searchLen;
        int start = 0;

        int step = smallSieve.sieveSearch(smallSieve.length, start);
        int convertedStep = (step * 2) + 1;

        // 复用 ThreadLocal 对象
        MutableBigInteger b = TL_B.get();
        MutableBigInteger q = TL_Q.get();
        b.copyValue(base.mag);
        b.intLen = base.mag.length;
        b.offset = 0;

        do {
            start = b.divideOneWord(convertedStep, q);
            start = convertedStep - start;
            if (start % 2 == 0)
                start += convertedStep;

            sieveSingleInternal(searchLen, (start - 1) / 2, convertedStep);

            step = smallSieve.sieveSearch(smallSieve.length, step + 1);
            convertedStep = (step * 2) + 1;
        } while (step > 0);
    }

    // ──────────────── Bit helpers ────────────────
    private static int unitIndex(int bitIndex) {
        return bitIndex >>> 6;
    }

    private static long bit(int bitIndex) {
        return 1L << (bitIndex & 63);
    }

    private boolean get(int bitIndex) {
        return (bits[unitIndex(bitIndex)] & bit(bitIndex)) != 0;
    }

    private void set(int bitIndex) {
        bits[unitIndex(bitIndex)] |= bit(bitIndex);
    }

    // ─── sieveSearch (unchanged) ───
    private int sieveSearch(int limit, int start) {
        if (start >= limit)
            return -1;

        int index = start;
        do {
            if (!get(index))
                return index;
            index++;
        } while (index < limit - 1);
        return -1;
    }

    // ─── sieveSingle (unchanged logic) ───
    private void sieveSingleInternal(int limit, int start, int step) {
        while (start < limit) {
            set(start);
            start += step;
        }
    }

    // used by static init
    private void sieveSingle(int limit, int start, int step) {
        sieveSingleInternal(limit, start, step);
    }

    // ──────────────── PRIME SITE (the money) ────────────────
    // 核心优化：批量跳过连续的 0 (已筛位)
    BigInteger retrieve(BigInteger initValue, int certainty, java.util.Random random) {
        long offset = 1;

        for (int i = 0; i < bits.length; i++) {
            long word = bits[i];
            long candidates = ~word;
            long wordEndOffset = 1L + (long) (i + 1) * 128L; // 该 word 最后一位之后的偏移

            while (candidates != 0) {
                int tz = Long.numberOfTrailingZeros(candidates);
                if (tz > 0) {
                    offset += (long) tz * 2;
                    candidates >>>= tz;
                }
                BigInteger candidate = initValue.add(cachedValueOf(offset));
                if (candidate.primeToCertainty(certainty, random))
                    return candidate;
                candidates >>>= 1;
                offset += 2;
            }
            offset = wordEndOffset; // 直接跳到下一个 word 起点
        }
        return null;
    }
}