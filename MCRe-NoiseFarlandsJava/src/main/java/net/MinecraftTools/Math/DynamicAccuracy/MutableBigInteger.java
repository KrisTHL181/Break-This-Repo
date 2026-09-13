package net.MinecraftTools.Math.DynamicAccuracy;

import static net.MinecraftTools.Math.DynamicAccuracy.BigDecimal.INFLATED;
import static net.MinecraftTools.Math.DynamicAccuracy.BigInteger.LONG_MASK;
import java.util.Arrays;

class MutableBigInteger {

    int[] value;

    int intLen;

    int offset = 0;

    static final MutableBigInteger ONE = new MutableBigInteger(1);

    // ──────────────────────────────────────────────
// 🔧 MCRe NoiseFarlands 性能优化
// 混合方案：浅拷贝构造 + modInverse/euclidModInverse 池化
// ──────────────────────────────────────────────

/** 🔧 浅拷贝构造：直接引用源数组 + offset/length 避免 Arrays.copyOf，只适用于临时使用且源不修改的场景 */
    MutableBigInteger shallowCopy(MutableBigInteger src) {
        this.value = src.value;
        this.offset = src.offset;
        this.intLen = src.intLen;
        return this;
    }

/** 🔧 快速构造：从 BigInteger 浅引用 mag 数组 调用者必须保证不修改 b */
    MutableBigInteger shallowCopy(BigInteger b) {
        this.value = b.mag;
        this.offset = 0;
        this.intLen = b.mag.length;
        return this;
    }

/** 🔧 快速初始化：从 int 值，复用已有 value 数组 */
    MutableBigInteger fastInit(int val) {
        if (value == null || value.length < 1)
            value = new int[1];
        value[0] = val;
        offset = 0;
        intLen = (val != 0) ? 1 : 0;
        return this;
    }

// ────────────── ThreadLocal 池（仅用于 modInverse 热路径）──────────────
    private static final ThreadLocal<
            MutableBigInteger> TL_TEMP_A = ThreadLocal.withInitial(MutableBigInteger::new);
    private static final ThreadLocal<
            MutableBigInteger> TL_TEMP_B = ThreadLocal.withInitial(MutableBigInteger::new);
    private static final ThreadLocal<
            MutableBigInteger> TL_TEMP_C = ThreadLocal.withInitial(MutableBigInteger::new);

    // ══════════════════════════════════════════════════════════════════
    // 🔧 MCRe NoiseFarlands：通用 ThreadLocal 借用池
    // 用法：BigInteger.divideKnuth / remainderKnuth / divideAndRemainderKnuth / gcd
    //       等“单线程、非重入”热路径借取临时 MutableBigInteger，用完 release 归还。
    // ⚠️ 安全约束：
    //  1. acquire/release 必须成对，且不跨递归（嵌套借用会自动分配新实例，不会互相踩踏）。
    //  2. 归还前不得再持有该实例内部数组的任何引用（用 toBigIntegerCopy 强制拷贝产出）。
    //  3. release 只复位 offset/intLen，不清数组内容（下轮 copyValue 覆盖）。
    // ══════════════════════════════════════════════════════════════════
    private static final int TL_POOL_CAPACITY = 4;

    private static final ThreadLocal<MutableBigInteger[]> TL_POOL =
            ThreadLocal.withInitial(() -> new MutableBigInteger[TL_POOL_CAPACITY]);
    private static final ThreadLocal<int[]> TL_POOL_SIZE = ThreadLocal.withInitial(() -> new int[1]);

    /** 🔧 借取一个干净的 MutableBigInteger（线程局部；嵌套/递归时自动 new 新实例） */
    static MutableBigInteger acquire() {
        int[] size = TL_POOL_SIZE.get();
        if (size[0] > 0) {
            int top = size[0] - 1;
            MutableBigInteger m = TL_POOL.get()[top];
            size[0] = top;
            m.reset();
            return m;
        }
        return new MutableBigInteger();
    }

    /** 🔧 归还实例（调用方保证不再引用其内部数组） */
    static void release(MutableBigInteger m) {
        if (m == null) return;
        m.reset(); // 只复位 offset/intLen，数组留待下轮 copyValue 复用
        int[] size = TL_POOL_SIZE.get();
        if (size[0] < TL_POOL_CAPACITY) {
            TL_POOL.get()[size[0]] = m;
            size[0]++;
        }
    }

    /** 🔧 从池化/普通 MutableBigInteger 产出 BigInteger —— 强制拷贝，避免与池共享内部数组 */
    BigInteger toBigIntegerCopy(int sign) {
        if (intLen == 0 || sign == 0)
            return BigInteger.ZERO;
        return new BigInteger(toIntArray(), sign); // toIntArray 总是新建数组
    }

    static final int KNUTH_POW2_THRESH_LEN = 6;

    static final int KNUTH_POW2_THRESH_ZEROS = 3;

    MutableBigInteger() {
        value = new int[1];
        intLen = 0;
    }

    MutableBigInteger(int val) {
        init(val);
    }

    MutableBigInteger(long val) {
        int hi = (int) (val >>> 32);
        if (hi == 0) {
            init((int) val);
        } else {
            value = new int[]{hi, (int) val};
            intLen = 2;
        }
    }

    private void init(int val) {
        value = new int[]{val};
        intLen = val != 0 ? 1 : 0;
    }

    MutableBigInteger(int[] val) {
        value = val;
        intLen = val.length;
    }

    MutableBigInteger(BigInteger b) {
        intLen = b.mag.length;
        value = Arrays.copyOf(b.mag, intLen);
    }

    MutableBigInteger(MutableBigInteger val) {
        intLen = val.intLen;
        value = Arrays.copyOfRange(val.value, val.offset, val.offset + intLen);
    }

    private void ones(int n) {
        if (n > value.length)
            value = new int[n];
        Arrays.fill(value, -1);
        offset = 0;
        intLen = n;
    }

    private int[] getMagnitudeArray() {
        if (offset > 0 || value.length != intLen) {

            int[] tmp = Arrays.copyOfRange(value, offset, offset + intLen);
            Arrays.fill(value, 0);
            offset = 0;
            intLen = tmp.length;
            value = tmp;
        }
        return value;
    }

    private long toLong() {
        assert (intLen <= 2) : "this MutableBigInteger exceeds the range of long";
        if (intLen == 0)
            return 0;
        long d = value[offset] & LONG_MASK;
        return (intLen == 2) ? d << 32 | (value[offset + 1] & LONG_MASK) : d;
    }

    BigInteger toBigInteger(int sign) {
        if (intLen == 0 || sign == 0)
            return BigInteger.ZERO;
        return new BigInteger(getMagnitudeArray(), sign);
    }

    BigInteger toBigInteger() {
        normalize();
        return toBigInteger(isZero() ? 0 : 1);
    }

