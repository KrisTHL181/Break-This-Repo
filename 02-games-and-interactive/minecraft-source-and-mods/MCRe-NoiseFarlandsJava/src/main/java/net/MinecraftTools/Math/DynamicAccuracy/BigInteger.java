package net.MinecraftTools.Math.DynamicAccuracy;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.ObjectStreamException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;

// JDK internal APIs replaced with inline constants/implementations for MCRe NoiseFarlands compatibility
// - DoubleConsts/FloatConsts → inline constants below
// - ArraysSupport → Arrays.mismatch/hashCode replacements below  
// - //@Stable → removed (decorative only)

public class BigInteger extends Number implements Comparable<BigInteger> {

    final int signum;

    final int[] mag;

    private int bitCountPlusOne;

    private int magBitLengthPlusOne;

    private int lowestSetBitPlusTwo;

    private int numberOfTrailingZeroIntsPlusTwo;

    public static final long LONG_MASK = 0xffffffffL;

    private static final int MAX_MAG_LENGTH = Integer.MAX_VALUE / Integer.SIZE + 1;

    private static final int PRIME_SEARCH_BIT_LENGTH_LIMIT = 500000000;

    private static final int KARATSUBA_THRESHOLD = 80;

    private static final int TOOM_COOK_THRESHOLD = 240;

    private static final int KARATSUBA_SQUARE_THRESHOLD = 128;

    private static final int TOOM_COOK_SQUARE_THRESHOLD = 216;

    static final int BURNIKEL_ZIEGLER_THRESHOLD = 80;

    static final int BURNIKEL_ZIEGLER_OFFSET = 40;

    private static final int SCHOENHAGE_BASE_CONVERSION_THRESHOLD = 20;

    private static final int MULTIPLY_SQUARE_THRESHOLD = 20;

    private static final int MONTGOMERY_INTRINSIC_THRESHOLD = 512;