    BigDecimal toBigDecimal(int sign, int scale) {
        if (intLen == 0 || sign == 0)
            return BigDecimal.zeroValueOf(scale);
        int[] mag = getMagnitudeArray();
        int len = mag.length;
        int d = mag[0];

        if (len > 2 || (d < 0 && len == 2))
            return new BigDecimal(new BigInteger(mag, sign), INFLATED, scale, 0);
        long v = (len == 2) ? ((mag[1] & LONG_MASK) | (d & LONG_MASK) << 32) : d & LONG_MASK;
        return BigDecimal.valueOf(sign == -1 ? -v : v, scale);
    }

    long toCompactValue(int sign) {
        if (intLen == 0 || sign == 0)
            return 0L;
        int[] mag = getMagnitudeArray();
        int len = mag.length;
        int d = mag[0];

        if (len > 2 || (d < 0 && len == 2))
            return INFLATED;
        long v = (len == 2) ? ((mag[1] & LONG_MASK) | (d & LONG_MASK) << 32) : d & LONG_MASK;
        return sign == -1 ? -v : v;
    }

    void clear() {
        offset = intLen = 0;
        for (int index = 0, n = value.length; index < n; index++)
            value[index] = 0;
    }

    void reset() {
        offset = intLen = 0;
    }

    final int compare(MutableBigInteger b) {
        int blen = b.intLen;
        if (intLen < blen)
            return -1;
        if (intLen > blen)
            return 1;

        int[] bval = b.value;
        for (int i = offset, j = b.offset; i < intLen + offset; i++, j++) {
            int b1 = value[i] + 0x80000000;
            int b2 = bval[j] + 0x80000000;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    private int compareShifted(MutableBigInteger b, int ints) {
        int blen = b.intLen;
        int alen = intLen - ints;
        if (alen < blen)
            return -1;
        if (alen > blen)
            return 1;

        int[] bval = b.value;
        for (int i = offset, j = b.offset; i < alen + offset; i++, j++) {
            int b1 = value[i] + 0x80000000;
            int b2 = bval[j] + 0x80000000;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    final int compareHalf(MutableBigInteger b) {
        int blen = b.intLen;
        int len = intLen;
        if (len <= 0)
            return blen <= 0 ? 0 : -1;
        if (len > blen)
            return 1;
        if (len < blen - 1)
            return -1;
        int[] bval = b.value;
        int bstart = 0;
        int carry = 0;

        if (len != blen) {
            if (bval[bstart] == 1) {
                ++bstart;
                carry = 0x80000000;
            } else
                return -1;
        }

        int[] val = value;
        for (int i = offset, j = bstart; i < len + offset; ) {
            int bv = bval[j++];
            long hb = ((bv >>> 1) + carry) & LONG_MASK;
            long v = val[i++] & LONG_MASK;
            if (v != hb)
                return v < hb ? -1 : 1;
            carry = (bv & 1) << 31;
        }
        return carry == 0 ? 0 : -1;
    }

    private final int getLowestSetBit() {
        if (intLen == 0)
            return -1;
        int j, b;
        for (j = intLen - 1; (j > 0) && (value[j + offset] == 0); j--)
            ;
        b = value[j + offset];
        if (b == 0)
            return -1;
        return ((intLen - 1 - j) << 5) + Integer.numberOfTrailingZeros(b);
    }

    private final int getInt(int index) {
        return value[offset + index];
    }

    private final long getLong(int index) {
        return value[offset + index] & LONG_MASK;
    }

    final void normalize() {
        if (intLen == 0) {
            offset = 0;
            return;
        }

        int index = offset;
        if (value[index] != 0)
            return;

        int indexBound = index + intLen;
        do {
            index++;
        } while (index < indexBound && value[index] == 0);

        int numZeros = index - offset;
        intLen -= numZeros;
        offset = (intLen == 0 ? 0 : offset + numZeros);
    }

    private final void ensureCapacity(int len) {
        if (value.length < len) {
            value = new int[len];
            offset = 0;
            intLen = len;
        }
    }

    int[] toIntArray() {
        int[] result = new int[intLen];
        for (int i = 0; i < intLen; i++)
            result[i] = value[offset + i];
        return result;
    }

    void setInt(int index, int val) {
        value[offset + index] = val;
    }

    void setValue(int[] val, int length) {
        value = val;
        intLen = length;
        offset = 0;
    }

    void copyValue(MutableBigInteger src) {
        int len = src.intLen;
        if (value.length < len)
            value = new int[len];
        System.arraycopy(src.value, src.offset, value, 0, len);
        intLen = len;
        offset = 0;
    }

    void copyValue(int[] val) {
        int len = val.length;
        if (value.length < len)
            value = new int[len];
        System.arraycopy(val, 0, value, 0, len);
        intLen = len;
        offset = 0;
    }

    boolean isOne() {
        return (intLen == 1) && (value[offset] == 1);
    }

    boolean isZero() {
        return (intLen == 0);
    }

    boolean isEven() {
        return (intLen == 0) || ((value[offset + intLen - 1] & 1) == 0);
    }

    boolean isOdd() {
        return isZero() ? false : ((value[offset + intLen - 1] & 1) == 1);
    }

    boolean isNormal() {
        if (intLen + offset > value.length)
            return false;
        if (intLen == 0)
            return true;
        return (value[offset] != 0);
    }

    public String toString() {
        BigInteger b = toBigInteger(1);
        return b.toString();
    }

    void safeRightShift(int n) {
        if (n / 32 >= intLen) {
            reset();
        } else {
            rightShift(n);
        }
    }

    void rightShift(int n) {
        if (intLen == 0)
            return;
        int nInts = n >>> 5;
        int nBits = n & 0x1F;
        this.intLen -= nInts;
        if (nBits == 0)
            return;
        int bitsInHighWord = BigInteger.bitLengthForInt(value[offset]);
        if (nBits >= bitsInHighWord) {
            this.primitiveLeftShift(32 - nBits);
            this.intLen--;
        } else {
            primitiveRightShift(nBits);
        }
    }

    void safeLeftShift(int n) {
        if (n > 0) {
            leftShift(n);
        }
    }

    void leftShift(int n) {

        if (intLen == 0)
            return;

        int nInts = n >>> 5;
        int nBits = n & 0x1F;
        int leadingZeros = Integer.numberOfLeadingZeros(value[offset]);

        if (n <= leadingZeros) {
            primitiveLeftShift(nBits);
            return;
        }

        int newLen = intLen + nInts;
        if (nBits > leadingZeros)
            newLen++;

        int[] result;
        final int newOffset;
        if (value.length < newLen) {
            result = new int[newLen];
            newOffset = 0;
        } else {
            result = value;
            newOffset = value.length - offset >= newLen ? offset : 0;
        }

        int trailingZerosPos = newOffset + intLen;
        if (nBits != 0) {

            if (nBits <= leadingZeros) {
                primitiveLeftShift(nBits, result, newOffset);
            } else {
                int lastInt = value[offset + intLen - 1];
                primitiveRightShift(32 - nBits, result, newOffset);
                result[trailingZerosPos++] = lastInt << nBits;
            }
        } else if (result != value || newOffset != offset) {
            System.arraycopy(value, offset, result, newOffset, intLen);
        }

        if (result == value)
            Arrays.fill(result, trailingZerosPos, newOffset + newLen, 0);

        value = result;
        intLen = newLen;
        offset = newOffset;
    }

    private int divadd(int[] a, int[] result, int offset) {
        long carry = 0;

        for (int j = a.length - 1; j >= 0; j--) {
            long sum = (a[j] & LONG_MASK) +
                    (result[j + offset] & LONG_MASK) + carry;
            result[j + offset] = (int) sum;
            carry = sum >>> 32;
        }
        return (int) carry;
    }

    private int mulsub(int[] q, int[] a, int x, int len, int offset) {
        long xLong = x & LONG_MASK;
        long carry = 0;
        offset += len;

        for (int j = len - 1; j >= 0; j--) {
            long product = (a[j] & LONG_MASK) * xLong + carry;
            long difference = q[offset] - product;
            q[offset--] = (int) difference;
            carry = (product >>> 32)
                    + (((difference & LONG_MASK) >
                                    (((~(int) product) & LONG_MASK))) ? 1 : 0);
        }
        return (int) carry;
    }

    private int mulsubBorrow(int[] q, int[] a, int x, int len, int offset) {
        long xLong = x & LONG_MASK;
        long carry = 0;
        offset += len;
        for (int j = len - 1; j >= 0; j--) {
            long product = (a[j] & LONG_MASK) * xLong + carry;
            long difference = q[offset--] - product;
            carry = (product >>> 32)
                    + (((difference & LONG_MASK) >
                                    (((~(int) product) & LONG_MASK))) ? 1 : 0);
        }
        return (int) carry;
    }

    private void primitiveRightShift(int n) {
        primitiveRightShift(n, value, offset);
    }

    private void primitiveRightShift(int n, int[] result, int resFrom) {
        int[] val = value;
        int n2 = 32 - n;

        int b = val[offset];
        result[resFrom] = b >>> n;
        for (int i = 1; i < intLen; i++) {
            int c = b;
            b = val[offset + i];
            result[resFrom + i] = (c << n2) | (b >>> n);
        }
    }

    private void primitiveLeftShift(int n) {
        primitiveLeftShift(n, value, offset);
    }

    private void primitiveLeftShift(int n, int[] result, int resFrom) {
        int[] val = value;
        int n2 = 32 - n;
        final int m = intLen - 1;
        int b = val[offset];
        for (int i = 0; i < m; i++) {
            int c = val[offset + i + 1];
            result[resFrom + i] = (b << n) | (c >>> n2);
            b = c;
        }
        result[resFrom + m] = b << n;
    }

    private BigInteger getLower(int n) {
        if (isZero()) {
            return BigInteger.ZERO;
        } else if (intLen < n) {
            return toBigInteger(1);
        } else {

            int len = n;
            while (len > 0 && value[offset + intLen - len] == 0)
                len--;
            int sign = len > 0 ? 1 : 0;
            return new BigInteger(Arrays.copyOfRange(value, offset + intLen - len, offset + intLen), sign);
        }
    }

    private void keepLower(int n) {
        if (intLen >= n) {
            offset += intLen - n;
            intLen = n;
        }
    }

    void add(MutableBigInteger addend) {
        int x = intLen;
        int y = addend.intLen;
        int resultLen = (intLen > addend.intLen ? intLen : addend.intLen);
        int[] result = (value.length < resultLen ? new int[resultLen] : value);

        int rstart = result.length - 1;
        long sum;
        long carry = 0;

        while (x > 0 && y > 0) {
            x--;
            y--;
            sum = (value[x + offset] & LONG_MASK) +
                    (addend.value[y + addend.offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        while (x > 0) {
            x--;
            if (carry == 0 && result == value && rstart == (x + offset))
                return;
            sum = (value[x + offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }
        while (y > 0) {
            y--;
            sum = (addend.value[y + addend.offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        if (carry > 0) {
            resultLen++;
            if (result.length < resultLen) {
                int temp[] = new int[resultLen];

                System.arraycopy(result, 0, temp, 1, result.length);
                temp[0] = 1;
                result = temp;
            } else {
                result[rstart--] = 1;
            }
        }

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    void addShifted(MutableBigInteger addend, int n) {
        if (addend.isZero()) {
            return;
        }

        int x = intLen;
        int y = addend.intLen + n;
        int resultLen = (intLen > y ? intLen : y);
        int[] result = (value.length < resultLen ? new int[resultLen] : value);

        int rstart = result.length - 1;
        long sum;
        long carry = 0;

        while (x > 0 && y > 0) {
            x--;
            y--;
            int bval = y + addend.offset < addend.value.length ? addend.value[
                    y + addend.offset] : 0;
            sum = (value[x + offset] & LONG_MASK) +
                    (bval & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        while (x > 0) {
            x--;
            if (carry == 0 && result == value && rstart == (x + offset)) {
                return;
            }
            sum = (value[x + offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }
        while (y > 0) {
            y--;
            int bval = y + addend.offset < addend.value.length ? addend.value[
                    y + addend.offset] : 0;
            sum = (bval & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        if (carry > 0) {
            resultLen++;
            if (result.length < resultLen) {
                int temp[] = new int[resultLen];

                System.arraycopy(result, 0, temp, 1, result.length);
                temp[0] = 1;
                result = temp;
            } else {
                result[rstart--] = 1;
            }
        }

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    void addDisjoint(MutableBigInteger addend, int n) {
        if (addend.isZero())
            return;

        int x = intLen;
        int y = addend.intLen + n;
        int resultLen = (intLen > y ? intLen : y);
        int[] result;
        if (value.length < resultLen)
            result = new int[resultLen];
        else {
            result = value;
            Arrays.fill(value, offset + intLen, value.length, 0);
        }

        int rstart = result.length - 1;

        System.arraycopy(value, offset, result, rstart + 1 - x, x);
        y -= x;
        rstart -= x;

        int len = Math.min(y, addend.value.length - addend.offset);
        System.arraycopy(addend.value, addend.offset, result, rstart + 1 - y, len);

        for (int i = rstart + 1 - y + len; i < rstart + 1; i++)
            result[i] = 0;

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    void addLower(MutableBigInteger addend, int n) {
        MutableBigInteger a = new MutableBigInteger(addend);
        if (a.offset + a.intLen >= n) {
            a.offset = a.offset + a.intLen - n;
            a.intLen = n;
        }
        a.normalize();
        add(a);
    }

    void shiftAdd(MutableBigInteger addend, int n) {

        if (addend.intLen <= n) {
            shiftAddDisjoint(addend, n);
        } else if (intLen == 0) {
            copyValue(addend);
        } else {
            leftShift(n << 5);
            add(addend);
        }
    }

    void shiftAddDisjoint(MutableBigInteger addend, int n) {
        if (intLen == 0) {
            copyValue(addend);
            return;
        }

        int[] res;
        final int resLen = intLen + n, resOffset;
        if (resLen > value.length) {
            res = new int[resLen];
            System.arraycopy(value, offset, res, 0, intLen);
            resOffset = 0;
        } else {
            res = value;
            if (offset + resLen > value.length) {
                System.arraycopy(value, offset, res, 0, intLen);
                resOffset = 0;
            } else {
                resOffset = offset;
            }

            if (addend.intLen < n)
                Arrays.fill(res, resOffset + intLen, resOffset + resLen - addend.intLen, 0);
        }

        System.arraycopy(addend.value, addend.offset, res, resOffset + resLen - addend.intLen, addend.intLen);

        value = res;
        offset = resOffset;
        intLen = resLen;
    }

    int subtract(MutableBigInteger b) {
        MutableBigInteger a = this;

        int[] result = value;
        int sign = a.compare(b);

        if (sign == 0) {
            reset();
            return 0;
        }
        if (sign < 0) {
            MutableBigInteger tmp = a;
            a = b;
            b = tmp;
        }

        int resultLen = a.intLen;
        if (result.length < resultLen)
            result = new int[resultLen];

        long diff = 0;
        int x = a.intLen;
        int y = b.intLen;
        int rstart = result.length - 1;

        while (y > 0) {
            x--;
            y--;

            diff = (a.value[x + a.offset] & LONG_MASK) -
                    (b.value[y + b.offset] & LONG_MASK) + (diff >> 32);
            result[rstart--] = (int) diff;
        }

        while (x > 0) {
            x--;
            diff = (a.value[x + a.offset] & LONG_MASK) + (diff >> 32);
            result[rstart--] = (int) diff;
        }

        value = result;
        intLen = resultLen;
        offset = value.length - resultLen;
        normalize();
        return sign;
    }

    private int difference(MutableBigInteger b) {
        MutableBigInteger a = this;
        int sign = a.compare(b);
        if (sign == 0)
            return 0;
        if (sign < 0) {
            MutableBigInteger tmp = a;
            a = b;
            b = tmp;
        }

        long diff = 0;
        int x = a.intLen;
        int y = b.intLen;

        while (y > 0) {
            x--;
            y--;
            diff = (a.value[a.offset + x] & LONG_MASK) -
                    (b.value[b.offset + y] & LONG_MASK) + (diff >> 32);
            a.value[a.offset + x] = (int) diff;
        }

        while (diff < 0 && x > 0) {
            x--;
            diff = (a.value[a.offset + x] & LONG_MASK) + (diff >> 32);
            a.value[a.offset + x] = (int) diff;
        }

        a.normalize();
        return sign;
    }

    void multiply(MutableBigInteger y, MutableBigInteger z) {
        int xLen = intLen;
        int yLen = y.intLen;
        int newLen = xLen + yLen;

        if (z.value.length < newLen)
            z.value = new int[newLen];
        z.offset = 0;
        z.intLen = newLen;

        long carry = 0;
        for (int j = yLen - 1, k = yLen + xLen - 1; j >= 0; j--, k--) {
            long product = (y.value[j + y.offset] & LONG_MASK) *
                            (value[xLen - 1 + offset] & LONG_MASK) + carry;
            z.value[k] = (int) product;
            carry = product >>> 32;
        }
        z.value[xLen - 1] = (int) carry;

        for (int i = xLen - 2; i >= 0; i--) {
            carry = 0;
            for (int j = yLen - 1, k = yLen + i; j >= 0; j--, k--) {
                long product = (y.value[j + y.offset] & LONG_MASK) *
                                (value[i + offset] & LONG_MASK) +
                        (z.value[k] & LONG_MASK) + carry;
                z.value[k] = (int) product;
                carry = product >>> 32;
            }
            z.value[i] = (int) carry;
        }

        z.normalize();
    }

    void mul(int y, MutableBigInteger z) {
        if (y == 1) {
            z.copyValue(this);
            return;
        }

        if (y == 0) {
            z.clear();
            return;
        }

        long ylong = y & LONG_MASK;
        int[] zval = (z.value.length < intLen + 1 ? new int[intLen + 1]
                : z.value);
        long carry = 0;
        for (int i = intLen - 1; i >= 0; i--) {
            long product = ylong * (value[i + offset] & LONG_MASK) + carry;
            zval[i + 1] = (int) product;
            carry = product >>> 32;
        }

        if (carry == 0) {
            z.offset = 1;
            z.intLen = intLen;
        } else {
            z.offset = 0;
            z.intLen = intLen + 1;
            zval[0] = (int) carry;
        }
        z.value = zval;
    }

    int divideOneWord(int divisor, MutableBigInteger quotient) {
        long divisorLong = divisor & LONG_MASK;

        if (intLen == 1) {
            int dividendValue = value[offset];
            int q = Integer.divideUnsigned(dividendValue, divisor);
            int r = Integer.remainderUnsigned(dividendValue, divisor);
            quotient.value[0] = q;
            quotient.intLen = (q == 0) ? 0 : 1;
            quotient.offset = 0;
            return r;
        }

        if (quotient.value.length < intLen)
            quotient.value = new int[intLen];
        quotient.offset = 0;
        quotient.intLen = intLen;

        long rem = 0;
        for (int xlen = intLen; xlen > 0; xlen--) {
            long dividendEstimate = (rem << 32) |
                    (value[offset + intLen - xlen] & LONG_MASK);
            int q = (int) Long.divideUnsigned(dividendEstimate, divisorLong);
            rem = Long.remainderUnsigned(dividendEstimate, divisorLong);
            quotient.value[intLen - xlen] = q;
        }

        quotient.normalize();
        return (int) rem;
    }

    MutableBigInteger divide(MutableBigInteger b, MutableBigInteger quotient) {
        return divide(b, quotient, true);
    }

    MutableBigInteger divide(MutableBigInteger b, MutableBigInteger quotient, boolean needRemainder) {
        if (b.intLen < BigInteger.BURNIKEL_ZIEGLER_THRESHOLD ||
                intLen - b.intLen < BigInteger.BURNIKEL_ZIEGLER_OFFSET) {
            return divideKnuth(b, quotient, needRemainder);
        } else {
            return divideAndRemainderBurnikelZiegler(b, quotient);
        }
    }

    MutableBigInteger divideKnuth(MutableBigInteger b, MutableBigInteger quotient) {
        return divideKnuth(b, quotient, true);
    }

    MutableBigInteger divideKnuth(MutableBigInteger b, MutableBigInteger quotient, boolean needRemainder) {
        if (b.intLen == 0)
            throw new ArithmeticException("BigInteger divide by zero");

        if (intLen == 0) {
            quotient.intLen = quotient.offset = 0;
            return needRemainder ? new MutableBigInteger() : null;
        }

        int cmp = compare(b);

        if (cmp < 0) {
            quotient.intLen = quotient.offset = 0;
            return needRemainder ? new MutableBigInteger(this) : null;
        }

        if (cmp == 0) {
            quotient.value[0] = quotient.intLen = 1;
            quotient.offset = 0;
            return needRemainder ? new MutableBigInteger() : null;
        }

        quotient.clear();

        if (b.intLen == 1) {
            int r = divideOneWord(b.value[b.offset], quotient);
            if (needRemainder) {
                if (r == 0)
                    return new MutableBigInteger();
                return new MutableBigInteger(r);
            } else {
                return null;
            }
        }

        if (intLen >= KNUTH_POW2_THRESH_LEN) {
            int trailingZeroBits = Math.min(getLowestSetBit(), b.getLowestSetBit());
            if (trailingZeroBits >= KNUTH_POW2_THRESH_ZEROS * 32) {
                MutableBigInteger a = new MutableBigInteger(this);
                b = new MutableBigInteger(b);
                a.rightShift(trailingZeroBits);
                b.rightShift(trailingZeroBits);
                MutableBigInteger r = a.divideKnuth(b, quotient);
                r.leftShift(trailingZeroBits);
                return r;
            }
        }

        return divideMagnitude(b, quotient, needRemainder);
    }

    MutableBigInteger divideAndRemainderBurnikelZiegler(MutableBigInteger b, MutableBigInteger quotient) {
        int r = intLen;
        int s = b.intLen;

        quotient.offset = quotient.intLen = 0;

        if (r < s) {
            return this;
        } else {

            int m = 1 << (32 - Integer.numberOfLeadingZeros(s / BigInteger.BURNIKEL_ZIEGLER_THRESHOLD));

            int j = (s + m - 1) / m;
            int n = j * m;
            long n32 = 32L * n;
            int sigma = (int) Math.max(0, n32 - b.bitLength());
            MutableBigInteger bShifted = new MutableBigInteger(b);
            bShifted.safeLeftShift(sigma);
            MutableBigInteger aShifted = new MutableBigInteger(this);
            aShifted.safeLeftShift(sigma);

            int t = (int) ((aShifted.bitLength() + n32) / n32);
            if (t < 2) {
                t = 2;
            }

            MutableBigInteger a1 = aShifted.getBlock(t - 1, t, n);

            MutableBigInteger z = aShifted.getBlock(t - 2, t, n);
            z.addDisjoint(a1, n);

            MutableBigInteger qi = new MutableBigInteger();
            MutableBigInteger ri;
            for (int i = t - 2; i > 0; i--) {

                ri = z.divide2n1n(bShifted, qi);

                z = aShifted.getBlock(i - 1, t, n);
                z.addDisjoint(ri, n);
                quotient.addShifted(qi, i * n);
            }

            ri = z.divide2n1n(bShifted, qi);
            quotient.add(qi);

            ri.rightShift(sigma);
            return ri;
        }
    }

    private MutableBigInteger divide2n1n(MutableBigInteger b, MutableBigInteger quotient) {
        int n = b.intLen;

        if (n % 2 != 0 || n < BigInteger.BURNIKEL_ZIEGLER_THRESHOLD) {
            return divideKnuth(b, quotient);
        }

        MutableBigInteger aUpper = new MutableBigInteger(this);
        aUpper.safeRightShift(32 * (n / 2));
        keepLower(n / 2);

        MutableBigInteger q1 = new MutableBigInteger();
        MutableBigInteger r1 = aUpper.divide3n2n(b, q1);

        addDisjoint(r1, n / 2);
        MutableBigInteger r2 = divide3n2n(b, quotient);

        quotient.addDisjoint(q1, n / 2);
        return r2;
    }

    private MutableBigInteger divide3n2n(MutableBigInteger b, MutableBigInteger quotient) {
        int n = b.intLen / 2;

        MutableBigInteger a12 = new MutableBigInteger(this);
        a12.safeRightShift(32 * n);

        MutableBigInteger b1 = new MutableBigInteger(b);
        b1.safeRightShift(n * 32);
        BigInteger b2 = b.getLower(n);

        MutableBigInteger r;
        MutableBigInteger d;
        if (compareShifted(b, n) < 0) {

            r = a12.divide2n1n(b1, quotient);

            d = new MutableBigInteger(quotient.toBigInteger().multiply(b2));
        } else {

            quotient.ones(n);
            a12.add(b1);
            b1.leftShift(32 * n);
            a12.subtract(b1);
            r = a12;

            d = new MutableBigInteger(b2);
            d.leftShift(32 * n);
            d.subtract(new MutableBigInteger(b2));
        }

        r.leftShift(32 * n);
        r.addLower(this, n);

        while (r.compare(d) < 0) {
            r.add(b);
            quotient.subtract(MutableBigInteger.ONE);
        }
        r.subtract(d);

        return r;
    }

    private MutableBigInteger getBlock(int index, int numBlocks, int blockLength) {
        int blockStart = index * blockLength;
        if (blockStart >= intLen) {
            return new MutableBigInteger();
        }

        int blockEnd;
        if (index == numBlocks - 1) {
            blockEnd = intLen;
        } else {
            blockEnd = (index + 1) * blockLength;
        }
        if (blockEnd > intLen) {
            return new MutableBigInteger();
        }

        int[] newVal = Arrays.copyOfRange(value, offset + intLen - blockEnd, offset + intLen - blockStart);
        return new MutableBigInteger(newVal);
    }

    long bitLength() {
        if (intLen == 0)
            return 0;
        return intLen * 32L - Integer.numberOfLeadingZeros(value[offset]);
    }

    long divide(long v, MutableBigInteger quotient) {
        if (v == 0)
            throw new ArithmeticException("BigInteger divide by zero");

        if (intLen == 0) {
            quotient.intLen = quotient.offset = 0;
            return 0;
        }
        if (v < 0)
            v = -v;

        int d = (int) (v >>> 32);
        quotient.clear();

        if (d == 0)
            return divideOneWord((int) v, quotient) & LONG_MASK;
        else {
            return divideLongMagnitude(v, quotient).toLong();
        }
    }

    private MutableBigInteger divideMagnitude(MutableBigInteger div,
            MutableBigInteger quotient,
            boolean needRemainder) {

        int shift = Integer.numberOfLeadingZeros(div.value[div.offset]);

        final int dlen = div.intLen;
        int[] divisor;
        MutableBigInteger rem;
        if (shift > 0) {
            divisor = new int[dlen];
            div.primitiveLeftShift(shift, divisor, 0);
            if (Integer.numberOfLeadingZeros(value[offset]) >= shift) {
                int[] remarr = new int[intLen + 1];
                rem = new MutableBigInteger(remarr);
                rem.intLen = intLen;
                rem.offset = 1;
                this.primitiveLeftShift(shift, remarr, 1);
            } else {
                int[] remarr = new int[intLen + 2];
                rem = new MutableBigInteger(remarr);
                rem.intLen = intLen + 1;
                rem.offset = 1;
                int rFrom = offset;
                int c = 0;
                int n2 = 32 - shift;
                for (int i = 1; i < intLen + 1; i++, rFrom++) {
                    int b = c;
                    c = value[rFrom];
                    remarr[i] = (b << shift) | (c >>> n2);
                }
                remarr[intLen + 1] = c << shift;
            }
        } else {
            divisor = Arrays.copyOfRange(div.value, div.offset, div.offset + div.intLen);
            rem = new MutableBigInteger(new int[intLen + 1]);
            System.arraycopy(value, offset, rem.value, 1, intLen);
            rem.intLen = intLen;
            rem.offset = 1;
        }

        int nlen = rem.intLen;

        final int limit = nlen - dlen + 1;
        if (quotient.value.length < limit) {
            quotient.value = new int[limit];
            quotient.offset = 0;
        }
        quotient.intLen = limit;
        int[] q = quotient.value;

        rem.offset = 0;
        rem.value[0] = 0;
        rem.intLen++;

        int dh = divisor[0];
        long dhLong = dh & LONG_MASK;
        int dl = divisor[1];

        for (int j = 0; j < limit - 1; j++) {

            int qhat = 0;
            int qrem = 0;
            boolean skipCorrection = false;
            int nh = rem.value[j + rem.offset];
            int nh2 = nh + 0x80000000;
            int nm = rem.value[j + 1 + rem.offset];

            if (nh == dh) {
                qhat = ~0;
                qrem = nh + nm;
                skipCorrection = qrem + 0x80000000 < nh2;
            } else {
                long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
                qhat = (int) Long.divideUnsigned(nChunk, dhLong);
                qrem = (int) Long.remainderUnsigned(nChunk, dhLong);
            }

            if (qhat == 0)
                continue;

            if (!skipCorrection) {
                long nl = rem.value[j + 2 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }

            rem.value[j + rem.offset] = 0;
            int borrow = mulsub(rem.value, divisor, qhat, dlen, j + rem.offset);

            if (borrow + 0x80000000 > nh2) {

                divadd(divisor, rem.value, j + 1 + rem.offset);
                qhat--;
            }

            q[j] = qhat;
        }

        int qhat = 0;
        int qrem = 0;
        boolean skipCorrection = false;
        int nh = rem.value[limit - 1 + rem.offset];
        int nh2 = nh + 0x80000000;
        int nm = rem.value[limit + rem.offset];

        if (nh == dh) {
            qhat = ~0;
            qrem = nh + nm;
            skipCorrection = qrem + 0x80000000 < nh2;
        } else {
            long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
            qhat = (int) Long.divideUnsigned(nChunk, dhLong);
            qrem = (int) Long.remainderUnsigned(nChunk, dhLong);
        }
        if (qhat != 0) {
            if (!skipCorrection) {
                long nl = rem.value[limit + 1 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }

            int borrow;
            rem.value[limit - 1 + rem.offset] = 0;
            if (needRemainder)
                borrow = mulsub(rem.value, divisor, qhat, dlen, limit - 1 + rem.offset);
            else
                borrow = mulsubBorrow(rem.value, divisor, qhat, dlen, limit - 1 + rem.offset);

            if (borrow + 0x80000000 > nh2) {

                if (needRemainder)
                    divadd(divisor, rem.value, limit - 1 + 1 + rem.offset);
                qhat--;
            }

            q[(limit - 1)] = qhat;
        }

        if (needRemainder) {

            if (shift > 0)
                rem.rightShift(shift);
            rem.normalize();
        }
        quotient.normalize();
        return needRemainder ? rem : null;
    }

    private MutableBigInteger divideLongMagnitude(long ldivisor, MutableBigInteger quotient) {

        MutableBigInteger rem = new MutableBigInteger(new int[intLen + 1]);
        System.arraycopy(value, offset, rem.value, 1, intLen);
        rem.intLen = intLen;
        rem.offset = 1;

        int nlen = rem.intLen;

        int limit = nlen - 2 + 1;
        if (quotient.value.length < limit) {
            quotient.value = new int[limit];
            quotient.offset = 0;
        }
        quotient.intLen = limit;
        int[] q = quotient.value;

        int shift = Long.numberOfLeadingZeros(ldivisor);
        if (shift > 0) {
            ldivisor <<= shift;
            rem.leftShift(shift);
        }

        if (rem.intLen == nlen) {
            rem.offset = 0;
            rem.value[0] = 0;
            rem.intLen++;
        }

        int dh = (int) (ldivisor >>> 32);
        long dhLong = dh & LONG_MASK;
        int dl = (int) (ldivisor & LONG_MASK);

        for (int j = 0; j < limit; j++) {

            int qhat = 0;
            int qrem = 0;
            boolean skipCorrection = false;
            int nh = rem.value[j + rem.offset];
            int nh2 = nh + 0x80000000;
            int nm = rem.value[j + 1 + rem.offset];

            if (nh == dh) {
                qhat = ~0;
                qrem = nh + nm;
                skipCorrection = qrem + 0x80000000 < nh2;
            } else {
                long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
                qhat = (int) Long.divideUnsigned(nChunk, dhLong);
                qrem = (int) Long.remainderUnsigned(nChunk, dhLong);
            }

            if (qhat == 0)
                continue;

            if (!skipCorrection) {
                long nl = rem.value[j + 2 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }

            rem.value[j + rem.offset] = 0;
            int borrow = mulsubLong(rem.value, dh, dl, qhat, j + rem.offset);

            if (borrow + 0x80000000 > nh2) {

                divaddLong(dh, dl, rem.value, j + 1 + rem.offset);
                qhat--;
            }

            q[j] = qhat;
        }

        if (shift > 0)
            rem.rightShift(shift);

        quotient.normalize();
        rem.normalize();
        return rem;
    }

    private int divaddLong(int dh, int dl, int[] result, int offset) {
        long carry = 0;

        long sum = (dl & LONG_MASK) + (result[1 + offset] & LONG_MASK);
        result[1 + offset] = (int) sum;

        sum = (dh & LONG_MASK) + (result[offset] & LONG_MASK) + carry;
        result[offset] = (int) sum;
        carry = sum >>> 32;
        return (int) carry;
    }

    private int mulsubLong(int[] q, int dh, int dl, int x, int offset) {
        long xLong = x & LONG_MASK;
        offset += 2;
        long product = (dl & LONG_MASK) * xLong;
        long difference = q[offset] - product;
        q[offset--] = (int) difference;
        long carry = (product >>> 32)
                + (((difference & LONG_MASK) >
                                (((~(int) product) & LONG_MASK))) ? 1 : 0);
        product = (dh & LONG_MASK) * xLong + carry;
        difference = q[offset] - product;
        q[offset--] = (int) difference;
        carry = (product >>> 32)
                + (((difference & LONG_MASK) >
                                (((~(int) product) & LONG_MASK))) ? 1 : 0);
        return (int) carry;
    }

    private boolean unsignedLongCompare(long one, long two) {
        return (one + Long.MIN_VALUE) > (two + Long.MIN_VALUE);
    }

    MutableBigInteger[] sqrtRem(boolean needRemainder) {

        if (this.intLen <= 2) {
            final long x = this.toLong();
            long s = unsignedLongSqrt(x);

            return new MutableBigInteger[]{
                    new MutableBigInteger((int) s),
                    needRemainder ? new MutableBigInteger(x - s * s) : null
            };
        }

        MutableBigInteger x = this;
        final int shift = (Integer.numberOfLeadingZeros(x.value[x.offset]) & ~1)
                + ((x.intLen & 1) << 5);

        if (shift != 0) {
            x = new MutableBigInteger(x);
            x.leftShift(shift);
        }

        MutableBigInteger[] sqrtRem = x.sqrtRemKaratsuba(x.intLen, needRemainder);

        if (shift != 0) {
            final int halfShift = shift >> 1;
            if (needRemainder) {

                final long s0 = sqrtRem[0].value[sqrtRem[0].offset + sqrtRem[0].intLen - 1]
                        & (-1 >>> -halfShift);
                if (s0 != 0L) {
                    MutableBigInteger doubleProd = new MutableBigInteger();
                    sqrtRem[0].mul((int) (s0 << 1), doubleProd);

                    sqrtRem[1].add(doubleProd);
                    sqrtRem[1].subtract(new MutableBigInteger(s0 * s0));
                }
                sqrtRem[1].rightShift(shift);
            }
            sqrtRem[0].primitiveRightShift(halfShift);
        }
        return sqrtRem;
    }

    private static long unsignedLongSqrt(long x) {

        long s = (long) Math.sqrt(x >= 0 ? x : x + 0x1p64);
        long s2 = s * s;
        return Long.compareUnsigned(x, s2) < 0 || s > LONG_MASK
                ? s - 1
                : (Long.compareUnsigned(x, s2 + (s << 1)) <= 0
                        ? s
                        : s + 1);
    }

    private MutableBigInteger[] sqrtRemKaratsuba(int len, boolean needRemainder) {
        if (len == 2) {
            long x = ((value[offset] & LONG_MASK) << 32) | (value[offset + 1] & LONG_MASK);
            long s = unsignedLongSqrt(x);

            MutableBigInteger sqrt = new MutableBigInteger(new int[intLen >> 1]);

            sqrt.intLen = 1;
            sqrt.value[0] = (int) s;

            return new MutableBigInteger[]{sqrt, new MutableBigInteger(x - s * s)};
        }

        final int halfLen = len >> 1;

        MutableBigInteger[] sr = sqrtRemKaratsuba(halfLen + (halfLen & 1), true);

        final int blockLen = halfLen >> 1;
        MutableBigInteger dividend = sr[1];
        dividend.shiftAddDisjoint(getBlockForSqrt(1, len, blockLen), blockLen);

        MutableBigInteger sqrt = sr[0];
        MutableBigInteger q = new MutableBigInteger();
        MutableBigInteger u = dividend.divide(sqrt, q);
        if (q.isOdd())
            u.add(sqrt);
        q.rightShift(1);

        sqrt.shiftAdd(q, blockLen);

        u.shiftAddDisjoint(getBlockForSqrt(0, len, blockLen), blockLen);
        BigInteger qBig = q.toBigInteger();
        MutableBigInteger qSqr = new MutableBigInteger(qBig.multiply(qBig).mag);

        MutableBigInteger rem;
        if (needRemainder) {
            rem = u;
            if (rem.subtract(qSqr) < 0) {
                MutableBigInteger twiceSqrt = new MutableBigInteger(sqrt);
                twiceSqrt.leftShift(1);

                rem.add(ONE);
                rem.subtract(twiceSqrt);
                sqrt.subtract(ONE);
            }
        } else {
            rem = null;
            if (u.compare(qSqr) < 0)
                sqrt.subtract(ONE);
        }

        sr[1] = rem;
        return sr;
    }

    private MutableBigInteger getBlockForSqrt(int blockIndex, int len, int blockLen) {
        final int to = offset + len - blockIndex * blockLen;

        int from;
        for (from = to - blockLen; from < to && value[from] == 0; from++)
            ;

        return from == to
                ? new MutableBigInteger()
                : new MutableBigInteger(Arrays.copyOfRange(value, from, to));
    }

    MutableBigInteger hybridGCD(MutableBigInteger b) {

        MutableBigInteger a = this;
        MutableBigInteger q = new MutableBigInteger();

        while (b.intLen != 0) {
            if (Math.abs(a.intLen - b.intLen) < 2)
                return a.binaryGCD(b);

            MutableBigInteger r = a.divide(b, q);
            a = b;
            b = r;
        }
        return a;
    }

    private MutableBigInteger binaryGCD(MutableBigInteger v) {

        MutableBigInteger u = this;
        MutableBigInteger r = new MutableBigInteger();

        int s1 = u.getLowestSetBit();
        int s2 = v.getLowestSetBit();
        int k = (s1 < s2) ? s1 : s2;
        if (k != 0) {
            u.rightShift(k);
            v.rightShift(k);
        }

        boolean uOdd = (k == s1);
        MutableBigInteger t = uOdd ? v : u;
        int tsign = uOdd ? -1 : 1;

        int lb;
        while ((lb = t.getLowestSetBit()) >= 0) {

            t.rightShift(lb);

            if (tsign > 0)
                u = t;
            else
                v = t;

            if (u.intLen < 2 && v.intLen < 2) {
                int x = u.value[u.offset];
                int y = v.value[v.offset];
                x = binaryGcd(x, y);
                r.value[0] = x;
                r.intLen = 1;
                r.offset = 0;
                if (k > 0)
                    r.leftShift(k);
                return r;
            }

            if ((tsign = u.difference(v)) == 0)
                break;
            t = (tsign >= 0) ? u : v;
        }

        if (k > 0)
            u.leftShift(k);
        return u;
    }

    static int binaryGcd(int a, int b) {
        if (b == 0)
            return a;
        if (a == 0)
            return b;

        int aZeros = Integer.numberOfTrailingZeros(a);
        int bZeros = Integer.numberOfTrailingZeros(b);
        a >>>= aZeros;
        b >>>= bZeros;

        int t = (aZeros < bZeros ? aZeros : bZeros);

        while (a != b) {
            if ((a + 0x80000000) > (b + 0x80000000)) {
                a -= b;
                a >>>= Integer.numberOfTrailingZeros(a);
            } else {
                b -= a;
                b >>>= Integer.numberOfTrailingZeros(b);
            }
        }
        return a << t;
    }

    MutableBigInteger mutableModInverse(MutableBigInteger p) {

        if (p.isOdd())
            return modInverse(p);

        if (isEven())
            throw new ArithmeticException("BigInteger not invertible.");

        int powersOf2 = p.getLowestSetBit();

        MutableBigInteger oddMod = new MutableBigInteger(p);
        oddMod.rightShift(powersOf2);

        if (oddMod.isOne())
            return modInverseMP2(powersOf2);

        MutableBigInteger oddPart = modInverse(oddMod);

        MutableBigInteger evenPart = modInverseMP2(powersOf2);

        MutableBigInteger y1 = modInverseBP2(oddMod, powersOf2);
        MutableBigInteger y2 = oddMod.modInverseMP2(powersOf2);

        MutableBigInteger temp1 = new MutableBigInteger();
        MutableBigInteger temp2 = new MutableBigInteger();
        MutableBigInteger result = new MutableBigInteger();

        oddPart.leftShift(powersOf2);
        oddPart.multiply(y1, result);
        oddPart.clear();

        evenPart.multiply(oddMod, temp1);
        temp1.multiply(y2, temp2);

        result.add(temp2);
        return result.divide(p, temp1);
    }

    MutableBigInteger modInverseMP2(int k) {
        if (isEven())
            throw new ArithmeticException("Non-invertible. (GCD != 1)");

        if (k > 64)
            return euclidModInverse(k);

        int t = inverseMod32(value[offset + intLen - 1]);

        if (k < 33) {
            t = (k == 32 ? t : t & ((1 << k) - 1));
            return new MutableBigInteger(t);
        }

        long pLong = (value[offset + intLen - 1] & LONG_MASK);
        if (intLen > 1)
            pLong |= ((long) value[offset + intLen - 2] << 32);
        long tLong = t & LONG_MASK;
        tLong = tLong * (2 - pLong * tLong);
        tLong = (k == 64 ? tLong : tLong & ((1L << k) - 1));

        MutableBigInteger result = new MutableBigInteger(new int[2]);
        result.value[0] = (int) (tLong >>> 32);
        result.value[1] = (int) tLong;
        result.intLen = 2;
        result.normalize();
        return result;
    }

    static int inverseMod32(int val) {

        int t = val;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        return t;
    }

    static long inverseMod64(long val) {

        long t = val;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        assert (t * val == 1);
        return t;
    }

    static MutableBigInteger modInverseBP2(MutableBigInteger mod, int k) {

        return fixup(new MutableBigInteger(1), new MutableBigInteger(mod), k);
    }

    private MutableBigInteger modInverse(MutableBigInteger mod) {
        MutableBigInteger p = TL_TEMP_A.get().shallowCopy(mod);
        MutableBigInteger f = TL_TEMP_B.get().shallowCopy(this);
        MutableBigInteger g = TL_TEMP_C.get().shallowCopy(mod);
        SignedMutableBigInteger c = new SignedMutableBigInteger(1);
        SignedMutableBigInteger d = new SignedMutableBigInteger();
        MutableBigInteger temp = null;
        SignedMutableBigInteger sTemp = null;

        int k = 0;

        if (f.isEven()) {
            int trailingZeros = f.getLowestSetBit();
            f.rightShift(trailingZeros);
            d.leftShift(trailingZeros);
            k = trailingZeros;
        }

        while (!f.isOne()) {

            if (f.isZero())
                throw new ArithmeticException("BigInteger not invertible.");

            if (f.compare(g) < 0) {
                temp = f;
                f = g;
                g = temp;
                sTemp = d;
                d = c;
                c = sTemp;
            }

            if (((f.value[f.offset + f.intLen - 1] ^
                                    g.value[g.offset + g.intLen - 1]) & 3) == 0) {
                f.subtract(g);
                c.signedSubtract(d);
            } else {
                f.add(g);
                c.signedAdd(d);
            }

            int trailingZeros = f.getLowestSetBit();
            f.rightShift(trailingZeros);
            d.leftShift(trailingZeros);
            k += trailingZeros;
        }

        if (c.compare(p) >= 0) {
            MutableBigInteger remainder = c.divide(p,
                    new MutableBigInteger());

            c.copyValue(remainder);
        }

        if (c.sign < 0) {
            c.signedAdd(p);
        }

        return fixup(c, p, k);
    }

    static MutableBigInteger fixup(MutableBigInteger c, MutableBigInteger p,
            int k) {
        MutableBigInteger temp = new MutableBigInteger();

        int r = -inverseMod32(p.value[p.offset + p.intLen - 1]);

        for (int i = 0, numWords = k >> 5; i < numWords; i++) {

            int v = r * c.value[c.offset + c.intLen - 1];
            temp.copyValue(p);
            temp.mul(v, temp);
            c.add(temp);

            c.intLen--;
        }
        int numBits = k & 0x1f;
        if (numBits != 0) {

            int v = r * c.value[c.offset + c.intLen - 1];
            v &= ((1 << numBits) - 1);

            temp.copyValue(p);
            temp.mul(v, temp);
            c.add(temp);

            c.rightShift(numBits);
        }

        if (c.compare(p) >= 0)
            c = c.divide(p, new MutableBigInteger());

        return c;
    }

    MutableBigInteger euclidModInverse(int k) {
        MutableBigInteger b = new MutableBigInteger(1);
        b.leftShift(k);
        MutableBigInteger mod = new MutableBigInteger(b);

        MutableBigInteger a = new MutableBigInteger(this);
        MutableBigInteger q = new MutableBigInteger();
        MutableBigInteger r = b.divide(a, q);

        MutableBigInteger swapper = b;

        b = r;
        r = swapper;

        MutableBigInteger t1 = new MutableBigInteger(q);
        MutableBigInteger t0 = new MutableBigInteger(1);
        MutableBigInteger temp = new MutableBigInteger();

        while (!b.isOne()) {
            r = a.divide(b, q);

            if (r.intLen == 0)
                throw new ArithmeticException("BigInteger not invertible.");

            swapper = r;
            a = swapper;

            if (q.intLen == 1)
                t1.mul(q.value[q.offset], temp);
            else
                q.multiply(t1, temp);
            swapper = q;
            q = temp;
            temp = swapper;
            t0.add(q);

            if (a.isOne())
                return t0;

            r = b.divide(a, q);

            if (r.intLen == 0)
                throw new ArithmeticException("BigInteger not invertible.");

            swapper = b;
            b = r;

            if (q.intLen == 1)
                t0.mul(q.value[q.offset], temp);
            else
                q.multiply(t0, temp);
            swapper = q;
            q = temp;
            temp = swapper;

            t1.add(q);
        }
        mod.subtract(t1);
        return mod;
    }
}