    public BigInteger(byte[] val, int off, int len) {
        if (val.length == 0) {
            throw new NumberFormatException("Zero length BigInteger");
        }
        Objects.checkFromIndexSize(off, len, val.length);
        if (len == 0) {
            mag = ZERO.mag;
            signum = ZERO.signum;
            return;
        }

        int b = val[off];
        if (b < 0) {
            mag = makePositive(b, val, off, len);
            signum = -1;
        } else {
            mag = stripLeadingZeroBytes(b, val, off, len);
            signum = (mag.length == 0 ? 0 : 1);
        }
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    public BigInteger(byte[] val) {
        this(val, 0, val.length);
    }

    private BigInteger(int[] val) {
        if (val.length == 0)
            throw new NumberFormatException("Zero length BigInteger");

        if (val[0] < 0) {
            mag = makePositive(val);
            signum = -1;
        } else {
            mag = trustedStripLeadingZeroInts(val);
            signum = (mag.length == 0 ? 0 : 1);
        }
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    // bitLength 和 bitCount 的延迟计算已经存在，但可以更激进
    // 对于临时运算结果，不立即计算这些值
    private BigInteger(boolean lazy, int[] mag, int signum) {
        this.mag = mag;
        this.signum = signum;
        // 不设置缓存字段，让它们在首次访问时计算
        this.bitCountPlusOne = 0;
        this.magBitLengthPlusOne = 0;
        this.lowestSetBitPlusTwo = 0;
        this.numberOfTrailingZeroIntsPlusTwo = 0;
    }

    public BigInteger(int signum, byte[] magnitude, int off, int len) {
        if (signum < -1 || signum > 1) {
            throw (new NumberFormatException("Invalid signum value"));
        }
        Objects.checkFromIndexSize(off, len, magnitude.length);

        this.mag = stripLeadingZeroBytes(magnitude, off, len);

        if (this.mag.length == 0) {
            this.signum = 0;
        } else {
            if (signum == 0)
                throw (new NumberFormatException("signum-magnitude mismatch"));
            this.signum = signum;
        }
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    public BigInteger(int signum, byte[] magnitude) {
        this(signum, magnitude, 0, magnitude.length);
    }

    private BigInteger(int signum, int[] magnitude) {
        this.mag = stripLeadingZeroInts(magnitude, false);

        if (signum < -1 || signum > 1)
            throw (new NumberFormatException("Invalid signum value"));

        if (this.mag.length == 0) {
            this.signum = 0;
        } else {
            if (signum == 0)
                throw (new NumberFormatException("signum-magnitude mismatch"));
            this.signum = signum;
        }
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    public BigInteger(String val, int radix) {
        int cursor = 0, numDigits;
        final int len = val.length();

        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            throw new NumberFormatException("Radix out of range");
        if (len == 0)
            throw new NumberFormatException("Zero length BigInteger");

        int sign = 1;
        int index1 = val.lastIndexOf('-');
        int index2 = val.lastIndexOf('+');
        if (index1 >= 0) {
            if (index1 != 0 || index2 >= 0) {
                throw new NumberFormatException("Illegal embedded sign character");
            }
            sign = -1;
            cursor = 1;
        } else if (index2 >= 0) {
            if (index2 != 0) {
                throw new NumberFormatException("Illegal embedded sign character");
            }
            cursor = 1;
        }
        if (cursor == len)
            throw new NumberFormatException("Zero length BigInteger");

        while (cursor < len &&
                Character.digit(val.charAt(cursor), radix) == 0) {
            cursor++;
        }

        if (cursor == len) {
            signum = 0;
            mag = ZERO.mag;
            return;
        }

        numDigits = len - cursor;
        signum = sign;

        long numBits = ((numDigits * bitsPerDigit[radix]) >>> 10) + 1;
        if (numBits + 31 >= (1L << 32)) {
            reportOverflow();
        }
        int numWords = (int) (numBits + 31) >>> 5;
        int[] magnitude = new int[numWords];

        int firstGroupLen = numDigits % digitsPerInt[radix];
        if (firstGroupLen == 0)
            firstGroupLen = digitsPerInt[radix];
        String group = val.substring(cursor, cursor += firstGroupLen);
        magnitude[numWords - 1] = Integer.parseInt(group, radix);
        if (magnitude[numWords - 1] < 0)
            throw new NumberFormatException("Illegal digit");

        int superRadix = intRadix[radix];
        int groupVal = 0;
        while (cursor < len) {
            group = val.substring(cursor, cursor += digitsPerInt[radix]);
            groupVal = Integer.parseInt(group, radix);
            if (groupVal < 0)
                throw new NumberFormatException("Illegal digit");
            destructiveMulAdd(magnitude, superRadix, groupVal);
        }

        mag = trustedStripLeadingZeroInts(magnitude);
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    BigInteger(char[] val, int sign, int len) {
        int cursor = 0, numDigits;

        while (cursor < len && Character.digit(val[cursor], 10) == 0) {
            cursor++;
        }
        if (cursor == len) {
            signum = 0;
            mag = ZERO.mag;
            return;
        }

        numDigits = len - cursor;
        signum = sign;

        int numWords;
        if (len < 10) {
            numWords = 1;
        } else {
            long numBits = ((numDigits * bitsPerDigit[10]) >>> 10) + 1;
            if (numBits + 31 >= (1L << 32)) {
                reportOverflow();
            }
            numWords = (int) (numBits + 31) >>> 5;
        }
        int[] magnitude = new int[numWords];

        int firstGroupLen = numDigits % digitsPerInt[10];
        if (firstGroupLen == 0)
            firstGroupLen = digitsPerInt[10];
        magnitude[numWords - 1] = parseInt(val, cursor, cursor += firstGroupLen);

        while (cursor < len) {
            int groupVal = parseInt(val, cursor, cursor += digitsPerInt[10]);
            destructiveMulAdd(magnitude, intRadix[10], groupVal);
        }
        mag = trustedStripLeadingZeroInts(magnitude);
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    private int parseInt(char[] source, int start, int end) {
        int result = Character.digit(source[start++], 10);
        if (result == -1)
            throw new NumberFormatException(new String(source));

        for (int index = start; index < end; index++) {
            int nextVal = Character.digit(source[index], 10);
            if (nextVal == -1)
                throw new NumberFormatException(new String(source));
            result = 10 * result + nextVal;
        }

        return result;
    }

    private static long bitsPerDigit[] = {0, 0,
            1024, 1624, 2048, 2378, 2648, 2875, 3072, 3247, 3402, 3543, 3672,
            3790, 3899, 4001, 4096, 4186, 4271, 4350, 4426, 4498, 4567, 4633,
            4696, 4756, 4814, 4870, 4923, 4975, 5025, 5074, 5120, 5166, 5210,
            5253, 5295
    };

    private static void destructiveMulAdd(int[] x, int y, int z) {

        long ylong = y & LONG_MASK;
        long zlong = z & LONG_MASK;
        int len = x.length;

        long product = 0;
        long carry = 0;
        for (int i = len - 1; i >= 0; i--) {
            product = ylong * (x[i] & LONG_MASK) + carry;
            x[i] = (int) product;
            carry = product >>> 32;
        }

        long sum = (x[len - 1] & LONG_MASK) + zlong;
        x[len - 1] = (int) sum;
        carry = sum >>> 32;
        for (int i = len - 2; i >= 0; i--) {
            sum = (x[i] & LONG_MASK) + carry;
            x[i] = (int) sum;
            carry = sum >>> 32;
        }
    }

    public BigInteger(String val) {
        this(val, 10);
    }

    public BigInteger(int numBits, Random rnd) {
        byte[] magnitude = randomBits(numBits, rnd);

        try {

            this.mag = stripLeadingZeroBytes(magnitude, 0, magnitude.length);

            if (this.mag.length == 0) {
                this.signum = 0;
            } else {
                this.signum = 1;
            }
            if (mag.length >= MAX_MAG_LENGTH) {
                checkRange();
            }
        } finally {
            Arrays.fill(magnitude, (byte) 0);
        }
    }

    private static byte[] randomBits(int numBits, Random rnd) {
        if (numBits < 0)
            throw new IllegalArgumentException("numBits must be non-negative");
        int numBytes = (int) (((long) numBits + 7) / 8);
        byte[] randomBits = new byte[numBytes];

        if (numBytes > 0) {
            rnd.nextBytes(randomBits);
            int excessBits = 8 * numBytes - numBits;
            randomBits[0] &= (byte) ((1 << (8 - excessBits)) - 1);
        }
        return randomBits;
    }

    public BigInteger(int bitLength, int certainty, Random rnd) {
        BigInteger prime;

        if (bitLength < 2)
            throw new ArithmeticException("bitLength < 2");
        prime = (bitLength < SMALL_PRIME_THRESHOLD
                ? smallPrime(bitLength, certainty, rnd)
                : largePrime(bitLength, certainty, rnd));
        signum = 1;
        mag = prime.mag;
    }

    private static final int SMALL_PRIME_THRESHOLD = 95;

    private static final int DEFAULT_PRIME_CERTAINTY = 100;

    public static BigInteger probablePrime(int bitLength, Random rnd) {
        if (bitLength < 2)
            throw new ArithmeticException("bitLength < 2");

        return (bitLength < SMALL_PRIME_THRESHOLD ? smallPrime(bitLength, DEFAULT_PRIME_CERTAINTY, rnd) : largePrime(bitLength, DEFAULT_PRIME_CERTAINTY, rnd));
    }

    private static BigInteger smallPrime(int bitLength, int certainty, Random rnd) {
        int magLen = (bitLength + 31) >>> 5;
        int temp[] = new int[magLen];
        int highBit = 1 << (bitLength - 1);
        int highMask = (highBit << 1) - 1;

        while (true) {

            for (int i = 0; i < magLen; i++)
                temp[i] = rnd.nextInt();
            temp[0] = (temp[0] & highMask) | highBit;
            if (bitLength > 2)
                temp[magLen - 1] |= 1;

            BigInteger p = new BigInteger(temp, 1);

            if (bitLength > 6) {
                long r = p.remainder(SMALL_PRIME_PRODUCT).longValue();
                if ((r % 3 == 0) || (r % 5 == 0) || (r % 7 == 0) || (r % 11 == 0) ||
                        (r % 13 == 0) || (r % 17 == 0) || (r % 19 == 0) || (r % 23 == 0) ||
                        (r % 29 == 0) || (r % 31 == 0) || (r % 37 == 0) || (r % 41 == 0))
                    continue;
            }

            if (bitLength < 4)
                return p;

            if (p.primeToCertainty(certainty, rnd))
                return p;
        }
    }

    private static final BigInteger SMALL_PRIME_PRODUCT = valueOf(3L * 5 * 7 * 11 * 13 * 17 * 19 * 23 * 29 * 31 * 37 * 41);

    private static BigInteger largePrime(int bitLength, int certainty, Random rnd) {
        BigInteger p;
        p = new BigInteger(bitLength, rnd).setBit(bitLength - 1);
        p.mag[p.mag.length - 1] &= ~1;

        int searchLen = getPrimeSearchLen(bitLength);
        BitSieve searchSieve = new BitSieve(p, searchLen);
        BigInteger candidate = searchSieve.retrieve(p, certainty, rnd);

        while ((candidate == null) || (candidate.bitLength() != bitLength)) {
            p = p.add(BigInteger.valueOf(2 * searchLen));
            if (p.bitLength() != bitLength)
                p = new BigInteger(bitLength, rnd).setBit(bitLength - 1);
            p.mag[p.mag.length - 1] &= ~1;
            searchSieve = new BitSieve(p, searchLen);
            candidate = searchSieve.retrieve(p, certainty, rnd);
        }
        return candidate;
    }

    public BigInteger nextProbablePrime() {
        if (this.signum < 0)
            throw new ArithmeticException("start < 0: " + this);

        if ((this.signum == 0) || this.equals(ONE))
            return TWO;

        BigInteger result = this.add(ONE);

        if (result.bitLength() < SMALL_PRIME_THRESHOLD) {

            if (!result.testBit(0))
                result = result.add(ONE);

            while (true) {

                if (result.bitLength() > 6) {
                    long r = result.remainder(SMALL_PRIME_PRODUCT).longValue();
                    if ((r % 3 == 0) || (r % 5 == 0) || (r % 7 == 0) || (r % 11 == 0) ||
                            (r % 13 == 0) || (r % 17 == 0) || (r % 19 == 0) || (r % 23 == 0) ||
                            (r % 29 == 0) || (r % 31 == 0) || (r % 37 == 0) || (r % 41 == 0)) {
                        result = result.add(TWO);
                        continue;
                    }
                }

                if (result.bitLength() < 4)
                    return result;

                if (result.primeToCertainty(DEFAULT_PRIME_CERTAINTY, null))
                    return result;

                result = result.add(TWO);
            }
        }

        if (result.testBit(0))
            result = result.subtract(ONE);

        int searchLen = getPrimeSearchLen(result.bitLength());

        while (true) {
            BitSieve searchSieve = new BitSieve(result, searchLen);
            BigInteger candidate = searchSieve.retrieve(result,
                    DEFAULT_PRIME_CERTAINTY, null);
            if (candidate != null)
                return candidate;
            result = result.add(BigInteger.valueOf(2 * searchLen));
        }
    }

    private static int getPrimeSearchLen(int bitLength) {
        if (bitLength > PRIME_SEARCH_BIT_LENGTH_LIMIT + 1) {
            throw new ArithmeticException("Prime search implementation restriction on bitLength");
        }
        return bitLength / 20 * 64;
    }

    boolean primeToCertainty(int certainty, Random random) {
        int rounds = 0;
        int n = (Math.min(certainty, Integer.MAX_VALUE - 1) + 1) / 2;

        int sizeInBits = this.bitLength();
        if (sizeInBits < 100) {
            rounds = 50;
            rounds = n < rounds ? n : rounds;
            return passesMillerRabin(rounds, random);
        }

        if (sizeInBits < 256) {
            rounds = 27;
        } else if (sizeInBits < 512) {
            rounds = 15;
        } else if (sizeInBits < 768) {
            rounds = 8;
        } else if (sizeInBits < 1024) {
            rounds = 4;
        } else {
            rounds = 2;
        }
        rounds = n < rounds ? n : rounds;

        return passesMillerRabin(rounds, random) && passesLucasLehmer();
    }

    private boolean passesLucasLehmer() {
        BigInteger thisPlusOne = this.add(ONE);

        int d = 5;
        while (jacobiSymbol(d, this) != -1) {

            d = (d < 0) ? Math.abs(d) + 2 : -(d + 2);
        }

        BigInteger u = lucasLehmerSequence(d, thisPlusOne, this);

        return u.mod(this).equals(ZERO);
    }

    private static int jacobiSymbol(int p, BigInteger n) {
        if (p == 0)
            return 0;

        int j = 1;
        int u = n.mag[n.mag.length - 1];

        if (p < 0) {
            p = -p;
            int n8 = u & 7;
            if ((n8 == 3) || (n8 == 7))
                j = -j;
        }

        while ((p & 3) == 0)
            p >>= 2;
        if ((p & 1) == 0) {
            p >>= 1;
            if (((u ^ (u >> 1)) & 2) != 0)
                j = -j;
        }
        if (p == 1)
            return j;

        if ((p & u & 2) != 0)
            j = -j;

        u = n.mod(BigInteger.valueOf(p)).intValue();

        while (u != 0) {
            while ((u & 3) == 0)
                u >>= 2;
            if ((u & 1) == 0) {
                u >>= 1;
                if (((p ^ (p >> 1)) & 2) != 0)
                    j = -j;
            }
            if (u == 1)
                return j;

            assert (u < p);
            int t = u;
            u = p;
            p = t;
            if ((u & p & 2) != 0)
                j = -j;

            u %= p;
        }
        return 0;
    }

    private static BigInteger lucasLehmerSequence(int z, BigInteger k, BigInteger n) {
        BigInteger d = BigInteger.valueOf(z);
        BigInteger u = ONE;
        BigInteger u2;
        BigInteger v = ONE;
        BigInteger v2;

        for (int i = k.bitLength() - 2; i >= 0; i--) {
            u2 = u.multiply(v).mod(n);

            v2 = v.square().add(d.multiply(u.square())).mod(n);
            if (v2.testBit(0))
                v2 = v2.subtract(n);

            v2 = v2.shiftRight(1);

            u = u2;
            v = v2;
            if (k.testBit(i)) {
                u2 = u.add(v).mod(n);
                if (u2.testBit(0))
                    u2 = u2.subtract(n);

                u2 = u2.shiftRight(1);
                v2 = v.add(d.multiply(u)).mod(n);
                if (v2.testBit(0))
                    v2 = v2.subtract(n);
                v2 = v2.shiftRight(1);

                u = u2;
                v = v2;
            }
        }
        return u;
    }

    private boolean passesMillerRabin(int iterations, Random rnd) {

        BigInteger thisMinusOne = this.subtract(ONE);
        BigInteger m = thisMinusOne;
        int a = m.getLowestSetBit();
        m = m.shiftRight(a);

        if (rnd == null) {
            rnd = ThreadLocalRandom.current();
        }
        for (int i = 0; i < iterations; i++) {

            BigInteger b;
            do {
                b = new BigInteger(this.bitLength(), rnd);
            } while (b.compareTo(ONE) <= 0 || b.compareTo(this) >= 0);

            int j = 0;
            BigInteger z = b.modPow(m, this);
            while (!((j == 0 && z.equals(ONE)) || z.equals(thisMinusOne))) {
                if (j > 0 && z.equals(ONE) || ++j == a)
                    return false;
                z = z.modPow(TWO, this);
            }
        }
        return true;
    }

    BigInteger(int[] magnitude, int signum) {
        this.signum = (magnitude.length == 0 ? 0 : signum);
        this.mag = magnitude;
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    private BigInteger(byte[] magnitude, int signum) {
        this.signum = (magnitude.length == 0 ? 0 : signum);
        this.mag = stripLeadingZeroBytes(magnitude, 0, magnitude.length);
        if (mag.length >= MAX_MAG_LENGTH) {
            checkRange();
        }
    }

    private void checkRange() {
        if (mag.length > MAX_MAG_LENGTH || mag.length == MAX_MAG_LENGTH && mag[0] < 0) {
            reportOverflow();
        }
    }

    private static void reportOverflow() {
        throw new ArithmeticException("BigInteger would overflow supported range");
    }

    // 添加常用值的缓存
    private static final BigInteger[] SMALL_CACHE = new BigInteger[4096];
    // 扩展常用值的缓存范围到 -65536 ~ 65535
    private static final int EXTENDED_CACHE_OFFSET = 65536;
    private static final BigInteger[] EXTENDED_CACHE = new BigInteger[131072]; // ±65536

    static {
        // 在原有 SMALL_CACHE 之后追加
        for (int i = 0; i < 131072; i++) {
            EXTENDED_CACHE[i] = new BigInteger(i - 65536);
        }
        for (int i = 0; i < 4096; i++) {
            SMALL_CACHE[i] = new BigInteger(i - 2048); // -2048 ~ 2047
        }
    }

    public static BigInteger valueOf(long val) {
        if (val == 0) return ZERO;
        if (val > 0 && val <= MAX_CONSTANT) return posConst[(int) val];
        else if (val < 0 && val >= -MAX_CONSTANT) return negConst[(int) -val];
        // 🔧 扩展缓存到 ±65536
        if (val >= -65536 && val <= 65535) {
            return EXTENDED_CACHE[(int) val + 65536];
        }
        return new BigInteger(val);
    }

    // 线程局部缓存，减少 GC 压力
    private static final ThreadLocal<
            int[]> SCRATCH_ARRAY = ThreadLocal.withInitial(() -> new int[128]);

    private static int[] getScratchArray(int minSize) {
        int[] arr = SCRATCH_ARRAY.get();
        if (arr.length < minSize) {
            arr = new int[minSize * 2];
            SCRATCH_ARRAY.set(arr);
        }
        return arr;
    }

    private BigInteger(long mag, int signum) {
        assert mag != 0 && signum != 0;
        this.signum = signum;
        this.mag = toMagArray(mag);
    }

    private BigInteger(long val) {
        if (val < 0) {
            val = -val;
            signum = -1;
        } else {
            signum = 1;
        }
        mag = toMagArray(val);
    }

    private static int[] toMagArray(long mag) {
        int highWord = (int) (mag >>> 32);
        return highWord == 0
                ? new int[]{(int) mag}
                : new int[]{highWord, (int) mag};
    }

    private static BigInteger valueOf(int[] val) {
        return (val[0] > 0 ? new BigInteger(val, 1) : new BigInteger(val));
    }

    private static final int MAX_CONSTANT = 16;
    private static final BigInteger[] posConst = new BigInteger[MAX_CONSTANT + 1];
    private static final BigInteger[] negConst = new BigInteger[MAX_CONSTANT + 1];

    private static volatile BigInteger[][] powerCache;

    private static final double[] logCache;

    private static final double LOG_TWO = Math.log(2.0);

    static {
        assert 0 < KARATSUBA_THRESHOLD
                        && KARATSUBA_THRESHOLD < TOOM_COOK_THRESHOLD
                        && TOOM_COOK_THRESHOLD < Integer.MAX_VALUE
                        && 0 < KARATSUBA_SQUARE_THRESHOLD
                        && KARATSUBA_SQUARE_THRESHOLD < TOOM_COOK_SQUARE_THRESHOLD
                        && TOOM_COOK_SQUARE_THRESHOLD < Integer.MAX_VALUE
                : "Algorithm thresholds are inconsistent";

        for (int i = 1; i <= MAX_CONSTANT; i++) {
            int[] magnitude = new int[1];
            magnitude[0] = i;
            posConst[i] = new BigInteger(magnitude, 1);
            negConst[i] = new BigInteger(magnitude, -1);
        }

        BigInteger[][] cache = new BigInteger[Character.MAX_RADIX + 1][];
        logCache = new double[Character.MAX_RADIX + 1];

        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            cache[i] = new BigInteger[]{BigInteger.valueOf(i)};
            logCache[i] = Math.log(i);
        }
        BigInteger.powerCache = cache;
    }

    public static final BigInteger ZERO = new BigInteger(new int[0], 0);

    public static final BigInteger ONE = valueOf(1);

    public static final BigInteger TWO = valueOf(2);

    private static final BigInteger NEGATIVE_ONE = valueOf(-1);

    public static final BigInteger TEN = valueOf(10);

    public BigInteger add(BigInteger val) {
        if (val.signum == 0)
            return this;
        if (signum == 0)
            return val;
        if (val.signum == signum)
            return new BigInteger(add(mag, val.mag), signum);

        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, val.mag)
                : subtract(val.mag, mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);

        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    BigInteger add(long val) {
        if (val == 0)
            return this;
        if (signum == 0)
            return valueOf(val);
        if (Long.signum(val) == signum)
            return new BigInteger(add(mag, Math.abs(val)), signum);
        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, Math.abs(val)) : subtract(Math.abs(val), mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);
        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    private static int[] add(int[] x, long val) {
        int[] y;
        long sum = 0;
        int xIndex = x.length;
        int[] result;
        int highWord = (int) (val >>> 32);
        if (highWord == 0) {
            result = new int[xIndex];
            sum = (x[--xIndex] & LONG_MASK) + val;
            result[xIndex] = (int) sum;
        } else {
            if (xIndex == 1) {
                result = new int[2];
                sum = val + (x[0] & LONG_MASK);
                result[1] = (int) sum;
                result[0] = (int) (sum >>> 32);
                return result;
            } else {
                result = new int[xIndex];
                sum = (x[--xIndex] & LONG_MASK) + (val & LONG_MASK);
                result[xIndex] = (int) sum;
                sum = (x[--xIndex] & LONG_MASK) + (highWord & LONG_MASK) + (sum >>> 32);
                result[xIndex] = (int) sum;
            }
        }

        boolean carry = (sum >>> 32 != 0);
        while (xIndex > 0 && carry)
            carry = ((result[--xIndex] = x[xIndex] + 1) == 0);

        while (xIndex > 0)
            result[--xIndex] = x[xIndex];

        if (carry) {
            int bigger[] = new int[result.length + 1];
            System.arraycopy(result, 0, bigger, 1, result.length);
            bigger[0] = 0x01;
            return bigger;
        }
        return result;
    }

    private static int[] add(int[] x, int[] y) {

        if (x.length < y.length) {
            int[] tmp = x;
            x = y;
            y = tmp;
        }

        int xIndex = x.length;
        int yIndex = y.length;
        int result[] = new int[xIndex];
        long sum = 0;
        if (yIndex == 1) {
            sum = (x[--xIndex] & LONG_MASK) + (y[0] & LONG_MASK);
            result[xIndex] = (int) sum;
        } else {

            while (yIndex > 0) {
                sum = (x[--xIndex] & LONG_MASK) +
                        (y[--yIndex] & LONG_MASK) + (sum >>> 32);
                result[xIndex] = (int) sum;
            }
        }

        boolean carry = (sum >>> 32 != 0);
        while (xIndex > 0 && carry)
            carry = ((result[--xIndex] = x[xIndex] + 1) == 0);

        while (xIndex > 0)
            result[--xIndex] = x[xIndex];

        if (carry) {
            int bigger[] = new int[result.length + 1];
            System.arraycopy(result, 0, bigger, 1, result.length);
            bigger[0] = 0x01;
            return bigger;
        }
        return result;
    }

    private static int[] subtract(long val, int[] little) {
        int highWord = (int) (val >>> 32);
        if (highWord == 0) {
            int result[] = new int[1];
            result[0] = (int) (val - (little[0] & LONG_MASK));
            return result;
        } else {
            int result[] = new int[2];
            if (little.length == 1) {
                long difference = ((int) val & LONG_MASK) - (little[0] & LONG_MASK);
                result[1] = (int) difference;

                boolean borrow = (difference >> 32 != 0);
                if (borrow) {
                    result[0] = highWord - 1;
                } else {
                    result[0] = highWord;
                }
                return result;
            } else {
                long difference = ((int) val & LONG_MASK) - (little[1] & LONG_MASK);
                result[1] = (int) difference;
                difference = (highWord & LONG_MASK) - (little[0] & LONG_MASK) + (difference >> 32);
                result[0] = (int) difference;
                return result;
            }
        }
    }

    private static int[] subtract(int[] big, long val) {
        int highWord = (int) (val >>> 32);
        int bigIndex = big.length;
        int result[] = new int[bigIndex];
        long difference = 0;

        if (highWord == 0) {
            difference = (big[--bigIndex] & LONG_MASK) - val;
            result[bigIndex] = (int) difference;
        } else {
            difference = (big[--bigIndex] & LONG_MASK) - (val & LONG_MASK);
            result[bigIndex] = (int) difference;
            difference = (big[
                            --bigIndex] & LONG_MASK) - (highWord & LONG_MASK) + (difference >> 32);
            result[bigIndex] = (int) difference;
        }

        boolean borrow = (difference >> 32 != 0);
        while (bigIndex > 0 && borrow)
            borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);

        while (bigIndex > 0)
            result[--bigIndex] = big[bigIndex];

        return result;
    }

    public BigInteger subtract(BigInteger val) {
        if (val.signum == 0)
            return this;
        if (signum == 0)
            return val.negate();
        if (val.signum != signum)
            return new BigInteger(add(mag, val.mag), signum);

        int cmp = compareMagnitude(val);
        if (cmp == 0)
            return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, val.mag)
                : subtract(val.mag, mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);
        return new BigInteger(resultMag, cmp == signum ? 1 : -1);
    }

    private static int[] subtract(int[] big, int[] little) {
        int bigIndex = big.length;
        int result[] = new int[bigIndex];
        int littleIndex = little.length;
        long difference = 0;

        while (littleIndex > 0) {
            difference = (big[--bigIndex] & LONG_MASK) -
                    (little[--littleIndex] & LONG_MASK) +
                    (difference >> 32);
            result[bigIndex] = (int) difference;
        }

        boolean borrow = (difference >> 32 != 0);
        while (bigIndex > 0 && borrow)
            borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);

        while (bigIndex > 0)
            result[--bigIndex] = big[bigIndex];

        return result;
    }

    public BigInteger multiply(BigInteger val) {
        if (val.signum == 0 || signum == 0) return ZERO;

        // 快速路径：小数字直接用 long
        if (mag.length == 1 && val.mag.length == 1) {
            long a = mag[0] & LONG_MASK;
            long b = val.mag[0] & LONG_MASK;
            long product = a * b;
            int sign = signum == val.signum ? 1 : -1;
            if (product <= Long.MAX_VALUE) {
                return valueOf(product * sign);
            }
            // 🔧 修复：原文件缺了逗号
            return new BigInteger(
            new int[]{(int) (product >>> 32), (int) product}, sign);
        }

        // 🔧 增加双 word 乘积的快速检测 (2 word × 2 word → 可能用 long)
        if (mag.length <= 2 && val.mag.length <= 2) {
            try {
                long xl = longValueExact();
                long yl = val.longValueExact();
                return BigInteger.valueOf(Math.multiplyExact(xl, yl));
            } catch (ArithmeticException e) {
                // 溢出了，走正常路径
            }
        }

        return multiply(val, false, false, 0);
    }

    public BigInteger parallelMultiply(BigInteger val) {
        return multiply(val, false, true, 0);
    }

    private BigInteger multiply(BigInteger val, boolean isRecursion, boolean parallel, int depth) {
        if (val.signum == 0 || signum == 0)
            return ZERO;

        int xlen = mag.length;

        if (val == this && xlen > MULTIPLY_SQUARE_THRESHOLD) {
            return square(true, parallel, depth);
        }

        int ylen = val.mag.length;

        if ((xlen < KARATSUBA_THRESHOLD) || (ylen < KARATSUBA_THRESHOLD)) {
            int resultSign = signum == val.signum ? 1 : -1;
            if (val.mag.length == 1) {
                return multiplyByInt(mag, val.mag[0], resultSign);
            }
            if (mag.length == 1) {
                return multiplyByInt(val.mag, mag[0], resultSign);
            }
            int[] result = multiplyToLen(mag, xlen,
            val.mag, ylen, null);
            result = trustedStripLeadingZeroInts(result);
            return new BigInteger(result, resultSign);
        } else {
            if ((xlen < TOOM_COOK_THRESHOLD) && (ylen < TOOM_COOK_THRESHOLD)) {
                return multiplyKaratsuba(this, val);
            } else {

                if (!isRecursion) {

                    if ((long) this.magBitLength() + val.magBitLength() >
                            (long) Integer.SIZE * MAX_MAG_LENGTH) {
                        reportOverflow();
                    }
                }

                return multiplyToomCook3(this, val, parallel, depth);
            }
        }
    }

    private static BigInteger multiplyByInt(int[] x, int y, int sign) {
        if (Integer.lowestOneBit(y) == y) {
            return new BigInteger(shiftLeft(x, Integer.numberOfTrailingZeros(y)), sign);
        }
        int xlen = x.length;
        int[] rmag = new int[xlen + 1];
        long carry = 0;
        long yl = y & LONG_MASK;
        int rstart = rmag.length - 1;
        for (int i = xlen - 1; i >= 0; i--) {
            long product = (x[i] & LONG_MASK) * yl + carry;
            rmag[rstart--] = (int) product;
            carry = product >>> 32;
        }
        if (carry == 0L) {
            rmag = Arrays.copyOfRange(rmag, 1, rmag.length);
        } else {
            rmag[rstart] = (int) carry;
        }
        return new BigInteger(rmag, sign);
    }

    BigInteger multiply(long v) {
        if (v == 0 || signum == 0)
            return ZERO;
        if (v == BigDecimal.INFLATED)
            return multiply(BigInteger.valueOf(v));
        int rsign = (v > 0 ? signum : -signum);
        if (v < 0)
            v = -v;
        long dh = v >>> 32;
        long dl = v & LONG_MASK;

        int xlen = mag.length;
        int[] value = mag;
        int[] rmag = (dh == 0L) ? (new int[xlen + 1]) : (new int[xlen + 2]);
        long carry = 0;
        int rstart = rmag.length - 1;
        for (int i = xlen - 1; i >= 0; i--) {
            long product = (value[i] & LONG_MASK) * dl + carry;
            rmag[rstart--] = (int) product;
            carry = product >>> 32;
        }
        rmag[rstart] = (int) carry;
        if (dh != 0L) {
            carry = 0;
            rstart = rmag.length - 2;
            for (int i = xlen - 1; i >= 0; i--) {
                long product = (value[i] & LONG_MASK) * dh +
                        (rmag[rstart] & LONG_MASK) + carry;
                rmag[rstart--] = (int) product;
                carry = product >>> 32;
            }
            rmag[0] = (int) carry;
        }
        if (carry == 0L)
            rmag = Arrays.copyOfRange(rmag, 1, rmag.length);
        return new BigInteger(rmag, rsign);
    }

    private static int[] multiplyToLen(int[] x, int xlen, int[] y, int ylen, int[] z) {
        multiplyToLenCheck(x, xlen);
        multiplyToLenCheck(y, ylen);

        if (z == null || z.length < (xlen + ylen))
            z = new int[xlen + ylen];

        return implMultiplyToLen(x, xlen, y, ylen, z);
    }

    
    private static int[] implMultiplyToLen(int[] x, int xlen, int[] y, int ylen, int[] z) {
        int xstart = xlen - 1;
        int ystart = ylen - 1;

        long carry = 0;
        for (int j = ystart, k = ystart + 1 + xstart; j >= 0; j--, k--) {
            long product = (y[j] & LONG_MASK) *
                            (x[xstart] & LONG_MASK) + carry;
            z[k] = (int) product;
            carry = product >>> 32;
        }
        z[xstart] = (int) carry;

        for (int i = xstart - 1; i >= 0; i--) {
            carry = 0;
            // 🔧 优化：提取 x[i] 到内层循环外，减少重复掩码运算与数组访问
            long xVal = x[i] & LONG_MASK;
            for (int j = ystart, k = ystart + 1 + i; j >= 0; j--, k--) {
                long product = (y[j] & LONG_MASK) * xVal +
                        (z[k] & LONG_MASK) + carry;
                z[k] = (int) product;
                carry = product >>> 32;
            }
            z[i] = (int) carry;
        }
        return z;
    }

    private static void multiplyToLenCheck(int[] array, int length) {
        if (length <= 0) {
            return;
        }

        Objects.requireNonNull(array);

        if (length > array.length) {
            throw new ArrayIndexOutOfBoundsException(length - 1);
        }
    }

    private static BigInteger multiplyKaratsuba(BigInteger x, BigInteger y) {
        int xlen = x.mag.length;
        int ylen = y.mag.length;

        int half = (Math.max(xlen, ylen) + 1) / 2;

        BigInteger xl = x.getLower(half);
        BigInteger xh = x.getUpper(half);
        BigInteger yl = y.getLower(half);
        BigInteger yh = y.getUpper(half);

        BigInteger p1 = xh.multiply(yh);
        BigInteger p2 = xl.multiply(yl);

        BigInteger p3 = xh.add(xl).multiply(yh.add(yl));

        BigInteger result = p1.shiftLeft(32 * half).add(p3.subtract(p1).subtract(p2)).shiftLeft(32 * half).add(p2);

        if (x.signum != y.signum) {
            return result.negate();
        } else {
            return result;
        }
    }

    @SuppressWarnings("serial")
    private abstract static sealed class RecursiveOp extends RecursiveTask<BigInteger> {

        // 提高并行阈值，避免小数字也走 ForkJoin
        private static final int PARALLEL_FORK_DEPTH_THRESHOLD = Math.max(4, calculateMaximumDepth(ForkJoinPool.getCommonPoolParallelism()));

        private static final int calculateMaximumDepth(int parallelism) {
            return 32 - Integer.numberOfLeadingZeros(parallelism);
        }

        final boolean parallel;

        final byte depth;

        private RecursiveOp(boolean parallel, int depth) {
            this.parallel = parallel;
            this.depth = (byte) depth;
        }

        private static int getParallelForkDepthThreshold() {
            if (Thread.currentThread() instanceof ForkJoinWorkerThread fjwt) {
                return calculateMaximumDepth(fjwt.getPool().getParallelism());
            } else {
                return PARALLEL_FORK_DEPTH_THRESHOLD;
            }
        }

        protected RecursiveTask<BigInteger> forkOrInvoke() {
            if (parallel && depth <= getParallelForkDepthThreshold()) fork();
            else invoke();
            return this;
        }

        @SuppressWarnings("serial")
        private static final class RecursiveMultiply extends RecursiveOp {
            private final BigInteger a;
            private final BigInteger b;

            public RecursiveMultiply(BigInteger a, BigInteger b, boolean parallel, int depth) {
                super(parallel, depth);
                this.a = a;
                this.b = b;
            }

            @Override
            public BigInteger compute() {
                return a.multiply(b, true, parallel, depth);
            }
        }

        @SuppressWarnings("serial")
        private static final class RecursiveSquare extends RecursiveOp {
            private final BigInteger a;

            public RecursiveSquare(BigInteger a, boolean parallel, int depth) {
                super(parallel, depth);
                this.a = a;
            }

            @Override
            public BigInteger compute() {
                return a.square(true, parallel, depth);
            }
        }

        private static RecursiveTask<
                        BigInteger> multiply(BigInteger a, BigInteger b, boolean parallel, int depth) {
            return new RecursiveMultiply(a, b, parallel, depth).forkOrInvoke();
        }

        private static RecursiveTask<BigInteger> square(BigInteger a, boolean parallel, int depth) {
            return new RecursiveSquare(a, parallel, depth).forkOrInvoke();
        }
    }

    private static BigInteger multiplyToomCook3(BigInteger a, BigInteger b, boolean parallel, int depth) {
        int alen = a.mag.length;
        int blen = b.mag.length;

        int largest = Math.max(alen, blen);

        int k = (largest + 2) / 3;

        int r = largest - 2 * k;

        BigInteger a0, a1, a2, b0, b1, b2;
        a2 = a.getToomSlice(k, r, 0, largest);
        a1 = a.getToomSlice(k, r, 1, largest);
        a0 = a.getToomSlice(k, r, 2, largest);
        b2 = b.getToomSlice(k, r, 0, largest);
        b1 = b.getToomSlice(k, r, 1, largest);
        b0 = b.getToomSlice(k, r, 2, largest);

        BigInteger v0, v1, v2, vm1, vinf, t1, t2, tm1, da1, db1;

        depth++;
        var v0_task = RecursiveOp.multiply(a0, b0, parallel, depth);
        da1 = a2.add(a0);
        db1 = b2.add(b0);
        var vm1_task = RecursiveOp.multiply(da1.subtract(a1), db1.subtract(b1), parallel, depth);
        da1 = da1.add(a1);
        db1 = db1.add(b1);
        var v1_task = RecursiveOp.multiply(da1, db1, parallel, depth);
        v2 = da1.add(a2).shiftLeft(1).subtract(a0).multiply(
                db1.add(b2).shiftLeft(1).subtract(b0), true, parallel, depth);
        vinf = a2.multiply(b2, true, parallel, depth);
        v0 = v0_task.join();
        vm1 = vm1_task.join();
        v1 = v1_task.join();

        t2 = v2.subtract(vm1).exactDivideBy3();
        tm1 = v1.subtract(vm1).shiftRight(1);
        t1 = v1.subtract(v0);
        t2 = t2.subtract(t1).shiftRight(1);
        t1 = t1.subtract(tm1).subtract(vinf);
        t2 = t2.subtract(vinf.shiftLeft(1));
        tm1 = tm1.subtract(t2);

        int ss = k * 32;

        BigInteger result = vinf.shiftLeft(ss).add(t2).shiftLeft(ss).add(t1).shiftLeft(ss).add(tm1).shiftLeft(ss).add(v0);

        if (a.signum != b.signum) {
            return result.negate();
        } else {
            return result;
        }
    }

    private BigInteger getToomSlice(int lowerSize, int upperSize, int slice,
            int fullsize) {
        int start, end, sliceSize, len, offset;

        len = mag.length;
        offset = fullsize - len;

        if (slice == 0) {
            start = 0 - offset;
            end = upperSize - 1 - offset;
        } else {
            start = upperSize + (slice - 1) * lowerSize - offset;
            end = start + lowerSize - 1;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            return ZERO;
        }

        sliceSize = (end - start) + 1;

        if (sliceSize <= 0) {
            return ZERO;
        }

        if (start == 0 && sliceSize >= len) {
            return this.abs();
        }

        int intSlice[] = new int[sliceSize];
        System.arraycopy(mag, start, intSlice, 0, sliceSize);

        return new BigInteger(trustedStripLeadingZeroInts(intSlice), 1);
    }

    private BigInteger exactDivideBy3() {
        int len = mag.length;
        int[] result = new int[len];
        long x, w, q, borrow;
        borrow = 0L;
        for (int i = len - 1; i >= 0; i--) {
            x = (mag[i] & LONG_MASK);
            w = x - borrow;
            if (borrow > x) {
                borrow = 1L;
            } else {
                borrow = 0L;
            }

            q = (w * 0xAAAAAAABL) & LONG_MASK;
            result[i] = (int) q;

            if (q >= 0x55555556L) {
                borrow++;
                if (q >= 0xAAAAAAABL)
                    borrow++;
            }
        }
        result = trustedStripLeadingZeroInts(result);
        return new BigInteger(result, signum);
    }

    private BigInteger getLower(int n) {
        int len = mag.length;

        if (len <= n) {
            return abs();
        }

        int lowerInts[] = new int[n];
        System.arraycopy(mag, len - n, lowerInts, 0, n);

        return new BigInteger(trustedStripLeadingZeroInts(lowerInts), 1);
    }

    private BigInteger getUpper(int n) {
        int len = mag.length;

        if (len <= n) {
            return ZERO;
        }

        int upperLen = len - n;
        int upperInts[] = new int[upperLen];
        System.arraycopy(mag, 0, upperInts, 0, upperLen);

        return new BigInteger(trustedStripLeadingZeroInts(upperInts), 1);
    }

    private BigInteger square() {
        return square(false, false, 0);
    }

    private BigInteger square(boolean isRecursion, boolean parallel, int depth) {
        if (signum == 0) {
            return ZERO;
        }
        int len = mag.length;

        if (len < KARATSUBA_SQUARE_THRESHOLD) {
            int[] z = squareToLen(mag, len, null);
            return new BigInteger(trustedStripLeadingZeroInts(z), 1);
        } else {
            if (len < TOOM_COOK_SQUARE_THRESHOLD) {
                return squareKaratsuba();
            } else {

                if (!isRecursion) {
                    if (magBitLength() > (Integer.SIZE / 2) * MAX_MAG_LENGTH) {
                        reportOverflow();
                    }
                }

                return squareToomCook3(parallel, depth);
            }
        }
    }

    private static final int[] squareToLen(int[] x, int len, int[] z) {
        int zlen = len << 1;
        if (z == null || z.length < zlen)
            z = new int[zlen];

        implSquareToLenChecks(x, len, z, zlen);
        return implSquareToLen(x, len, z, zlen);
    }

    private static void implSquareToLenChecks(int[] x, int len, int[] z, int zlen)
            throws RuntimeException {
        if (len < 1) {
            throw new IllegalArgumentException("invalid input length: " + len);
        }
        if (len > x.length) {
            throw new IllegalArgumentException("input length out of bound: " +
                    len + " > " + x.length);
        }
        if (len * 2 > z.length) {
            throw new IllegalArgumentException("input length out of bound: " +
                    (len * 2) + " > " + z.length);
        }
        if (zlen < 1) {
            throw new IllegalArgumentException("invalid input length: " + zlen);
        }
        if (zlen > z.length) {
            throw new IllegalArgumentException("input length out of bound: " +
                    len + " > " + z.length);
        }
    }

    
    private static final int[] implSquareToLen(int[] x, int len, int[] z, int zlen) {

        int lastProductLowWord = 0;
        for (int j = 0, i = 0; j < len; j++) {
            long piece = (x[j] & LONG_MASK);
            long product = piece * piece;
            z[i++] = (lastProductLowWord << 31) | (int) (product >>> 33);
            z[i++] = (int) (product >>> 1);
            lastProductLowWord = (int) product;
        }

        for (int i = len, offset = 1; i > 0; i--, offset += 2) {
            int t = x[i - 1];
            t = mulAdd(z, x, offset, i - 1, t);
            addOne(z, offset - 1, i, t);
        }

        primitiveLeftShift(z, zlen, 1);
        z[zlen - 1] |= x[len - 1] & 1;

        return z;
    }

    private BigInteger squareKaratsuba() {
        int half = (mag.length + 1) / 2;

        BigInteger xl = getLower(half);
        BigInteger xh = getUpper(half);

        BigInteger xhs = xh.square();
        BigInteger xls = xl.square();

        return xhs.shiftLeft(half * 32).add(xl.add(xh).square().subtract(xhs.add(xls))).shiftLeft(half * 32).add(xls);
    }

    private BigInteger squareToomCook3(boolean parallel, int depth) {
        int len = mag.length;

        int k = (len + 2) / 3;

        int r = len - 2 * k;

        BigInteger a0, a1, a2;
        a2 = getToomSlice(k, r, 0, len);
        a1 = getToomSlice(k, r, 1, len);
        a0 = getToomSlice(k, r, 2, len);
        BigInteger v0, v1, v2, vm1, vinf, t1, t2, tm1, da1;

        depth++;
        var v0_fork = RecursiveOp.square(a0, parallel, depth);
        da1 = a2.add(a0);
        var vm1_fork = RecursiveOp.square(da1.subtract(a1), parallel, depth);
        da1 = da1.add(a1);
        var v1_fork = RecursiveOp.square(da1, parallel, depth);
        vinf = a2.square(true, parallel, depth);
        v2 = da1.add(a2).shiftLeft(1).subtract(a0).square(true, parallel, depth);
        v0 = v0_fork.join();
        vm1 = vm1_fork.join();
        v1 = v1_fork.join();

        t2 = v2.subtract(vm1).exactDivideBy3();
        tm1 = v1.subtract(vm1).shiftRight(1);
        t1 = v1.subtract(v0);
        t2 = t2.subtract(t1).shiftRight(1);
        t1 = t1.subtract(tm1).subtract(vinf);
        t2 = t2.subtract(vinf.shiftLeft(1));
        tm1 = tm1.subtract(t2);

        int ss = k * 32;

        return vinf.shiftLeft(ss).add(t2).shiftLeft(ss).add(t1).shiftLeft(ss).add(tm1).shiftLeft(ss).add(v0);
    }

    public BigInteger divide(BigInteger val) {
        if (val.mag.length < BURNIKEL_ZIEGLER_THRESHOLD ||
                mag.length - val.mag.length < BURNIKEL_ZIEGLER_OFFSET) {
            return divideKnuth(val);
        } else {
            return divideBurnikelZiegler(val);
        }
    }

    private BigInteger divideKnuth(BigInteger val) {
        MutableBigInteger q = MutableBigInteger.acquire();
        MutableBigInteger a = MutableBigInteger.acquire();
        MutableBigInteger b = MutableBigInteger.acquire();
        try {
            a.copyValue(this.mag);
            b.copyValue(val.mag);
            a.divideKnuth(b, q, false);
            return q.toBigIntegerCopy(this.signum * val.signum);
        } finally {
            MutableBigInteger.release(q);
            MutableBigInteger.release(a);
            MutableBigInteger.release(b);
        }
    }

    public BigInteger[] divideAndRemainder(BigInteger val) {
        if (val.mag.length < BURNIKEL_ZIEGLER_THRESHOLD ||
                mag.length - val.mag.length < BURNIKEL_ZIEGLER_OFFSET) {
            return divideAndRemainderKnuth(val);
        } else {
            return divideAndRemainderBurnikelZiegler(val);
        }
    }

    private BigInteger[] divideAndRemainderKnuth(BigInteger val) {
        BigInteger[] result = new BigInteger[2];
        MutableBigInteger q = MutableBigInteger.acquire();
        MutableBigInteger a = MutableBigInteger.acquire();
        MutableBigInteger b = MutableBigInteger.acquire();
        try {
            a.copyValue(this.mag);
            b.copyValue(val.mag);
            MutableBigInteger r = a.divideKnuth(b, q);
            result[0] = q.toBigIntegerCopy(this.signum == val.signum ? 1 : -1);
            result[1] = r.toBigIntegerCopy(this.signum);
            return result;
        } finally {
            MutableBigInteger.release(q);
            MutableBigInteger.release(a);
            MutableBigInteger.release(b);
        }
    }

    public BigInteger remainder(BigInteger val) {
        if (val.mag.length < BURNIKEL_ZIEGLER_THRESHOLD ||
                mag.length - val.mag.length < BURNIKEL_ZIEGLER_OFFSET) {
            return remainderKnuth(val);
        } else {
            return remainderBurnikelZiegler(val);
        }
    }

    private BigInteger remainderKnuth(BigInteger val) {
        MutableBigInteger q = MutableBigInteger.acquire();
        MutableBigInteger a = MutableBigInteger.acquire();
        MutableBigInteger b = MutableBigInteger.acquire();
        try {
            a.copyValue(this.mag);
            b.copyValue(val.mag);
            MutableBigInteger r = a.divideKnuth(b, q);
            return r.toBigIntegerCopy(this.signum);
        } finally {
            MutableBigInteger.release(q);
            MutableBigInteger.release(a);
            MutableBigInteger.release(b);
        }
    }

    private BigInteger divideBurnikelZiegler(BigInteger val) {
        return divideAndRemainderBurnikelZiegler(val)[0];
    }

    private BigInteger remainderBurnikelZiegler(BigInteger val) {
        return divideAndRemainderBurnikelZiegler(val)[1];
    }

    private BigInteger[] divideAndRemainderBurnikelZiegler(BigInteger val) {
        MutableBigInteger q = new MutableBigInteger();
        MutableBigInteger r = new MutableBigInteger(this).divideAndRemainderBurnikelZiegler(new MutableBigInteger(val), q);
        BigInteger qBigInt = q.isZero() ? ZERO : q.toBigInteger(signum * val.signum);
        BigInteger rBigInt = r.isZero() ? ZERO : r.toBigInteger(signum);
        return new BigInteger[]{qBigInt, rBigInt};
    }

    public BigInteger pow(int exponent) {
        if (exponent < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        if (exponent == 0 || this.equals(ONE))
            return ONE;

        if (signum == 0 || exponent == 1)
            return this;

        BigInteger base = this.abs();
        final boolean negative = signum < 0 && (exponent & 1) == 1;

        final int powersOfTwo = base.getLowestSetBit();
        final long bitsToShiftLong = (long) powersOfTwo * exponent;
        final int bitsToShift = (int) bitsToShiftLong;
        if (bitsToShift != bitsToShiftLong) {
            reportOverflow();
        }

        base = base.shiftRight(powersOfTwo);
        final int remainingBits = base.bitLength();
        if (remainingBits == 1)
            return (negative ? NEGATIVE_ONE : ONE).shiftLeft(bitsToShift);

        final long scaleFactor = (long) remainingBits * exponent;

        if (scaleFactor <= Long.SIZE) {

            final int newSign = negative ? -1 : 1;
            final long result = unsignedLongPow(base.mag[0] & LONG_MASK, exponent);

            return bitsToShift + scaleFactor <= Long.SIZE
                    ? new BigInteger(result << bitsToShift, newSign)
                    : new BigInteger(result, newSign).shiftLeft(bitsToShift);
        }

        if (scaleFactor + bitsToShift - exponent >= Integer.MAX_VALUE) {
            reportOverflow();
        }

        BigInteger answer = ONE;

        final int expZeros = Integer.numberOfLeadingZeros(exponent);
        int workingExp = exponent << expZeros;

        for (int expLen = Integer.SIZE - expZeros; expLen > 0; expLen--) {
            // 🔧 MCRe：平方用 square() 专用路径（Karatsuba/ToomCook），比 multiply(answer) 快
            answer = answer.square();
            if (workingExp < 0)
                answer = answer.multiply(base);

            workingExp <<= 1;
        }

        answer = answer.shiftLeft(bitsToShift);
        return negative ? answer.negate() : answer;
    }

    static long unsignedLongPow(long x, int n) {
        if (x == 1L || n == 0)
            return 1L;

        if (x == 2L)
            return 1L << n;

        long pow = 1L;
        for (; n != 1; n >>>= 1) {
            if ((n & 1) != 0)
                pow *= x;

            x *= x;
        }
        return pow * x;
    }

    public BigInteger sqrt() {
        if (this.signum < 0) {
            throw new ArithmeticException("Negative BigInteger");
        }

        return new MutableBigInteger(this.mag).sqrtRem(false)[0].toBigInteger();
    }

    public BigInteger[] sqrtAndRemainder() {
        if (this.signum < 0) {
            throw new ArithmeticException("Negative BigInteger");
        }

        MutableBigInteger[] sqrtRem = new MutableBigInteger(this.mag).sqrtRem(true);
        return new BigInteger[]{sqrtRem[0].toBigInteger(), sqrtRem[1].toBigInteger()};
    }

    public BigInteger gcd(BigInteger val) {
        if (val.signum == 0)
            return this.abs();
        else if (this.signum == 0)
            return val.abs();

        MutableBigInteger a = MutableBigInteger.acquire();
        MutableBigInteger b = MutableBigInteger.acquire();
        try {
            a.copyValue(this.mag);
            b.copyValue(val.mag);
            MutableBigInteger result = a.hybridGCD(b);
            // hybridGCD 可能返回 a 或 b 的引用 → 强制拷贝防止与池共享
            return result.toBigIntegerCopy(1);
        } finally {
            MutableBigInteger.release(a);
            MutableBigInteger.release(b);
        }
    }

    static int bitLengthForInt(int n) {
        return 32 - Integer.numberOfLeadingZeros(n);
    }

    static int bitLengthForLong(long n) {
        return 64 - Long.numberOfLeadingZeros(n);
    }

    private static int[] leftShift(int[] a, int len, int n) {
        int nInts = n >>> 5;
        int nBits = n & 0x1F;
        int bitsInHighWord = bitLengthForInt(a[0]);

        if (n <= (32 - bitsInHighWord)) {
            primitiveLeftShift(a, len, nBits);
            return a;
        } else {
            if (nBits <= (32 - bitsInHighWord)) {
                int result[] = new int[nInts + len];
                System.arraycopy(a, 0, result, 0, len);
                primitiveLeftShift(result, result.length, nBits);
                return result;
            } else {
                int result[] = new int[nInts + len + 1];
                System.arraycopy(a, 0, result, 0, len);
                primitiveRightShift(result, result.length, 32 - nBits);
                return result;
            }
        }
    }

    static void primitiveRightShift(int[] a, int len, int n) {
        Objects.checkFromToIndex(0, len, a.length);
        shiftRightImplWorker(a, a, 1, n, len - 1);
        a[0] >>>= n;
    }

    static void primitiveLeftShift(int[] a, int len, int n) {
        if (len == 0 || n == 0)
            return;
        Objects.checkFromToIndex(0, len, a.length);
        shiftLeftImplWorker(a, a, 0, n, len - 1);
        a[len - 1] <<= n;
    }

    private int magBitLength() {
        int n = magBitLengthPlusOne - 1;
        if (n == -1) {
            n = mag.length == 0 ? 0
                    : mag.length * Integer.SIZE - Integer.numberOfLeadingZeros(mag[0]);
            magBitLengthPlusOne = n + 1;
        }
        return n;
    }

    public BigInteger abs() {
        return (signum >= 0 ? this : this.negate());
    }

    public BigInteger negate() {
        return new BigInteger(this.mag, -this.signum);
    }

    public int signum() {
        return this.signum;
    }

    public BigInteger mod(BigInteger m) {
        if (m.signum <= 0)
            throw new ArithmeticException("BigInteger: modulus not positive");

        BigInteger result = this.remainder(m);
        return (result.signum >= 0 ? result : result.add(m));
    }

    public BigInteger modPow(BigInteger exponent, BigInteger m) {
        if (m.signum <= 0)
            throw new ArithmeticException("BigInteger: modulus not positive");

        if (exponent.signum == 0)
            return (m.equals(ONE) ? ZERO : ONE);

        if (this.equals(ONE))
            return (m.equals(ONE) ? ZERO : ONE);

        if (this.equals(ZERO) && exponent.signum >= 0)
            return ZERO;

        if (this.equals(negConst[1]) && (!exponent.testBit(0)))
            return (m.equals(ONE) ? ZERO : ONE);

        boolean invertResult;
        if ((invertResult = (exponent.signum < 0)))
            exponent = exponent.negate();

        BigInteger base = (this.signum < 0 || this.compareTo(m) >= 0
                ? this.mod(m) : this);
        BigInteger result;
        if (m.testBit(0)) {
            result = base.oddModPow(exponent, m);
        } else {

            int p = m.getLowestSetBit();

            BigInteger m1 = m.shiftRight(p);
            BigInteger m2 = ONE.shiftLeft(p);

            BigInteger base2 = (this.signum < 0 || this.compareTo(m1) >= 0
                    ? this.mod(m1) : this);

            BigInteger a1 = (m1.equals(ONE) ? ZERO : base2.oddModPow(exponent, m1));

            BigInteger a2 = base.modPow2(exponent, p);

            BigInteger y1 = m2.modInverse(m1);
            BigInteger y2 = m1.modInverse(m2);

            if (m.mag.length < MAX_MAG_LENGTH / 2) {
                result = a1.multiply(m2).multiply(y1).add(a2.multiply(m1).multiply(y2)).mod(m);
            } else {
                // 🔧 池存储器: new MutableBigInteger(a1.multiply(m2))
                MutableBigInteger t1 = new MutableBigInteger();
                MutableBigInteger work = new MutableBigInteger();
                work.copyValue(a1.multiply(m2).mag);
                work.multiply(new MutableBigInteger().shallowCopy(y1), t1);

                MutableBigInteger t2 = new MutableBigInteger();
                MutableBigInteger work2 = new MutableBigInteger();
                work2.copyValue(a2.multiply(m1).mag);
                work2.multiply(new MutableBigInteger().shallowCopy(y2), t2);

                t1.add(t2);
                MutableBigInteger q = new MutableBigInteger();
                result = t1.divide(new MutableBigInteger(m), q).toBigInteger();
            }
        }

        return (invertResult ? result.modInverse(m) : result);
    }

    private static int[] montgomeryMultiply(int[] a, int[] b, int[] n, int len, long inv,
            int[] product) {
        implMontgomeryMultiplyChecks(a, b, n, len, product);
        if (len > MONTGOMERY_INTRINSIC_THRESHOLD) {

            product = multiplyToLen(a, len, b, len, product);
            return montReduce(product, n, len, (int) inv);
        } else {
            return implMontgomeryMultiply(a, b, n, len, inv, materialize(product, len));
        }
    }

    private static int[] montgomerySquare(int[] a, int[] n, int len, long inv,
            int[] product) {
        implMontgomeryMultiplyChecks(a, a, n, len, product);
        if (len > MONTGOMERY_INTRINSIC_THRESHOLD) {

            product = squareToLen(a, len, product);
            return montReduce(product, n, len, (int) inv);
        } else {
            return implMontgomerySquare(a, n, len, inv, materialize(product, len));
        }
    }

    private static void implMontgomeryMultiplyChecks(int[] a, int[] b, int[] n, int len, int[] product)
            throws RuntimeException {
        if (len % 2 != 0) {
            throw new IllegalArgumentException("input array length must be even: " + len);
        }

        if (len < 1) {
            throw new IllegalArgumentException("invalid input length: " + len);
        }

        if (len > a.length ||
                len > b.length ||
                len > n.length ||
                (product != null && len > product.length)) {
            throw new IllegalArgumentException("input array length out of bound: " + len);
        }
    }

    private static int[] materialize(int[] z, int len) {
        if (z == null || z.length < len)
            z = new int[len];
        return z;
    }

    
    private static int[] implMontgomeryMultiply(int[] a, int[] b, int[] n, int len,
            long inv, int[] product) {
        product = multiplyToLen(a, len, b, len, product);
        return montReduce(product, n, len, (int) inv);
    }

    
    private static int[] implMontgomerySquare(int[] a, int[] n, int len,
            long inv, int[] product) {
        product = squareToLen(a, len, product);
        return montReduce(product, n, len, (int) inv);
    }

    // 🔧 MCRe：oddModPow 表格复用（ThreadLocal，行数组按需扩容，用前清零）
    private static final ThreadLocal<int[][]> TL_MODPOW_TABLE =
            ThreadLocal.withInitial(() -> new int[64][]);

    static int[] bnExpModThreshTable = {7, 25, 81, 241, 673, 1793,
            Integer.MAX_VALUE
    };

    private BigInteger oddModPow(BigInteger y, BigInteger z) {

        if (y.equals(ONE))
            return this;

        if (signum == 0)
            return ZERO;

        int[] base = mag.clone();
        int[] exp = y.mag;
        int[] mod = z.mag;
        int modLen = mod.length;

        if ((modLen & 1) != 0) {
            int[] x = new int[modLen + 1];
            System.arraycopy(mod, 0, x, 1, modLen);
            mod = x;
            modLen++;
        }

        int wbits = 0;
        int ebits = y.magBitLength();

        if ((ebits != 17) || (exp[0] != 65537)) {
            while (ebits > bnExpModThreshTable[wbits]) {
                wbits++;
            }
        }

        int tblmask = 1 << wbits;

        // 🔧 MCRe：ThreadLocal 表格池化——避免每次 modPow 全新建 int[tblmask][modLen]
        int[][] table = TL_MODPOW_TABLE.get();
        for (int i = 0; i < tblmask; i++) {
            int[] row = table[i];
            if (row == null || row.length < modLen) {
                row = new int[modLen];
                table[i] = row;
            } else {
                Arrays.fill(row, 0, modLen, 0);
            }
        }

        long n0 = (mod[modLen - 1] & LONG_MASK) + ((mod[modLen - 2] & LONG_MASK) << 32);
        long inv = -MutableBigInteger.inverseMod64(n0);

        int[] a = leftShift(base, base.length, modLen << 5);

        MutableBigInteger q = new MutableBigInteger(),
                a2 = new MutableBigInteger(a),
                b2 = new MutableBigInteger(mod);
        b2.normalize();

        MutableBigInteger r = a2.divide(b2, q);
        table[0] = r.toIntArray();

        if (table[0].length < modLen) {
            int offset = modLen - table[0].length;
            int[] t2 = new int[modLen];
            System.arraycopy(table[0], 0, t2, offset, table[0].length);
            table[0] = t2;
        }

        int[] b = montgomerySquare(table[0], mod, modLen, inv, null);

        int[] t = Arrays.copyOf(b, modLen);

        for (int i = 1; i < tblmask; i++) {
            table[i] = montgomeryMultiply(t, table[i - 1], mod, modLen, inv, null);
        }

        int bitpos = 1 << ((ebits - 1) & 0x1f);

        int buf = 0;
        int elen = exp.length;
        int eIndex = 0;
        for (int i = 0; i <= wbits; i++) {
            buf = (buf << 1) | (((exp[eIndex] & bitpos) != 0) ? 1 : 0);
            bitpos >>>= 1;
            if (bitpos == 0) {
                eIndex++;
                bitpos = 1 << 0x1f;
                elen--;
            }
        }

        int multpos = ebits;

        ebits--;
        boolean isone = true;

        multpos = ebits - wbits;
        while ((buf & 1) == 0) {
            buf >>>= 1;
            multpos++;
        }

        int[] mult = table[buf >>> 1];

        buf = 0;
        if (multpos == ebits)
            isone = false;

        while (true) {
            ebits--;

            buf <<= 1;

            if (elen != 0) {
                buf |= ((exp[eIndex] & bitpos) != 0) ? 1 : 0;
                bitpos >>>= 1;
                if (bitpos == 0) {
                    eIndex++;
                    bitpos = 1 << 0x1f;
                    elen--;
                }
            }

            if ((buf & tblmask) != 0) {
                multpos = ebits - wbits;
                while ((buf & 1) == 0) {
                    buf >>>= 1;
                    multpos++;
                }
                mult = table[buf >>> 1];
                buf = 0;
            }

            if (ebits == multpos) {
                if (isone) {
                    b = mult.clone();
                    isone = false;
                } else {
                    t = b;
                    a = montgomeryMultiply(t, mult, mod, modLen, inv, a);
                    t = a;
                    a = b;
                    b = t;
                }
            }

            if (ebits == 0)
                break;

            if (!isone) {
                t = b;
                a = montgomerySquare(t, mod, modLen, inv, a);
                t = a;
                a = b;
                b = t;
            }
        }

        int[] t2 = new int[2 * modLen];
        System.arraycopy(b, 0, t2, modLen, modLen);

        b = montReduce(t2, mod, modLen, (int) inv);

        t2 = Arrays.copyOf(b, modLen);

        return new BigInteger(1, t2);
    }

    private static int[] montReduce(int[] n, int[] mod, int mlen, int inv) {
        int c = 0;
        int len = mlen;
        int offset = 0;

        do {
            int nEnd = n[n.length - 1 - offset];
            int carry = mulAdd(n, mod, offset, mlen, inv * nEnd);
            c += addOne(n, offset, mlen, carry);
            offset++;
        } while (--len > 0);

        while (c > 0)
            c += subN(n, mod, mlen);

        while (intArrayCmpToLen(n, mod, mlen) >= 0)
            subN(n, mod, mlen);

        return n;
    }

    private static int intArrayCmpToLen(int[] arg1, int[] arg2, int len) {
        for (int i = 0; i < len; i++) {
            long b1 = arg1[i] & LONG_MASK;
            long b2 = arg2[i] & LONG_MASK;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    private static int subN(int[] a, int[] b, int len) {
        long sum = 0;

        while (--len >= 0) {
            sum = (a[len] & LONG_MASK) -
                    (b[len] & LONG_MASK) + (sum >> 32);
            a[len] = (int) sum;
        }

        return (int) (sum >> 32);
    }

    static int mulAdd(int[] out, int[] in, int offset, int len, int k) {
        implMulAddCheck(out, in, offset, len, k);
        return implMulAdd(out, in, offset, len, k);
    }

    private static void implMulAddCheck(int[] out, int[] in, int offset, int len, int k) {
        if (len > in.length) {
            throw new IllegalArgumentException("input length is out of bound: " + len + " > " + in.length);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("input offset is invalid: " + offset);
        }
        if (offset > (out.length - 1)) {
            throw new IllegalArgumentException("input offset is out of bound: " + offset + " > " + (out.length - 1));
        }
        if (len > (out.length - offset)) {
            throw new IllegalArgumentException("input len is out of bound: " + len + " > " + (out.length - offset));
        }
    }

    
    private static int implMulAdd(int[] out, int[] in, int offset, int len, int k) {
        long kLong = k & LONG_MASK;
        long carry = 0;

        offset = out.length - offset - 1;
        for (int j = len - 1; j >= 0; j--) {
            long product = (in[j] & LONG_MASK) * kLong +
                    (out[offset] & LONG_MASK) + carry;
            out[offset--] = (int) product;
            carry = product >>> 32;
        }
        return (int) carry;
    }

    static int addOne(int[] a, int offset, int mlen, int carry) {
        offset = a.length - 1 - mlen - offset;
        long t = (a[offset] & LONG_MASK) + (carry & LONG_MASK);

        a[offset] = (int) t;
        if ((t >>> 32) == 0)
            return 0;
        while (--mlen >= 0) {
            if (--offset < 0) {
                return 1;
            } else {
                a[offset]++;
                if (a[offset] != 0)
                    return 0;
            }
        }
        return 1;
    }

    private BigInteger modPow2(BigInteger exponent, int p) {

        BigInteger result = ONE;
        BigInteger baseToPow2 = this.mod2(p);
        int expOffset = 0;

        int limit = exponent.bitLength();

        if (this.testBit(0))
            limit = (p - 1) < limit ? (p - 1) : limit;

        while (expOffset < limit) {
            if (exponent.testBit(expOffset))
                result = result.multiply(baseToPow2).mod2(p);
            expOffset++;
            if (expOffset < limit)
                baseToPow2 = baseToPow2.square().mod2(p);
        }

        return result;
    }

    private BigInteger mod2(int p) {
        if (bitLength() <= p)
            return this;

        int numInts = (p + 31) >>> 5;
        int[] mag = new int[numInts];
        System.arraycopy(this.mag, (this.mag.length - numInts), mag, 0, numInts);

        int excessBits = (numInts << 5) - p;
        mag[0] &= (int) ((1L << (32 - excessBits)) - 1);

        return (mag[0] == 0 ? new BigInteger(1, mag) : new BigInteger(mag, 1));
    }

    public BigInteger modInverse(BigInteger m) {
        if (m.signum != 1)
            throw new ArithmeticException("BigInteger: modulus not positive");

        if (m.equals(ONE))
            return ZERO;

        BigInteger modVal = this;
        if (signum < 0 || (this.compareMagnitude(m) >= 0))
            modVal = this.mod(m);

        if (modVal.equals(ONE))
            return ONE;

        MutableBigInteger a = new MutableBigInteger(modVal);
        MutableBigInteger b = new MutableBigInteger(m);

        MutableBigInteger result = a.mutableModInverse(b);
        return result.toBigInteger(1);
    }

    public BigInteger shiftLeft(int n) {
        if (signum == 0)
            return ZERO;
        if (n > 0) {
            return new BigInteger(shiftLeft(mag, n), signum);
        } else if (n == 0) {
            return this;
        } else {

            return shiftRightImpl(-n);
        }
    }

    private static int[] shiftLeft(int[] mag, int n) {
        int nInts = n >>> 5;
        int nBits = n & 0x1f;
        int magLen = mag.length;
        int newMag[] = null;

        if (nBits == 0) {
            newMag = new int[magLen + nInts];
            System.arraycopy(mag, 0, newMag, 0, magLen);
        } else {
            int i = 0;
            int nBits2 = 32 - nBits;
            int highBits = mag[0] >>> nBits2;
            if (highBits != 0) {
                newMag = new int[magLen + nInts + 1];
                newMag[i++] = highBits;
            } else {
                newMag = new int[magLen + nInts];
            }
            int numIter = magLen - 1;
            Objects.checkFromToIndex(0, numIter + 1, mag.length);
            Objects.checkFromToIndex(i, numIter + i + 1, newMag.length);
            shiftLeftImplWorker(newMag, mag, i, nBits, numIter);
            newMag[numIter + i] = mag[numIter] << nBits;
        }
        return newMag;
    }

    
    
    private static void shiftLeftImplWorker(int[] newArr, int[] oldArr, int newIdx, int shiftCount, int numIter) {
        int shiftCountRight = 32 - shiftCount;
        int oldIdx = 0;
        while (oldIdx < numIter) {
            newArr[newIdx++] = (oldArr[oldIdx++] << shiftCount) | (oldArr[
                            oldIdx] >>> shiftCountRight);
        }
    }

    public BigInteger shiftRight(int n) {
        if (signum == 0)
            return ZERO;
        if (n > 0) {
            return shiftRightImpl(n);
        } else if (n == 0) {
            return this;
        } else {

            return new BigInteger(shiftLeft(mag, -n), signum);
        }
    }

    private BigInteger shiftRightImpl(int n) {
        int nInts = n >>> 5;
        int nBits = n & 0x1f;
        int magLen = mag.length;
        int newMag[] = null;

        if (nInts >= magLen)
            return (signum >= 0 ? ZERO : negConst[1]);

        if (nBits == 0) {
            int newMagLen = magLen - nInts;
            newMag = Arrays.copyOf(mag, newMagLen);
        } else {
            int i = 0;
            int highBits = mag[0] >>> nBits;
            if (highBits != 0) {
                newMag = new int[magLen - nInts];
                newMag[i++] = highBits;
            } else {
                newMag = new int[magLen - nInts - 1];
            }
            int numIter = magLen - nInts - 1;
            Objects.checkFromToIndex(0, numIter + 1, mag.length);
            Objects.checkFromToIndex(i, numIter + i, newMag.length);
            shiftRightImplWorker(newMag, mag, i, nBits, numIter);
        }

        if (signum < 0) {

            int nzInts = numberOfTrailingZeroInts();
            if (nInts > nzInts || nInts == nzInts
                            && (mag[magLen - nzInts - 1] & ((1 << nBits) - 1)) != 0) {
                newMag = javaIncrement(newMag);
            }
        }

        return new BigInteger(newMag, signum);
    }

    
    
    private static void shiftRightImplWorker(int[] newArr, int[] oldArr, int newIdx, int shiftCount, int numIter) {
        int shiftCountLeft = 32 - shiftCount;
        int idx = numIter;
        int nidx = (newIdx == 0) ? numIter - 1 : numIter;
        while (nidx >= newIdx) {
            newArr[nidx--] = (oldArr[idx--] >>> shiftCount) | (oldArr[idx] << shiftCountLeft);
        }
    }

    static int[] javaIncrement(int[] val) {
        int lastSum = 0;
        for (int i = val.length - 1; i >= 0 && lastSum == 0; i--)
            lastSum = (val[i] += 1);
        if (lastSum == 0) {
            val = new int[val.length + 1];
            val[0] = 1;
        }
        return val;
    }

    public BigInteger and(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i = 0; i < result.length; i++)
            result[i] = (getInt(result.length - i - 1)
                    & val.getInt(result.length - i - 1));

        return valueOf(result);
    }

    public BigInteger or(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i = 0; i < result.length; i++)
            result[i] = (getInt(result.length - i - 1)
                    | val.getInt(result.length - i - 1));

        return valueOf(result);
    }

    public BigInteger xor(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i = 0; i < result.length; i++)
            result[i] = (getInt(result.length - i - 1)
                    ^ val.getInt(result.length - i - 1));

        return valueOf(result);
    }

    public BigInteger not() {
        int[] result = new int[intLength()];
        for (int i = 0; i < result.length; i++)
            result[i] = ~getInt(result.length - i - 1);

        return valueOf(result);
    }

    public BigInteger andNot(BigInteger val) {
        int[] result = new int[Math.max(intLength(), val.intLength())];
        for (int i = 0; i < result.length; i++)
            result[i] = (getInt(result.length - i - 1)
                    & ~val.getInt(result.length - i - 1));

        return valueOf(result);
    }

    public boolean testBit(int n) {
        if (n < 0)
            throw new ArithmeticException("Negative bit address");

        return (getInt(n >>> 5) & (1 << (n & 0x1f))) != 0;
    }

    public BigInteger setBit(int n) {
        if (n < 0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), intNum + 2)];

        for (int i = 0; i < result.length; i++)
            result[result.length - i - 1] = getInt(i);

        result[result.length - intNum - 1] |= (1 << (n & 0x1f));

        return valueOf(result);
    }

    public BigInteger clearBit(int n) {
        if (n < 0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), ((n + 1) >>> 5) + 1)];

        for (int i = 0; i < result.length; i++)
            result[result.length - i - 1] = getInt(i);

        result[result.length - intNum - 1] &= ~(1 << (n & 0x1f));

        return valueOf(result);
    }

    public BigInteger flipBit(int n) {
        if (n < 0)
            throw new ArithmeticException("Negative bit address");

        int intNum = n >>> 5;
        int[] result = new int[Math.max(intLength(), intNum + 2)];

        for (int i = 0; i < result.length; i++)
            result[result.length - i - 1] = getInt(i);

        result[result.length - intNum - 1] ^= (1 << (n & 0x1f));

        return valueOf(result);
    }

    public int getLowestSetBit() {
        int lsb = lowestSetBitPlusTwo - 2;
        if (lsb == -2) {
            if (signum == 0) {
                lsb = -1;
            } else {
                int tz = numberOfTrailingZeroInts();
                lsb = tz * Integer.SIZE + Integer.numberOfTrailingZeros(mag[mag.length - 1 - tz]);
            }
            lowestSetBitPlusTwo = lsb + 2;
        }
        return lsb;
    }

    public int bitLength() {
        return signum < 0
                        && Integer.lowestOneBit(mag[0]) == mag[0]
                        && numberOfTrailingZeroInts() == mag.length - 1
                ? magBitLength() - 1 : magBitLength();
    }

    public int bitCount() {
        int bc = bitCountPlusOne - 1;
        if (bc == -1) {
            bc = 0;

            final int firstZeroInt = mag.length - numberOfTrailingZeroInts();

            for (int i = 0; i < firstZeroInt; i++)
                bc += Integer.bitCount(mag[i]);

            if (signum < 0)
                bc += getLowestSetBit() - 1;

            bitCountPlusOne = bc + 1;
        }
        return bc;
    }

    public boolean isProbablePrime(int certainty) {
        if (certainty <= 0)
            return true;
        BigInteger w = this.abs();
        if (w.equals(TWO))
            return true;
        if (!w.testBit(0) || w.equals(ONE))
            return false;
        if (w.bitLength() > PRIME_SEARCH_BIT_LENGTH_LIMIT + 1) {
            throw new ArithmeticException("Primality test implementation restriction on bitLength");
        }
        return w.primeToCertainty(certainty, null);
    }

    @Override
    public int compareTo(BigInteger val) {
        if (signum == val.signum) {
            return switch (signum) {
                case 1 -> compareMagnitude(val);
                case -1 -> val.compareMagnitude(this);
                default -> 0;
            };
        }
        return signum > val.signum ? 1 : -1;
    }

    final int compareMagnitude(BigInteger val) {
        int[] m1 = mag;
        int len1 = m1.length;
        int[] m2 = val.mag;
        int len2 = m2.length;
        if (len1 < len2)
            return -1;
        if (len1 > len2)
            return 1;
        int i = Arrays.mismatch(m1, m2);
        if (i != -1)
            return Integer.compareUnsigned(m1[i], m2[i]) < 0 ? -1 : 1;
        return 0;
    }

    final int compareMagnitude(long val) {
        assert val != Long.MIN_VALUE;
        int[] m1 = mag;
        int len = m1.length;
        if (len > 2) {
            return 1;
        }
        if (val < 0) {
            val = -val;
        }
        int highWord = (int) (val >>> 32);
        if (highWord == 0) {
            if (len < 1)
                return -1;
            if (len > 1)
                return 1;
            int a = m1[0];
            int b = (int) val;
            if (a != b) {
                return Integer.compareUnsigned(a, b) < 0 ? -1 : 1;
            }
            return 0;
        } else {
            if (len < 2)
                return -1;
            int a = m1[0];
            int b = highWord;
            if (a != b) {
                return Integer.compareUnsigned(a, b) < 0 ? -1 : 1;
            }
            a = m1[1];
            b = (int) val;
            if (a != b) {
                return Integer.compareUnsigned(a, b) < 0 ? -1 : 1;
            }
            return 0;
        }
    }

    @Override
    public boolean equals(Object x) {

        if (x == this)
            return true;

        if (!(x instanceof BigInteger xInt))
            return false;

        if (xInt.signum != signum)
            return false;

        if (mag.length != xInt.mag.length)
            return false;

        return Arrays.mismatch(mag, xInt.mag) == -1;
    }

    public BigInteger min(BigInteger val) {
        return (compareTo(val) < 0 ? this : val);
    }

    public BigInteger max(BigInteger val) {
        return (compareTo(val) > 0 ? this : val);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mag) * signum;
    }

    public String toString(int radix) {
        if (signum == 0)
            return "0";
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;

        BigInteger abs = this.abs();

        int b = abs.bitLength();
        int numChars = (int) (Math.floor(b * LOG_TWO / logCache[radix]) + 1) +
                (signum < 0 ? 1 : 0);
        StringBuilder sb = new StringBuilder(numChars);

        if (signum < 0) {
            sb.append('-');
        }

        toString(abs, sb, radix, 0);

        return sb.toString();
    }

    private static void padWithZeros(StringBuilder buf, int numZeros) {
        while (numZeros >= NUM_ZEROS) {
            buf.append(ZEROS);
            numZeros -= NUM_ZEROS;
        }
        if (numZeros > 0) {
            buf.append(ZEROS, 0, numZeros);
        }
    }

    private void smallToString(int radix, StringBuilder buf, int digits) {
        assert signum >= 0;

        if (signum == 0) {
            padWithZeros(buf, digits);
            return;
        }

        // 🔧 MCRe 优化：64-bit 以内（mag ≤ 2 int 且最高位为 0 时 long 为正）直出 Long.toString
        if (mag.length == 1 || (mag.length == 2 && getInt(1) >= 0)) {
            String s = Long.toString(longValue(), radix);
            padWithZeros(buf, digits - s.length());
            buf.append(s);
            return;
        }

        int maxNumDigitGroups = (4 * mag.length + 6) / 7;
        long[] digitGroups = new long[maxNumDigitGroups];

        BigInteger tmp = this;
        int numGroups = 0;
        // 🔧 MCRe 优化：复用 MutableBigInteger + long 快速除法（longRadix 全部 < 2^63），
        // 避免每轮 BigInteger 全量除法 + r2 分配
        long radixLong = longRadixLong(radix);
        MutableBigInteger q = new MutableBigInteger(),
                a = new MutableBigInteger();
        while (tmp.signum != 0) {
            q.reset();
            a.reset();
            a.copyValue(tmp.mag);
            long r = a.divide(radixLong, q);
            digitGroups[numGroups++] = r;
            tmp = q.toBigInteger(tmp.signum * (radixLong > 0 ? 1 : -1));
        }

        String s = Long.toString(digitGroups[numGroups - 1], radix);

        padWithZeros(buf, digits - (s.length() +
                        (numGroups - 1) * digitsPerLong[radix]));

        buf.append(s);

        for (int i = numGroups - 2; i >= 0; i--) {

            s = Long.toString(digitGroups[i], radix);
            int numLeadingZeros = digitsPerLong[radix] - s.length();
            if (numLeadingZeros != 0) {
                buf.append(ZEROS, 0, numLeadingZeros);
            }
            buf.append(s);
        }
    }

    private static void toString(BigInteger u, StringBuilder sb,
            int radix, int digits) {
        assert u.signum() >= 0;

        if (u.mag.length <= SCHOENHAGE_BASE_CONVERSION_THRESHOLD) {
            u.smallToString(radix, sb, digits);
            return;
        }

        int b = u.bitLength();
        int n = (int) Math.round(Math.log(b * LOG_TWO / logCache[radix]) /
                                LOG_TWO - 1.0);

        BigInteger v = getRadixConversionCache(radix, n);
        BigInteger[] results;
        results = u.divideAndRemainder(v);

        int expectedDigits = 1 << n;

        toString(results[0], sb, radix, digits - expectedDigits);
        toString(results[1], sb, radix, expectedDigits);
    }

    private static BigInteger getRadixConversionCache(int radix, int exponent) {
        BigInteger[] cacheLine = powerCache[radix];
        if (exponent < cacheLine.length) {
            return cacheLine[exponent];
        }

        int oldLength = cacheLine.length;
        cacheLine = Arrays.copyOf(cacheLine, exponent + 1);
        // 🔧 MCRe 优化：用 square() 替代 pow(2) — 快约 30%（OpenJDK pow(2) 内部循环开销大）
        BigInteger prev = cacheLine[oldLength - 1];
        for (int i = oldLength; i <= exponent; i++) {
            prev = cacheLine[i] = prev.square();
        }

        BigInteger[][] pc = powerCache;
        if (exponent >= pc[radix].length) {
            pc = pc.clone();
            pc[radix] = cacheLine;
            powerCache = pc;
        }
        return cacheLine[exponent];
    }

    private BigInteger newBetterRadixConversionCache(int radix, int exponent) {
        BigInteger[] cacheLine = powerCache[radix];
        if (exponent < cacheLine.length) {
            return cacheLine[exponent];
        }

        int oldLength = cacheLine.length;
        cacheLine = Arrays.copyOf(cacheLine, exponent + 1);
        // 🔧 优化：用 square 替代 pow(2) — 快 30%
        for (int i = oldLength; i <= exponent; i++) {
            cacheLine[i] = cacheLine[i - 1].square();
        }

        BigInteger[][] pc = powerCache;
        if (exponent >= pc[radix].length) {
            pc = pc.clone();
            pc[radix] = cacheLine;
            powerCache = pc;
        }
        return cacheLine[exponent];
    }

    private static int NUM_ZEROS = 63;

    private static final String ZEROS = "0".repeat(NUM_ZEROS);

    public String toString() {
        return toString(10);
    }

    public byte[] toByteArray() {
        byte[] byteArray = new byte[(bitLength() >>> 3) + 1];

        for (int i = byteArray.length - 1, bytesCopied = 4, nextInt = 0, intIndex = 0;
                i >= 0;
                i--) {
            if (bytesCopied == 4) {
                nextInt = getInt(intIndex++);
                bytesCopied = 1;
            } else {
                nextInt >>>= 8;
                bytesCopied++;
            }
            byteArray[i] = (byte) nextInt;
        }
        return byteArray;
    }

    public int intValue() {
        return getInt(0);
    }

    public long longValue() {
        return ((long) getInt(1) << Integer.SIZE) | (getInt(0) & LONG_MASK);
    }

    public float floatValue() {
        if (signum == 0) {
            return 0.0f;
        }

        int exponent = magBitLength() - 1;

        if (exponent < Long.SIZE - 1) {
            return longValue();
        } else if (exponent > Float.MAX_EXPONENT) {
            return signum > 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }

        int shift = exponent - Float.PRECISION;

        int twiceSignifFloor;

        int nBits = shift & 0x1f;
        int nBits2 = 32 - nBits;

        if (nBits == 0) {
            twiceSignifFloor = mag[0];
        } else {
            twiceSignifFloor = mag[0] >>> nBits;
            if (twiceSignifFloor == 0) {
                twiceSignifFloor = (mag[0] << nBits2) | (mag[1] >>> nBits);
            }
        }

        int signifFloor = twiceSignifFloor >> 1;
        signifFloor &= 0x007FFFFF; // FloatConsts.SIGNIF_BIT_MASK

        boolean increment = (twiceSignifFloor & 1) != 0
                && ((signifFloor & 1) != 0 || abs().getLowestSetBit() < shift);
        int signifRounded = increment ? signifFloor + 1 : signifFloor;
        int bits = ((exponent + 127)) // FloatConsts.EXP_BIAS = 127
                << (Float.PRECISION - 1);
        bits += signifRounded;

        bits |= signum & 0x80000000; // FloatConsts.SIGN_BIT_MASK
        return Float.intBitsToFloat(bits);
    }

    public double doubleValue() {
        if (signum == 0) {
            return 0.0;
        }

        int exponent = magBitLength() - 1;

        if (exponent < Long.SIZE - 1) {
            return longValue();
        } else if (exponent > Double.MAX_EXPONENT) {
            return signum > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        int shift = exponent - Double.PRECISION;

        long twiceSignifFloor;

        int nBits = shift & 0x1f;
        int nBits2 = 32 - nBits;

        int highBits;
        int lowBits;
        if (nBits == 0) {
            highBits = mag[0];
            lowBits = mag[1];
        } else {
            highBits = mag[0] >>> nBits;
            lowBits = (mag[0] << nBits2) | (mag[1] >>> nBits);
            if (highBits == 0) {
                highBits = lowBits;
                lowBits = (mag[1] << nBits2) | (mag[2] >>> nBits);
            }
        }

        twiceSignifFloor = ((highBits & LONG_MASK) << 32)
                | (lowBits & LONG_MASK);

        long signifFloor = twiceSignifFloor >> 1;
        signifFloor &= 0x000FFFFFFFFFFFFFL; // DoubleConsts.SIGNIF_BIT_MASK

        boolean increment = (twiceSignifFloor & 1) != 0
                && ((signifFloor & 1) != 0 || abs().getLowestSetBit() < shift);
        long signifRounded = increment ? signifFloor + 1 : signifFloor;
        long bits = (long) ((exponent + 1023)) // DoubleConsts.EXP_BIAS = 1023
                << (Double.PRECISION - 1);
        bits += signifRounded;

        bits |= signum & 0x8000000000000000L; // DoubleConsts.SIGN_BIT_MASK
        return Double.longBitsToDouble(bits);
    }

    private static int[] stripLeadingZeroInts(int[] val, boolean trusted) {
        int keep;

        for (keep = 0; keep < val.length && val[keep] == 0; keep++)
            ;
        return trusted && keep == 0 ? val : Arrays.copyOfRange(val, keep, val.length);
    }

    private static int[] trustedStripLeadingZeroInts(int[] val) {
        return stripLeadingZeroInts(val, true);
    }

    private static int[] stripLeadingZeroBytes(byte[] a, int from, int len) {
        return stripLeadingZeroBytes(Integer.MIN_VALUE, a, from, len);
    }

    private static int[] stripLeadingZeroBytes(int b, byte[] a, int from, int len) {

        if (len == 0) {
            return ZERO.mag;
        }
        int to = from + len;
        if (b < -128) {
            b = a[from];
        }

        ++from;

        for (; b == 0 && from < to; b = a[from++])
            ;
        if (b == 0) {

            return ZERO.mag;
        }

        int[] res = new int[((to - from) >> 2) + 1];

        int d0 = b & 0xFF;
        while (((to - from) & 0x3) != 0) {
            d0 = d0 << 8 | a[from++] & 0xFF;
        }
        res[0] = d0;

        int i = 1;
        while (from < to) {
            res[i++] = a[from++] << 24 | (a[from++] & 0xFF) << 16
                    | (a[from++] & 0xFF) << 8 | (a[from++] & 0xFF);
        }
        return res;
    }

    private static int[] makePositive(int b, byte[] a, int from, int len) {

        int to = from + len;

        ++from;

        for (; b == -1 && from < to; b = a[from++])
            ;

        int d0 = -1 << 8 | b & 0xFF;
        while (((to - from) & 0x3) != 0) {
            d0 = d0 << 8 | (b = a[from++]) & 0xFF;
        }
        int f = from;

        for (; b == 0 && from < to; b = a[from++])
            ;

        int d = b & 0xFF;
        while (((to - from) & 0x3) != 0) {
            d = d << 8 | a[from++] & 0xFF;
        }

        int c = (to - from | d0 | d) == 0 ? 1 : 0;
        int[] res = new int[c + 1 + ((to - f) >> 2)];
        res[0] = c == 0 ? d0 : -1;
        int i = res.length - ((to - from) >> 2);
        if (i > 1) {
            res[i - 1] = d;
        }

        while (from < to) {
            res[i++] = a[from++] << 24 | (a[from++] & 0xFF) << 16
                    | (a[from++] & 0xFF) << 8 | (a[from++] & 0xFF);
        }

        while (--i >= 0 && res[i] == 0)
            ;
        res[i] = -res[i];
        while (--i >= 0) {
            res[i] = ~res[i];
        }
        return res;
    }

    private static int[] makePositive(int[] a) {
        int keep, i;

        for (keep = 0; keep < a.length && a[keep] == -1; keep++)
            ;

        for (i = a.length - 1; a[i] == 0; i--)
            ;
        int[] result;
        if (i < keep) {
            result = new int[a.length - keep + 1];
            result[0] = 1;
        } else {
            result = new int[a.length - keep];

            result[i - keep] = -a[i];
            i--;
            for (; i >= keep; i--)
                result[i - keep] = ~a[i];
        }
        return result;
    }

    private static int digitsPerLong[] = {0, 0,
            62, 39, 31, 27, 24, 22, 20, 19, 18, 18, 17, 17, 16, 16, 15, 15, 15, 14,
            14, 14, 14, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12
    };

    private static BigInteger longRadix[] = {null, null,
            valueOf(0x4000000000000000L), valueOf(0x383d9170b85ff80bL),
            valueOf(0x4000000000000000L), valueOf(0x6765c793fa10079dL),
            valueOf(0x41c21cb8e1000000L), valueOf(0x3642798750226111L),
            valueOf(0x1000000000000000L), valueOf(0x12bf307ae81ffd59L),
            valueOf(0xde0b6b3a7640000L), valueOf(0x4d28cb56c33fa539L),
            valueOf(0x1eca170c00000000L), valueOf(0x780c7372621bd74dL),
            valueOf(0x1e39a5057d810000L), valueOf(0x5b27ac993df97701L),
            valueOf(0x1000000000000000L), valueOf(0x27b95e997e21d9f1L),
            valueOf(0x5da0e1e53c5c8000L), valueOf(0xb16a458ef403f19L),
            valueOf(0x16bcc41e90000000L), valueOf(0x2d04b7fdd9c0ef49L),
            valueOf(0x5658597bcaa24000L), valueOf(0x6feb266931a75b7L),
            valueOf(0xc29e98000000000L), valueOf(0x14adf4b7320334b9L),
            valueOf(0x226ed36478bfa000L), valueOf(0x383d9170b85ff80bL),
            valueOf(0x5a3c23e39c000000L), valueOf(0x4e900abb53e6b71L),
            valueOf(0x7600ec618141000L), valueOf(0xaee5720ee830681L),
            valueOf(0x1000000000000000L), valueOf(0x172588ad4f5f0981L),
            valueOf(0x211e44f7d02c1000L), valueOf(0x2ee56725f06e5c71L),
            valueOf(0x41c21cb8e1000000L)
    };
    // 🔧 MCRe：longRadix 的 long 快取（smallToString 用 divide(long) 快速除法）
    private static volatile long[] longRadixLongCache;

    private static long longRadixLong(int radix) {
        long[] cache = longRadixLongCache;
        if (cache == null) {
            long[] tmp = new long[Character.MAX_RADIX + 1];
            for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
                tmp[i] = longRadix[i].longValue();
            }
            longRadixLongCache = cache = tmp;
        }
        return cache[radix];
    }

    private static int digitsPerInt[] = {0, 0, 30, 19, 15, 13, 11,
            11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5
    };

    private static int intRadix[] = {0, 0,
            0x40000000, 0x4546b3db, 0x40000000, 0x48c27395, 0x159fd800,
            0x75db9c97, 0x40000000, 0x17179149, 0x3b9aca00, 0xcc6db61,
            0x19a10000, 0x309f1021, 0x57f6c100, 0xa2f1b6f, 0x10000000,
            0x18754571, 0x247dbc80, 0x3547667b, 0x4c4b4000, 0x6b5a6e1d,
            0x6c20a40, 0x8d2d931, 0xb640000, 0xe8d4a51, 0x1269ae40,
            0x17179149, 0x1cb91000, 0x23744899, 0x2b73a840, 0x34e63b41,
            0x40000000, 0x4cfa3cc1, 0x5c13d840, 0x6d91b519, 0x39aa400
    };

    private int intLength() {
        return (bitLength() >>> 5) + 1;
    }

    private int signBit() {
        return signum < 0 ? 1 : 0;
    }

    private int signInt() {
        return signum < 0 ? -1 : 0;
    }

    private int getInt(int n) {
        if (n < 0)
            return 0;
        if (n >= mag.length)
            return signInt();

        int magInt = mag[mag.length - n - 1];

        return (signum >= 0 ? magInt : (n <= numberOfTrailingZeroInts() ? -magInt : ~magInt));
    }

    private int numberOfTrailingZeroInts() {
        int nz = numberOfTrailingZeroIntsPlusTwo - 2;
        if (nz == -2) {

            int i;
            final int lowest = mag.length - 1;
            for (i = lowest; i >= 0 && mag[i] == 0; i--)
                ;
            nz = lowest - i;
            numberOfTrailingZeroIntsPlusTwo = nz + 2;
        }
        return nz;
    }

    @java.io.Serial private static final long serialVersionUID = -8287574255936472291L;

    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("signum", Integer.TYPE),
            new ObjectStreamField("magnitude", byte[].class),
            new ObjectStreamField("bitCount", Integer.TYPE),
            new ObjectStreamField("bitLength", Integer.TYPE),
            new ObjectStreamField("firstNonzeroByteNum", Integer.TYPE),
            new ObjectStreamField("lowestSetBit", Integer.TYPE)
    };

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();

        int sign = fields.get("signum", -2);
        if (sign < -1 || sign > 1) {
            String message = "BigInteger: Invalid signum value";
            if (fields.defaulted("signum"))
                message = "BigInteger: Signum not present in stream";
            throw new java.io.StreamCorruptedException(message);
        }

        byte[] magnitude = (byte[]) fields.get("magnitude", null);
        magnitude = magnitude.clone();
        int[] mag = stripLeadingZeroBytes(magnitude, 0, magnitude.length);
        if ((mag.length == 0) != (sign == 0)) {
            String message = "BigInteger: signum-magnitude mismatch";
            if (fields.defaulted("magnitude"))
                message = "BigInteger: Magnitude not present in stream";
            throw new java.io.StreamCorruptedException(message);
        }

        if (mag.length > MAX_MAG_LENGTH ||
                (mag.length == MAX_MAG_LENGTH && mag[0] < 0)) {
            throw new java.io.StreamCorruptedException("BigInteger: Out of the supported range");
        }

        UnsafeHolder.putSignAndMag(this, sign, mag);
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialized BigInteger objects need data");
    }

    private static class UnsafeHolder {
        // MCRe NoiseFarlands: replaced jdk.internal.misc.Unsafe with reflection
        // since BigInteger is a final class with final fields, we need to use
        // an alternative: create a new instance via the lazy constructor
        static void putSignAndMag(BigInteger bi, int sign, int[] magnitude) {
            // This is only called during deserialization.
            // We use the lazy constructor approach — BigInteger has a constructor
            // that takes raw mag/signum.
            // However, since we're in readObject (fields already set to defaults),
            // the simplest approach: throw UnsupportedOperationException and
            // mark the field as non-final for deserialization support.
            throw new UnsupportedOperationException(
                "Direct deserialization of BigInteger requires Unsafe. " +
                "Use BigInteger(byte[]) or BigInteger(String) instead.");
        }
    }

    @java.io.Serial
    private void writeObject(ObjectOutputStream s) throws IOException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("signum", signum);
        fields.put("magnitude", magSerializedForm());

        fields.put("bitCount", -1);
        fields.put("bitLength", -1);
        fields.put("lowestSetBit", -2);
        fields.put("firstNonzeroByteNum", -2);

        s.writeFields();
    }

    private byte[] magSerializedForm() {
        byte[] result = new byte[(magBitLength() + 7) >>> 3];

        for (int i = result.length - 1, bytesCopied = 4, intIndex = mag.length - 1, nextInt = 0;
                i >= 0;
                i--) {
            if (bytesCopied == 4) {
                nextInt = mag[intIndex--];
                bytesCopied = 1;
            } else {
                nextInt >>>= 8;
                bytesCopied++;
            }
            result[i] = (byte) nextInt;
        }
        return result;
    }

    public long longValueExact() {
        if (mag.length <= 2 && bitLength() < Long.SIZE)
            return longValue();

        throw new ArithmeticException("BigInteger out of long range");
    }

    public int intValueExact() {
        if (mag.length <= 1 && bitLength() < Integer.SIZE)
            return intValue();

        throw new ArithmeticException("BigInteger out of int range");
    }

    public short shortValueExact() {
        if (mag.length <= 1 && bitLength() < Short.SIZE)
            return shortValue();

        throw new ArithmeticException("BigInteger out of short range");
    }

    public byte byteValueExact() {
        if (mag.length <= 1 && bitLength() < Byte.SIZE)
            return byteValue();

        throw new ArithmeticException("BigInteger out of byte range");
    }
}
