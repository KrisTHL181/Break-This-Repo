package net.MinecraftTools.Math.DynamicAccuracy;

import static net.MinecraftTools.Math.DynamicAccuracy.BigInteger.LONG_MASK;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

// MCRe NoiseFarlands: removed jdk.internal.* imports, using inline replacements

public class BigDecimal extends Number implements Comparable<BigDecimal> {
    // MCRe NoiseFarlands: JLA replaced with simple new String(byte[], charset)
    // 在 FarLandsConfigData 中加一个缓存控制
    public static boolean enablePerformanceOptimizations = true;

    private static final double L = 3.321928094887362;

    private static final int P_F = Float.PRECISION;
    private static final int Q_MIN_F = Float.MIN_EXPONENT - (P_F - 1);
    private static final int Q_MAX_F = Float.MAX_EXPONENT - (P_F - 1);

    private static final int P_D = Double.PRECISION;
    private static final int Q_MIN_D = (Double.MIN_EXPONENT - (P_D - 1));
    private static final int Q_MAX_D = (Double.MAX_EXPONENT - (P_D - 1));

    private final BigInteger intVal;

    private final int scale;

    private transient int precision;

    private transient String stringCache;

    static final long INFLATED = Long.MIN_VALUE;

    private static final BigInteger INFLATED_BIGINT = BigInteger.valueOf(INFLATED);

    private final transient long intCompact;

    private static final int MAX_COMPACT_DIGITS = 18;

    @java.io.Serial private static final long serialVersionUID = 6108874887143696463L;

    private static final BigDecimal[] ZERO_THROUGH_TEN = {
            new BigDecimal(BigInteger.ZERO, 0, 0, 1),
            new BigDecimal(BigInteger.ONE, 1, 0, 1),
            new BigDecimal(BigInteger.TWO, 2, 0, 1),
            new BigDecimal(BigInteger.valueOf(3), 3, 0, 1),
            new BigDecimal(BigInteger.valueOf(4), 4, 0, 1),
            new BigDecimal(BigInteger.valueOf(5), 5, 0, 1),
            new BigDecimal(BigInteger.valueOf(6), 6, 0, 1),
            new BigDecimal(BigInteger.valueOf(7), 7, 0, 1),
            new BigDecimal(BigInteger.valueOf(8), 8, 0, 1),
            new BigDecimal(BigInteger.valueOf(9), 9, 0, 1),
            new BigDecimal(BigInteger.TEN, 10, 0, 2),
    };

    private static final BigDecimal[] ZERO_SCALED_BY = {
            ZERO_THROUGH_TEN[0],
            new BigDecimal(BigInteger.ZERO, 0, 1, 1),
            new BigDecimal(BigInteger.ZERO, 0, 2, 1),
            new BigDecimal(BigInteger.ZERO, 0, 3, 1),
            new BigDecimal(BigInteger.ZERO, 0, 4, 1),
            new BigDecimal(BigInteger.ZERO, 0, 5, 1),
            new BigDecimal(BigInteger.ZERO, 0, 6, 1),
            new BigDecimal(BigInteger.ZERO, 0, 7, 1),
            new BigDecimal(BigInteger.ZERO, 0, 8, 1),
            new BigDecimal(BigInteger.ZERO, 0, 9, 1),
            new BigDecimal(BigInteger.ZERO, 0, 10, 1),
            new BigDecimal(BigInteger.ZERO, 0, 11, 1),
            new BigDecimal(BigInteger.ZERO, 0, 12, 1),
            new BigDecimal(BigInteger.ZERO, 0, 13, 1),
            new BigDecimal(BigInteger.ZERO, 0, 14, 1),
            new BigDecimal(BigInteger.ZERO, 0, 15, 1),
    };

    private static final long HALF_LONG_MAX_VALUE = Long.MAX_VALUE / 2;
    private static final long HALF_LONG_MIN_VALUE = Long.MIN_VALUE / 2;

    public static final BigDecimal ZERO = ZERO_THROUGH_TEN[0];

    public static final BigDecimal ONE = ZERO_THROUGH_TEN[1];

    public static final BigDecimal TWO = ZERO_THROUGH_TEN[2];

    public static final BigDecimal TEN = ZERO_THROUGH_TEN[10];

    BigDecimal(BigInteger intVal, long val, int scale, int prec) {
        this.scale = scale;
        this.precision = prec;
        this.intCompact = val;
        this.intVal = intVal;
    }

    public BigDecimal(char[] in, int offset, int len) {
        this(in, offset, len, MathContext.UNLIMITED);
    }

    public BigDecimal(char[] in, int offset, int len, MathContext mc) {

        try {
            Objects.checkFromIndexSize(offset, len, in.length);
        } catch (IndexOutOfBoundsException e) {
            throw new NumberFormatException("Bad offset or len arguments for char[] input.");
        }

        int prec = 0;
        long scl = 0;
        long rs = 0;
        BigInteger rb = null;

        try {

            boolean isneg = false;
            if (in[offset] == '-') {
                isneg = true;
                offset++;
                len--;
            } else if (in[offset] == '+') {
                offset++;
                len--;
            }

            boolean dot = false;
            char c;
            boolean isCompact = (len <= MAX_COMPACT_DIGITS);

            int idx = 0;
            if (isCompact) {

                for (; len > 0; offset++, len--) {
                    c = in[offset];
                    if ((c == '0')) {
                        if (prec == 0)
                            prec = 1;
                        else if (rs != 0) {
                            rs *= 10;
                            ++prec;
                        }
                        if (dot)
                            ++scl;
                    } else if ((c >= '1' && c <= '9')) {
                        int digit = c - '0';
                        if (prec != 1 || rs != 0)
                            ++prec;
                        rs = rs * 10 + digit;
                        if (dot)
                            ++scl;
                    } else if (c == '.') {

                        if (dot)
                            throw new NumberFormatException("Character array"
                                    + " contains more than one decimal point.");
                        dot = true;
                    } else if (Character.isDigit(c)) {
                        int digit = Character.digit(c, 10);
                        if (digit == 0) {
                            if (prec == 0)
                                prec = 1;
                            else if (rs != 0) {
                                rs *= 10;
                                ++prec;
                            }
                        } else {
                            if (prec != 1 || rs != 0)
                                ++prec;
                            rs = rs * 10 + digit;
                        }
                        if (dot)
                            ++scl;
                    } else if ((c == 'e') || (c == 'E')) {
                        scl -= parseExp(in, offset, len);
                        break;
                    } else {
                        throw new NumberFormatException("Character " + c
                                + " is neither a decimal digit number, decimal point, nor"
                                + " \"e\" notation exponential mark.");
                    }
                }
                if (prec == 0)
                    throw new NumberFormatException("No digits found.");
                rs = isneg ? -rs : rs;
                int mcp = mc.precision;
                int drop = prec - mcp;

                if (mcp > 0 && drop > 0) {
                    while (drop > 0) {
                        scl -= drop;
                        rs = divideAndRound(rs, LONG_TEN_POWERS_TABLE[
                        drop], mc.roundingMode.oldMode);
                        prec = longDigitLength(rs);
                        drop = prec - mcp;
                    }
                }
            } else {
                char[] coeff = new char[len];
                for (; len > 0; offset++, len--) {
                    c = in[offset];

                    if ((c >= '0' && c <= '9') || Character.isDigit(c)) {

                        if (c == '0' || Character.digit(c, 10) == 0) {
                            if (prec == 0) {
                                coeff[idx] = c;
                                prec = 1;
                            } else if (idx != 0) {
                                coeff[idx++] = c;
                                ++prec;
                            }
                        } else {
                            if (prec != 1 || idx != 0)
                                ++prec;
                            coeff[idx++] = c;
                        }
                        if (dot)
                            ++scl;
                        continue;
                    }

                    if (c == '.') {

                        if (dot)
                            throw new NumberFormatException("Character array"
                                    + " contains more than one decimal point.");
                        dot = true;
                        continue;
                    }

                    if ((c != 'e') && (c != 'E'))
                        throw new NumberFormatException("Character array"
                                + " is missing \"e\" notation exponential mark.");
                    scl -= parseExp(in, offset, len);
                    break;
                }

                if (prec == 0)
                    throw new NumberFormatException("No digits found.");

                rb = new BigInteger(coeff, isneg ? -1 : 1, prec);
                rs = compactValFor(rb);
                int mcp = mc.precision;
                if (mcp > 0 && (prec > mcp)) {
                    if (rs == INFLATED) {
                        int drop = prec - mcp;
                        while (drop > 0) {
                            scl -= drop;
                            rb = divideAndRoundByTenPow(rb, drop, mc.roundingMode.oldMode);
                            rs = compactValFor(rb);
                            if (rs != INFLATED) {
                                prec = longDigitLength(rs);
                                break;
                            }
                            prec = bigDigitLength(rb);
                            drop = prec - mcp;
                        }
                    }
                    if (rs != INFLATED) {
                        int drop = prec - mcp;
                        while (drop > 0) {
                            scl -= drop;
                            rs = divideAndRound(rs, LONG_TEN_POWERS_TABLE[
                            drop], mc.roundingMode.oldMode);
                            prec = longDigitLength(rs);
                            drop = prec - mcp;
                        }
                        rb = null;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
            NumberFormatException nfe = new NumberFormatException();
            nfe.initCause(e);
            throw nfe;
        }
        if ((int) scl != scl)
            throw new NumberFormatException("Exponent overflow.");
        this.scale = (int) scl;
        this.precision = prec;
        this.intCompact = rs;
        this.intVal = rb;
    }

    private static long parseExp(char[] in, int offset, int len) {
        long exp = 0;
        offset++;
        char c = in[offset];
        len--;
        boolean negexp = (c == '-');

        if (negexp || c == '+') {
            offset++;
            c = in[offset];
            len--;
        }
        if (len <= 0)
            throw new NumberFormatException("No exponent digits.");

        while (len > 10 && (c == '0' || (Character.digit(c, 10) == 0))) {
            offset++;
            c = in[offset];
            len--;
        }
        if (len > 10)
            throw new NumberFormatException("Too many nonzero exponent digits.");

        for (; ; len--) {
            int v;
            if (c >= '0' && c <= '9') {
                v = c - '0';
            } else {
                v = Character.digit(c, 10);
                if (v < 0)
                    throw new NumberFormatException("Not a digit.");
            }
            exp = exp * 10 + v;
            if (len == 1)
                break;
            offset++;
            c = in[offset];
        }
        if (negexp)
            exp = -exp;
        return exp;
    }

    public BigDecimal(char[] in) {
        this(in, 0, in.length);
    }

    public BigDecimal(char[] in, MathContext mc) {
        this(in, 0, in.length, mc);
    }

    public BigDecimal(String val) {
        this(val.toCharArray(), 0, val.length());
    }

    public BigDecimal(String val, MathContext mc) {
        this(val.toCharArray(), 0, val.length(), mc);
    }

    public BigDecimal(double val) {
        this(val, MathContext.UNLIMITED);
    }

    public BigDecimal(double val, MathContext mc) {
        if (Double.isInfinite(val) || Double.isNaN(val))
            throw new NumberFormatException("Infinite or NaN");

        long valBits = Double.doubleToLongBits(val);
        int sign = ((valBits >> 63) == 0 ? 1 : -1);
        int exponent = (int) ((valBits >> 52) & 0x7ffL);
        long significand = (exponent == 0
                ? (valBits & ((1L << 52) - 1)) << 1
                : (valBits & ((1L << 52) - 1)) | (1L << 52));
        exponent -= 1075;

        if (significand == 0) {
            this.intVal = BigInteger.ZERO;
            this.scale = 0;
            this.intCompact = 0;
            this.precision = 1;
            return;
        }

        while ((significand & 1) == 0) {
            significand >>= 1;
            exponent++;
        }
        int scl = 0;

        BigInteger rb;
        long compactVal = sign * significand;
        if (exponent == 0) {
            rb = (compactVal == INFLATED) ? INFLATED_BIGINT : null;
        } else {
            if (exponent < 0) {
                rb = BigInteger.valueOf(5).pow(-exponent).multiply(compactVal);
                scl = -exponent;
            } else {
                rb = BigInteger.TWO.pow(exponent).multiply(compactVal);
            }
            compactVal = compactValFor(rb);
        }
        int prec = 0;
        int mcp = mc.precision;
        if (mcp > 0) {
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(rb);
                drop = prec - mcp;
                while (drop > 0) {
                    scl = checkScaleNonZero((long) scl - drop);
                    rb = divideAndRoundByTenPow(rb, drop, mode);
                    compactVal = compactValFor(rb);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(rb);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scl = checkScaleNonZero((long) scl - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[
                    drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                rb = null;
            }
        }
        this.intVal = rb;
        this.intCompact = compactVal;
        this.scale = scl;
        this.precision = prec;
    }

    private static BigInteger toStrictBigInteger(BigInteger val) {
        return (val.getClass() == BigInteger.class) ? val : new BigInteger(val.toByteArray().clone());
    }

    public BigDecimal(BigInteger val) {
        scale = 0;
        intVal = toStrictBigInteger(val);
        intCompact = compactValFor(intVal);
    }

    public BigDecimal(BigInteger val, MathContext mc) {
        this(toStrictBigInteger(val), 0, mc);
    }

    public BigDecimal(BigInteger unscaledVal, int scale) {

        this.intVal = toStrictBigInteger(unscaledVal);
        this.intCompact = compactValFor(this.intVal);
        this.scale = scale;
    }

    public BigDecimal(BigInteger unscaledVal, int scale, MathContext mc) {
        unscaledVal = toStrictBigInteger(unscaledVal);
        long compactVal = compactValFor(unscaledVal);
        int mcp = mc.precision;
        int prec = 0;
        if (mcp > 0) {
            int mode = mc.roundingMode.oldMode;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(unscaledVal);
                int drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    unscaledVal = divideAndRoundByTenPow(unscaledVal, drop, mode);
                    compactVal = compactValFor(unscaledVal);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(unscaledVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                int drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                unscaledVal = null;
            }
        }
        this.intVal = unscaledVal;
        this.intCompact = compactVal;
        this.scale = scale;
        this.precision = prec;
    }

    public BigDecimal(int val) {
        this.intCompact = val;
        this.scale = 0;
        this.intVal = null;
    }

    public BigDecimal(int val, MathContext mc) {
        int mcp = mc.precision;
        long compactVal = val;
        int scl = 0;
        int prec = 0;
        if (mcp > 0) {
            prec = longDigitLength(compactVal);
            int drop = prec - mcp;
            while (drop > 0) {
                scl = checkScaleNonZero((long) scl - drop);
                compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[
                drop], mc.roundingMode.oldMode);
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
            }
        }
        this.intVal = null;
        this.intCompact = compactVal;
        this.scale = scl;
        this.precision = prec;
    }

    public BigDecimal(long val) {
        this.intCompact = val;
        this.intVal = (val == INFLATED) ? INFLATED_BIGINT : null;
        this.scale = 0;
    }

    public BigDecimal(long val, MathContext mc) {
        int mcp = mc.precision;
        int mode = mc.roundingMode.oldMode;
        int prec = 0;
        int scl = 0;
        BigInteger rb = (val == INFLATED) ? INFLATED_BIGINT : null;
        if (mcp > 0) {
            if (val == INFLATED) {
                prec = 19;
                int drop = prec - mcp;
                while (drop > 0) {
                    scl = checkScaleNonZero((long) scl - drop);
                    rb = divideAndRoundByTenPow(rb, drop, mode);
                    val = compactValFor(rb);
                    if (val != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(rb);
                    drop = prec - mcp;
                }
            }
            if (val != INFLATED) {
                prec = longDigitLength(val);
                int drop = prec - mcp;
                while (drop > 0) {
                    scl = checkScaleNonZero((long) scl - drop);
                    val = divideAndRound(val, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(val);
                    drop = prec - mcp;
                }
                rb = null;
            }
        }
        this.intVal = rb;
        this.intCompact = val;
        this.scale = scl;
        this.precision = prec;
    }

    public static BigDecimal valueOf(long unscaledVal, int scale) {
        if (scale == 0)
            return valueOf(unscaledVal);
        else if (unscaledVal == 0) {
            return zeroValueOf(scale);
        }
        return new BigDecimal(unscaledVal == INFLATED ? INFLATED_BIGINT : null,
        unscaledVal, scale, 0);
    }

    public static BigDecimal valueOf(long val) {
        if (val >= 0 && val < ZERO_THROUGH_TEN.length)
            return ZERO_THROUGH_TEN[(int) val];
        else if (val != INFLATED)
            return new BigDecimal(null, val, 0, 0);
        return new BigDecimal(INFLATED_BIGINT, val, 0, 0);
    }

    static BigDecimal valueOf(long unscaledVal, int scale, int prec) {
        if (scale == 0 && unscaledVal >= 0 && unscaledVal < ZERO_THROUGH_TEN.length) {
            return ZERO_THROUGH_TEN[(int) unscaledVal];
        } else if (unscaledVal == 0) {
            return zeroValueOf(scale);
        }
        return new BigDecimal(unscaledVal == INFLATED ? INFLATED_BIGINT : null,
        unscaledVal, scale, prec);
    }

    static BigDecimal valueOf(BigInteger intVal, int scale, int prec) {
        long val = compactValFor(intVal);
        if (val == 0) {
            return zeroValueOf(scale);
        } else if (scale == 0 && val >= 0 && val < ZERO_THROUGH_TEN.length) {
            return ZERO_THROUGH_TEN[(int) val];
        }
        return new BigDecimal(intVal, val, scale, prec);
    }

    static BigDecimal zeroValueOf(int scale) {
        if (scale >= 0 && scale < ZERO_SCALED_BY.length)
            return ZERO_SCALED_BY[scale];
        else
            return new BigDecimal(BigInteger.ZERO, 0, scale, 1);
    }

    public static BigDecimal valueOf(double val) {
        if (!Double.isFinite(val)) {
            throw new NumberFormatException("Infinite or NaN");
        }

        // 0.0/-0.0 走字符串路径，保持原语义（scale=1 的 "0.0"）
        if (val == 0.0) {
            return new BigDecimal(Double.toString(val));
        }

        // 🔧 MCRe：整数 double 走 IEEE 754 位精确转换，绕开 Double.toString 的科学记数法截断
        // 问题：Double.toString 对大整数用科学记数法只保留 17 位有效数字
        //      例如 Double.toString(9223372036854775808.0) = "9.223372036854776E18"（最后 4 位丢失）
        //      导致 BigDecimal 偏移缩放全链路（如 WorldReposition/DebugEntryPosition）在地形坐标 ≥ 2^53 时精度崩盘
        // 解决：位解析路径 sign × significand × 2^scaleExp 精确重建，对任意大小整数 double 都精确
        if (val == Math.rint(val)) {
            long bits = Double.doubleToRawLongBits(val);
            boolean neg = (bits >>> 63) != 0;
            int expBits = (int) ((bits >>> 52) & 0x7FF);
            long mantBits = bits & 0x000F_FFFF_FFFF_FFFFL; // 或者 (1L << 52) - 1
            long significand = mantBits | (1L << 52); // 53-bit 含隐含位，long 范围
            int scaleExp = expBits - 1075; // 值 = sign × significand × 2^scaleExp
            BigInteger mant = BigInteger.valueOf(neg ? -significand : significand);
            if (scaleExp >= 0) {
                return new BigDecimal(mant.shiftLeft(scaleExp));
            } else {
                // 值 = mant × 5^(-scaleExp) × 10^(scaleExp)，scale = -scaleExp
                BigInteger unscaled = mant.multiply(BigInteger.valueOf(5).pow(-scaleExp));
                return new BigDecimal(unscaled, -scaleExp);
            }
        }

        // 小数 double：Double.toString 无损（17 位有效数字是 double 自身精度上限）
        String ds = Double.toString(val);
        return new BigDecimal(ds);
    }

    public BigDecimal add(BigDecimal augend) {
        if (this.intCompact != INFLATED) {
            if ((augend.intCompact != INFLATED)) {
                return add(this.intCompact, this.scale, augend.intCompact, augend.scale);
            } else {
                return add(this.intCompact, this.scale, augend.intVal, augend.scale);
            }
        } else {
            if ((augend.intCompact != INFLATED)) {
                return add(augend.intCompact, augend.scale, this.intVal, this.scale);
            } else {
                return add(this.intVal, this.scale, augend.intVal, augend.scale);
            }
        }
    }

    public BigDecimal add(BigDecimal augend, MathContext mc) {
        if (mc.precision == 0)
            return add(augend);
        BigDecimal lhs = this;

        {
            boolean lhsIsZero = lhs.signum() == 0;
            boolean augendIsZero = augend.signum() == 0;

            if (lhsIsZero || augendIsZero) {
                int preferredScale = Math.max(lhs.scale(), augend.scale());
                BigDecimal result;

                if (lhsIsZero && augendIsZero)
                    return zeroValueOf(preferredScale);
                result = lhsIsZero ? doRound(augend, mc) : doRound(lhs, mc);

                if (result.scale() == preferredScale)
                    return result;
                else if (result.scale() > preferredScale) {
                    return stripZerosToMatchScale(result.intVal, result.intCompact, result.scale, preferredScale);
                } else {
                    int precisionDiff = mc.precision - result.precision();
                    int scaleDiff = preferredScale - result.scale();

                    if (precisionDiff >= scaleDiff)
                        return result.setScale(preferredScale);
                    else
                        return result.setScale(result.scale() + precisionDiff);
                }
            }
        }

        long padding = (long) lhs.scale - augend.scale;
        if (padding != 0) {
            BigDecimal[] arg = preAlign(lhs, augend, padding, mc);
            matchScale(arg);
            lhs = arg[0];
            augend = arg[1];
        }
        return doRound(lhs.inflated().add(augend.inflated()), lhs.scale, mc);
    }

    private BigDecimal[] preAlign(BigDecimal lhs, BigDecimal augend, long padding, MathContext mc) {
        assert padding != 0;
        BigDecimal big;
        BigDecimal small;

        if (padding < 0) {
            big = lhs;
            small = augend;
        } else {
            big = augend;
            small = lhs;
        }

        long estResultUlpScale = (long) big.scale - big.precision() + mc.precision;

        long smallHighDigitPos = (long) small.scale - small.precision() + 1;
        if (smallHighDigitPos > big.scale + 2 &&
                smallHighDigitPos > estResultUlpScale + 2) {
            small = BigDecimal.valueOf(small.signum(), this.checkScale(Math.max(big.scale, estResultUlpScale) + 3));
        }

        BigDecimal[] result = {big, small};
        return result;
    }

    public BigDecimal subtract(BigDecimal subtrahend) {
        if (this.intCompact != INFLATED) {
            if ((subtrahend.intCompact != INFLATED)) {
                return add(this.intCompact, this.scale, -subtrahend.intCompact, subtrahend.scale);
            } else {
                return add(this.intCompact, this.scale, subtrahend.intVal.negate(), subtrahend.scale);
            }
        } else {
            if ((subtrahend.intCompact != INFLATED)) {

                return add(-subtrahend.intCompact, subtrahend.scale, this.intVal, this.scale);
            } else {
                return add(this.intVal, this.scale, subtrahend.intVal.negate(), subtrahend.scale);
            }
        }
    }

    public BigDecimal subtract(BigDecimal subtrahend, MathContext mc) {
        if (mc.precision == 0)
            return subtract(subtrahend);

        return add(subtrahend.negate(), mc);
    }

    public BigDecimal multiply(BigDecimal multiplicand) {
        int productScale = checkScale((long) scale + multiplicand.scale);
        if (this.intCompact != INFLATED) {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiply(this.intCompact, multiplicand.intCompact, productScale);
            } else {
                return multiply(this.intCompact, multiplicand.intVal, productScale);
            }
        } else {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiply(multiplicand.intCompact, this.intVal, productScale);
            } else {
                return multiply(this.intVal, multiplicand.intVal, productScale);
            }
        }
    }

    public BigDecimal multiply(BigDecimal multiplicand, MathContext mc) {
        if (mc.precision == 0)
            return multiply(multiplicand);
        int productScale = checkScale((long) scale + multiplicand.scale);
        if (this.intCompact != INFLATED) {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiplyAndRound(this.intCompact, multiplicand.intCompact, productScale, mc);
            } else {
                return multiplyAndRound(this.intCompact, multiplicand.intVal, productScale, mc);
            }
        } else {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiplyAndRound(multiplicand.intCompact, this.intVal, productScale, mc);
            } else {
                return multiplyAndRound(this.intVal, multiplicand.intVal, productScale, mc);
            }
        }
    }

    @Deprecated(since = "9")
    public BigDecimal divide(BigDecimal divisor, int scale, int roundingMode) {
        if (roundingMode < ROUND_UP || roundingMode > ROUND_UNNECESSARY)
            throw new IllegalArgumentException("Invalid rounding mode");
        if (this.intCompact != INFLATED) {
            if ((divisor.intCompact != INFLATED)) {
                return divide(this.intCompact, this.scale, divisor.intCompact, divisor.scale, scale, roundingMode);
            } else {
                return divide(this.intCompact, this.scale, divisor.intVal, divisor.scale, scale, roundingMode);
            }
        } else {
            if ((divisor.intCompact != INFLATED)) {
                return divide(this.intVal, this.scale, divisor.intCompact, divisor.scale, scale, roundingMode);
            } else {
                return divide(this.intVal, this.scale, divisor.intVal, divisor.scale, scale, roundingMode);
            }
        }
    }

    public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode) {
        return divide(divisor, scale, roundingMode.oldMode);
    }

    @Deprecated(since = "9")
    public BigDecimal divide(BigDecimal divisor, int roundingMode) {
        return this.divide(divisor, scale, roundingMode);
    }

    public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode) {
        return this.divide(divisor, scale, roundingMode.oldMode);
    }

    public BigDecimal divide(BigDecimal divisor) {

        if (divisor.signum() == 0) {
            if (this.signum() == 0)
                throw new ArithmeticException("Division undefined");
            throw new ArithmeticException("Division by zero");
        }

        int preferredScale = saturateLong((long) this.scale - divisor.scale);

        if (this.signum() == 0)
            return zeroValueOf(preferredScale);
        else {

            // 🔧 池化：原 new MathContext((int) Math.min(...), RoundingMode.UNNECESSARY)
            MathContext mc = MathContext.getCached(
                    (int) Math.min(this.precision() +
                                    (long) Math.ceil(10.0 * divisor.precision() / 3.0),
                                    Integer.MAX_VALUE),
                    RoundingMode.UNNECESSARY);
            BigDecimal quotient;
            try {
                quotient = this.divide(divisor, mc);
            } catch (ArithmeticException e) {
                throw new ArithmeticException("Non-terminating decimal expansion; " +
                        "no exact representable decimal result.");
            }

            int quotientScale = quotient.scale();

            if (preferredScale > quotientScale)
                return quotient.setScale(preferredScale, ROUND_UNNECESSARY);

            return quotient;
        }
    }

    public BigDecimal divide(BigDecimal divisor, MathContext mc) {
        int mcp = mc.precision;
        if (mcp == 0)
            return divide(divisor);

        BigDecimal dividend = this;
        long preferredScale = (long) dividend.scale - divisor.scale;

        if (divisor.signum() == 0) {
            if (dividend.signum() == 0)
                throw new ArithmeticException("Division undefined");
            throw new ArithmeticException("Division by zero");
        }
        if (dividend.signum() == 0)
            return zeroValueOf(saturateLong(preferredScale));
        int xscale = dividend.precision();
        int yscale = divisor.precision();
        if (dividend.intCompact != INFLATED) {
            if (divisor.intCompact != INFLATED) {
                return divide(dividend.intCompact, xscale, divisor.intCompact, yscale, preferredScale, mc);
            } else {
                return divide(dividend.intCompact, xscale, divisor.intVal, yscale, preferredScale, mc);
            }
        } else {
            if (divisor.intCompact != INFLATED) {
                return divide(dividend.intVal, xscale, divisor.intCompact, yscale, preferredScale, mc);
            } else {
                return divide(dividend.intVal, xscale, divisor.intVal, yscale, preferredScale, mc);
            }
        }
    }

    public BigDecimal divideToIntegralValue(BigDecimal divisor) {

        int preferredScale = saturateLong((long) this.scale - divisor.scale);
        if (this.compareMagnitude(divisor) < 0) {

            return zeroValueOf(preferredScale);
        }

        if (this.signum() == 0 && divisor.signum() != 0)
            return this.setScale(preferredScale, ROUND_UNNECESSARY);

        int maxDigits = (int) Math.min(this.precision() +
                        (long) Math.ceil(10.0 * divisor.precision() / 3.0) +
                        Math.abs((long) this.scale() - divisor.scale()) + 2,
                        Integer.MAX_VALUE);
        // 🔧 池化：原 new MathContext(maxDigits, RoundingMode.DOWN)
        BigDecimal quotient = this.divide(divisor, MathContext.getCached(maxDigits,
                RoundingMode.DOWN));
        if (quotient.scale > 0) {
            quotient = quotient.setScale(0, RoundingMode.DOWN);
            quotient = stripZerosToMatchScale(quotient.intVal, quotient.intCompact, quotient.scale, preferredScale);
        }

        if (quotient.scale < preferredScale) {

            quotient = quotient.setScale(preferredScale, ROUND_UNNECESSARY);
        }

        return quotient;
    }

    public BigDecimal divideToIntegralValue(BigDecimal divisor, MathContext mc) {
        if (mc.precision == 0 ||
                (this.compareMagnitude(divisor) < 0))
            return divideToIntegralValue(divisor);

        int preferredScale = saturateLong((long) this.scale - divisor.scale);

        // 🔧 池化：原 new MathContext(mc.precision, RoundingMode.DOWN)
        BigDecimal result = this.divide(divisor, MathContext.getCached(mc.precision, RoundingMode.DOWN));

        if (result.scale() < 0) {

            BigDecimal product = result.multiply(divisor);

            if (this.subtract(product).compareMagnitude(divisor) >= 0) {
                throw new ArithmeticException("Division impossible");
            }
        } else if (result.scale() > 0) {

            result = result.setScale(0, RoundingMode.DOWN);
        }

        int precisionDiff;
        if ((preferredScale > result.scale()) &&
                (precisionDiff = mc.precision - result.precision()) > 0) {
            return result.setScale(result.scale() +
                    Math.min(precisionDiff, preferredScale - result.scale));
        } else {
            return stripZerosToMatchScale(result.intVal, result.intCompact, result.scale, preferredScale);
        }
    }

    public BigDecimal remainder(BigDecimal divisor) {
        BigDecimal[] divrem = this.divideAndRemainder(divisor);
        return divrem[1];
    }

    public BigDecimal remainder(BigDecimal divisor, MathContext mc) {
        BigDecimal[] divrem = this.divideAndRemainder(divisor, mc);
        return divrem[1];
    }

    public BigDecimal[] divideAndRemainder(BigDecimal divisor) {

        BigDecimal[] result = new BigDecimal[2];

        result[0] = this.divideToIntegralValue(divisor);
        result[1] = this.subtract(result[0].multiply(divisor));
        return result;
    }

    public BigDecimal[] divideAndRemainder(BigDecimal divisor, MathContext mc) {
        if (mc.precision == 0)
            return divideAndRemainder(divisor);

        BigDecimal[] result = new BigDecimal[2];
        BigDecimal lhs = this;

        result[0] = lhs.divideToIntegralValue(divisor, mc);
        result[1] = lhs.subtract(result[0].multiply(divisor));
        return result;
    }

    public BigDecimal sqrt(MathContext mc) {
        final int signum = signum();
        if (signum != 1) {
            switch (signum) {
                case -1 ->
                        throw new ArithmeticException("Attempted square root of negative BigDecimal");
                case 0 -> {
                    BigDecimal result = valueOf(0L, scale / 2);
                    assert squareRootResultAssertions(result, mc);
                    return result;
                }
                default -> throw new AssertionError("Bad value from signum");
            }
        }

        final int preferredScale = this.scale / 2;

        BigDecimal result;
        if (mc.roundingMode == RoundingMode.UNNECESSARY || mc.precision == 0) {

            final BigDecimal stripped = this.stripTrailingZeros();
            final int strippedScale = stripped.scale;

            if ((strippedScale & 1) != 0)
                throw new ArithmeticException("Computed square root not exact.");

            if (stripped.isPowerOfTen()) {
                result = valueOf(1L, strippedScale >> 1);

                return result.adjustToPreferredScale(preferredScale, mc.precision);
            }

            BigInteger[] sqrtRem = stripped.unscaledValue().sqrtAndRemainder();
            result = new BigDecimal(sqrtRem[0], strippedScale >> 1);

            if (sqrtRem[1].signum != 0 || mc.precision != 0 && result.precision() > mc.precision)
                throw new ArithmeticException("Computed square root not exact.");

            assert squareRootResultAssertions(result, mc);

            return result.adjustToPreferredScale(preferredScale, mc.precision);
        }

        final boolean halfWay = isHalfWay(mc.roundingMode);

        final long minWorkingPrec = ((mc.precision + (halfWay ? 1L : 0L)) << 1) - 1L;

        long normScale = minWorkingPrec - this.precision() + this.scale;
        normScale += normScale & 1L;

        final long workingScale = this.scale - normScale;
        if (workingScale != (int) workingScale)
            throw new ArithmeticException("Overflow");

        BigDecimal working = new BigDecimal(this.intVal, this.intCompact, (int) workingScale, this.precision);
        BigInteger workingInt = working.toBigInteger();

        BigInteger sqrt;
        long resultScale = normScale >> 1;

        if (halfWay) {
            BigInteger workingSqrt = workingInt.sqrt();

            BigInteger[] quotRem10 = workingSqrt.divideAndRemainder(BigInteger.TEN);
            sqrt = quotRem10[0];
            resultScale--;

            boolean increment = false;
            int digit = quotRem10[1].intValue();
            if (digit > 5) {
                increment = true;
            } else if (digit == 5) {
                if (mc.roundingMode == RoundingMode.HALF_UP
                        || mc.roundingMode == RoundingMode.HALF_EVEN && sqrt.testBit(0)
                        || !workingInt.equals(workingSqrt.multiply(workingSqrt))
                        || !working.isInteger()) {
                    increment = true;
                }
            }

            if (increment)
                sqrt = sqrt.add(1L);
        } else {
            switch (mc.roundingMode) {
                case DOWN, FLOOR -> sqrt = workingInt.sqrt();

                case UP, CEILING -> {
                    BigInteger[] sqrtRem = workingInt.sqrtAndRemainder();
                    sqrt = sqrtRem[0];

                    if (sqrtRem[1].signum != 0 || !working.isInteger())
                        sqrt = sqrt.add(1L);
                }

                default ->
                        throw new AssertionError("Unexpected value for RoundingMode: " + mc.roundingMode);
            }
        }

        result = new BigDecimal(sqrt, checkScale(sqrt, resultScale), mc);

        assert squareRootResultAssertions(result, mc);

        if (result.scale > preferredScale)
            result = stripZerosToMatchScale(result.intVal, result.intCompact, result.scale, preferredScale);

        return result;
    }

    private BigDecimal adjustToPreferredScale(int preferredScale, int maxPrecision) {
        BigDecimal result = this;
        if (result.scale > preferredScale) {
            result = stripZerosToMatchScale(result.intVal, result.intCompact, result.scale, preferredScale);
        } else if (result.scale < preferredScale) {
            int maxScale = maxPrecision == 0 ? preferredScale : (int) Math.min(preferredScale, result.scale + (long) (maxPrecision - result.precision()));
            result = result.setScale(maxScale);
        }
        return result;
    }

    private static boolean isHalfWay(RoundingMode m) {
        return switch (m) {
            case HALF_DOWN, HALF_UP, HALF_EVEN -> true;
            case FLOOR, CEILING, DOWN, UP, UNNECESSARY -> false;
        };
    }

    private BigDecimal square() {
        return this.multiply(this);
    }

    private boolean isPowerOfTen() {
        return BigInteger.ONE.equals(this.unscaledValue());
    }

    private boolean squareRootResultAssertions(BigDecimal result, MathContext mc) {
        if (result.signum() == 0) {
            return squareRootZeroResultAssertions(result, mc);
        } else {
            RoundingMode rm = mc.getRoundingMode();
            BigDecimal ulp = result.ulp();
            BigDecimal neighborUp = result.add(ulp);

            if (result.isPowerOfTen()) {
                ulp = ulp.divide(TEN);
            }
            BigDecimal neighborDown = result.subtract(ulp);

            assert (result.signum() == 1 &&
                            this.signum() == 1)
                    : "Bad signum of this and/or its sqrt.";

            switch (rm) {
                case DOWN:
                case FLOOR:
                    assert result.square().compareTo(this) <= 0 &&
                                    neighborUp.square().compareTo(this) > 0
                            : "Square of result out for bounds rounding " + rm;
                    return true;

                case UP:
                case CEILING:
                    assert result.square().compareTo(this) >= 0 &&
                                    neighborDown.square().compareTo(this) < 0
                            : "Square of result out for bounds rounding " + rm;
                    return true;

                case HALF_DOWN:
                case HALF_EVEN:
                case HALF_UP:
                    BigDecimal err = result.square().subtract(this).abs();
                    BigDecimal errUp = neighborUp.square().subtract(this);
                    BigDecimal errDown = this.subtract(neighborDown.square());

                    int err_comp_errUp = err.compareTo(errUp);
                    int err_comp_errDown = err.compareTo(errDown);

                    assert errUp.signum() == 1 &&
                                    errDown.signum() == 1
                            : "Errors of neighbors squared don't have correct signs";

                    assert err_comp_errUp <= 0 ||
                                    err_comp_errDown <= 0
                            : "Computed square root has larger error than neighbors for " + rm;

                    assert ((err_comp_errUp == 0) ? err_comp_errDown < 0 : true) &&
                                    ((err_comp_errDown == 0) ? err_comp_errUp < 0 : true)
                            : "Incorrect error relationships";

                    return true;

                default:
                    return true;
            }
        }
    }

    private boolean squareRootZeroResultAssertions(BigDecimal result, MathContext mc) {
        return this.compareTo(ZERO) == 0;
    }

    public BigDecimal pow(int n) {
        if (n < 0 || n > 999999999)
            throw new ArithmeticException("Invalid operation");

        int newScale = checkScale((long) scale * n);
        return new BigDecimal(this.inflated().pow(n), newScale);
    }

    public BigDecimal pow(int n, MathContext mc) {
        if (mc.precision == 0)
            return pow(n);
        if (n < -999999999 || n > 999999999)
            throw new ArithmeticException("Invalid operation");
        if (n == 0)
            return ONE;
        BigDecimal lhs = this;
        MathContext workmc = mc;
        int mag = Math.abs(n);
        if (mc.precision > 0) {
            int elength = longDigitLength(mag);
            if (elength > mc.precision)
                throw new ArithmeticException("Invalid operation");
            // 🔧 池化：原 new MathContext(mc.precision + elength + 1, mc.roundingMode)
            workmc = MathContext.getCached(mc.precision + elength + 1, mc.roundingMode);
        }

        BigDecimal acc = ONE;
        boolean seenbit = false;
        for (int i = 1; ; i++) {
            mag += mag;
            if (mag < 0) {
                seenbit = true;
                acc = acc.multiply(lhs, workmc);
            }
            if (i == 31)
                break;
            if (seenbit)
                acc = acc.multiply(acc, workmc);
        }

        if (n < 0)
            acc = ONE.divide(acc, workmc);

        return doRound(acc, mc);
    }

    public BigDecimal abs() {
        return (signum() < 0 ? negate() : this);
    }

    public BigDecimal abs(MathContext mc) {
        return (signum() < 0 ? negate(mc) : plus(mc));
    }

    public BigDecimal negate() {
        if (intCompact == INFLATED) {
            return new BigDecimal(intVal.negate(), INFLATED, scale, precision);
        } else {
            return valueOf(-intCompact, scale, precision);
        }
    }

    public BigDecimal negate(MathContext mc) {
        return negate().plus(mc);
    }

    public BigDecimal plus() {
        return this;
    }

    public BigDecimal plus(MathContext mc) {
        if (mc.precision == 0)
            return this;
        return doRound(this, mc);
    }

    public int signum() {
        return (intCompact != INFLATED) ? Long.signum(intCompact) : intVal.signum();
    }

    public int scale() {
        return scale;
    }

    public int precision() {
        int result = precision;
        if (result == 0) {
            long s = intCompact;
            if (s != INFLATED)
                result = longDigitLength(s);
            else
                result = bigDigitLength(intVal);
            precision = result;
        }
        return result;
    }

    public BigInteger unscaledValue() {
        return this.inflated();
    }

    @Deprecated(since = "9")
    public static final int ROUND_UP = 0;

    @Deprecated(since = "9")
    public static final int ROUND_DOWN = 1;

    @Deprecated(since = "9")
    public static final int ROUND_CEILING = 2;

    @Deprecated(since = "9")
    public static final int ROUND_FLOOR = 3;

    @Deprecated(since = "9")
    public static final int ROUND_HALF_UP = 4;

    @Deprecated(since = "9")
    public static final int ROUND_HALF_DOWN = 5;

    @Deprecated(since = "9")
    public static final int ROUND_HALF_EVEN = 6;

    @Deprecated(since = "9")
    public static final int ROUND_UNNECESSARY = 7;

    public BigDecimal round(MathContext mc) {
        return plus(mc);
    }

    public BigDecimal setScale(int newScale, RoundingMode roundingMode) {
        return setScale(newScale, roundingMode.oldMode);
    }

    @Deprecated(since = "9")
    public BigDecimal setScale(int newScale, int roundingMode) {
        if (roundingMode < ROUND_UP || roundingMode > ROUND_UNNECESSARY)
            throw new IllegalArgumentException("Invalid rounding mode");

        int oldScale = this.scale;
        if (newScale == oldScale)
            return this;
        if (this.signum() == 0)
            return zeroValueOf(newScale);
        if (this.intCompact != INFLATED) {
            long rs = this.intCompact;
            if (newScale > oldScale) {
                int raise = checkScale((long) newScale - oldScale);
                if ((rs = longMultiplyPowerTen(rs, raise)) != INFLATED) {
                    return valueOf(rs, newScale);
                }
                BigInteger rb = bigMultiplyPowerTen(raise);
                return new BigDecimal(rb, INFLATED, newScale, (precision > 0) ? precision + raise : 0);
            } else {

                int drop = checkScale((long) oldScale - newScale);
                if (drop < LONG_TEN_POWERS_TABLE.length) {
                    return divideAndRound(rs, LONG_TEN_POWERS_TABLE[
                    drop], newScale, roundingMode, newScale);
                } else {
                    return divideAndRound(this.inflated(), bigTenToThe(drop), newScale, roundingMode, newScale);
                }
            }
        } else {
            if (newScale > oldScale) {
                int raise = checkScale((long) newScale - oldScale);
                BigInteger rb = bigMultiplyPowerTen(this.intVal, raise);
                return new BigDecimal(rb, INFLATED, newScale, (precision > 0) ? precision + raise : 0);
            } else {

                int drop = checkScale((long) oldScale - newScale);
                if (drop < LONG_TEN_POWERS_TABLE.length)
                    return divideAndRound(this.intVal, LONG_TEN_POWERS_TABLE[
                    drop], newScale, roundingMode,
                    newScale);
                else
                    return divideAndRound(this.intVal, bigTenToThe(drop), newScale, roundingMode, newScale);
            }
        }
    }

    public BigDecimal setScale(int newScale) {
        return setScale(newScale, ROUND_UNNECESSARY);
    }

    public BigDecimal movePointLeft(int n) {
        if (n == 0 && scale >= 0) return this;

        int newScale = checkScale((long) scale + n);
        BigDecimal num = new BigDecimal(intVal, intCompact, newScale, 0);
        return num.scale < 0 ? num.setScale(0, ROUND_UNNECESSARY) : num;
    }

    public BigDecimal movePointRight(int n) {
        if (n == 0 && scale >= 0) return this;

        int newScale = checkScale((long) scale - n);
        BigDecimal num = new BigDecimal(intVal, intCompact, newScale, 0);
        return num.scale < 0 ? num.setScale(0, ROUND_UNNECESSARY) : num;
    }

    public BigDecimal scaleByPowerOfTen(int n) {
        return new BigDecimal(intVal, intCompact,
        checkScale((long) scale - n), precision);
    }

    public BigDecimal stripTrailingZeros() {
        return intCompact == 0 || (intVal != null && intVal.signum() == 0)
                ? BigDecimal.ZERO
                : stripZerosToMatchScale(intVal, intCompact, scale, Long.MIN_VALUE);
    }

    @Override
    public int compareTo(BigDecimal val) {

        if (scale == val.scale) {
            long xs = intCompact;
            long ys = val.intCompact;
            if (xs != INFLATED && ys != INFLATED)
                return xs != ys ? ((xs > ys) ? 1 : -1) : 0;
        }
        int xsign = this.signum();
        int ysign = val.signum();
        if (xsign != ysign)
            return (xsign > ysign) ? 1 : -1;
        if (xsign == 0)
            return 0;
        int cmp = compareMagnitude(val);
        return (xsign > 0) ? cmp : -cmp;
    }

    private int compareMagnitude(BigDecimal val) {
        // 快速路径：当 scale 相同且都是 compact 时直接比较 long
        if (scale == val.scale && intCompact != INFLATED && val.intCompact != INFLATED) {
            return Long.compare(Math.abs(intCompact), Math.abs(val.intCompact));
        }

        long ys = val.intCompact;
        long xs = this.intCompact;
        if (xs == 0)
            return (ys == 0) ? 0 : -1;
        if (ys == 0)
            return 1;

        long sdiff = (long) this.scale - val.scale;
        if (sdiff != 0) {

            long xae = (long) this.precision() - this.scale;
            long yae = (long) val.precision() - val.scale;
            if (xae < yae)
                return -1;
            if (xae > yae)
                return 1;
            if (sdiff < 0) {

                if (sdiff > Integer.MIN_VALUE &&
                        (xs == INFLATED ||
                                (xs = longMultiplyPowerTen(xs, (int) -sdiff)) == INFLATED) &&
                        ys == INFLATED) {
                    BigInteger rb = bigMultiplyPowerTen((int) -sdiff);
                    return rb.compareMagnitude(val.intVal);
                }
            } else {

                if (sdiff <= Integer.MAX_VALUE &&
                        (ys == INFLATED ||
                                (ys = longMultiplyPowerTen(ys, (int) sdiff)) == INFLATED) &&
                        xs == INFLATED) {
                    BigInteger rb = val.bigMultiplyPowerTen((int) sdiff);
                    return this.intVal.compareMagnitude(rb);
                }
            }
        }
        if (xs != INFLATED)
            return (ys != INFLATED) ? longCompareMagnitude(xs, ys) : -1;
        else if (ys != INFLATED)
            return 1;
        else
            return this.intVal.compareMagnitude(val.intVal);
    }

    @Override
    public boolean equals(Object x) {
        if (!(x instanceof BigDecimal xDec))
            return false;
        if (x == this)
            return true;
        if (scale != xDec.scale)
            return false;
        long s = this.intCompact;
        long xs = xDec.intCompact;
        if (s != INFLATED) {
            if (xs == INFLATED)
                xs = compactValFor(xDec.intVal);
            return xs == s;
        } else if (xs != INFLATED)
            return xs == compactValFor(this.intVal);

        return this.inflated().equals(xDec.inflated());
    }

    public BigDecimal min(BigDecimal val) {
        return (compareTo(val) <= 0 ? this : val);
    }

    public BigDecimal max(BigDecimal val) {
        return (compareTo(val) >= 0 ? this : val);
    }

    @Override
    public int hashCode() {
        if (intCompact != INFLATED) {
            long val2 = (intCompact < 0) ? -intCompact : intCompact;
            int temp = (int) (((int) (val2 >>> 32)) * 31 +
                            (val2 & LONG_MASK));
            return 31 * ((intCompact < 0) ? -temp : temp) + scale;
        } else
            return 31 * intVal.hashCode() + scale;
    }

    @Override
    public String toString() {
        String sc = stringCache;
        if (sc == null) {
            stringCache = sc = layoutChars(true);
        }
        return sc;
    }

    public String toEngineeringString() {
        return layoutChars(false);
    }

    public String toPlainString() {
        if (scale == 0) {
            if (intCompact != INFLATED) {
                return Long.toString(intCompact);
            } else {
                return intVal.toString();
            }
        }
        if (this.scale < 0) {
            if (signum() == 0) {
                return "0";
            }
            int trailingZeros = checkScaleNonZero((-(long) scale));
            String str = intCompact != INFLATED
                    ? Long.toString(intCompact)
                    : intVal.toString();
            int len = str.length() + trailingZeros;
            if (len < 0) {
                throw new OutOfMemoryError("too large to fit in a String");
            }
            StringBuilder buf = new StringBuilder(len);
            buf.append(str);
            buf.repeat('0', trailingZeros);
            return buf.toString();
        }
        String str;
        if (intCompact != INFLATED) {
            str = Long.toString(Math.abs(intCompact));
        } else {
            str = intVal.abs().toString();
        }
        return getValueString(signum(), str, scale);
    }

    private static String getValueString(int signum, String intString, int scale) {

        StringBuilder buf;
        int insertionPoint = intString.length() - scale;
        if (insertionPoint == 0) {
            return (signum < 0 ? "-0." : "0.") + intString;
        } else if (insertionPoint > 0) {
            buf = new StringBuilder(intString);
            buf.insert(insertionPoint, '.');
            if (signum < 0)
                buf.insert(0, '-');
        } else {
            int len = (signum < 0 ? 3 : 2) + scale;
            if (len < 0) {
                throw new OutOfMemoryError("too large to fit in a String");
            }
            buf = new StringBuilder(len);
            buf.append(signum < 0 ? "-0." : "0.");
            buf.repeat('0', -insertionPoint);
            buf.append(intString);
        }
        return buf.toString();
    }

    boolean isInteger() {
        if (scale <= 0 || signum() == 0)
            return true;

        int digitLen = precision != 0 ? precision
                : (intCompact != INFLATED ? precision() : (digitLengthLower(unscaledValue()) + 1));
        return digitLen > scale && stripZerosToMatchScale(intVal, intCompact, scale, 0L).scale == 0;
    }

    public BigInteger toBigInteger() {

        return this.setScale(0, ROUND_DOWN).inflated();
    }

    public BigInteger toBigIntegerExact() {

        return this.setScale(0, ROUND_UNNECESSARY).inflated();
    }

    @Override
    public long longValue() {
        if (intCompact != INFLATED && scale == 0) {
            return intCompact;
        } else {

            if (this.signum() == 0 || fractionOnly() ||
                    scale <= -64) {
                return 0;
            } else {
                return toBigInteger().longValue();
            }
        }
    }

    private boolean fractionOnly() {
        assert this.signum() != 0;
        return this.precision() <= this.scale;
    }

    public long longValueExact() {
        if (intCompact != INFLATED && scale == 0)
            return intCompact;

        if (this.signum() == 0)
            return 0;

        if (fractionOnly())
            throw new ArithmeticException("Rounding necessary");

        if (precision() - 19 > scale)
            throw new java.lang.ArithmeticException("Overflow");

        BigDecimal num = this.setScale(0, ROUND_UNNECESSARY);
        if (num.precision() >= 19)
            LongOverflow.check(num);
        return num.inflated().longValue();
    }

    private static class LongOverflow {

        private static final BigInteger LONGMIN = BigInteger.valueOf(Long.MIN_VALUE);

        private static final BigInteger LONGMAX = BigInteger.valueOf(Long.MAX_VALUE);

        public static void check(BigDecimal num) {
            BigInteger intVal = num.inflated();
            if (intVal.compareTo(LONGMIN) < 0 ||
                    intVal.compareTo(LONGMAX) > 0)
                throw new java.lang.ArithmeticException("Overflow");
        }
    }

    @Override
    public int intValue() {
        return (intCompact != INFLATED && scale == 0) ? (int) intCompact : (int) longValue();
    }

    public int intValueExact() {
        long num;
        num = this.longValueExact();
        if ((int) num != num)
            throw new java.lang.ArithmeticException("Overflow");
        return (int) num;
    }

    public short shortValueExact() {
        long num;
        num = this.longValueExact();
        if ((short) num != num)
            throw new java.lang.ArithmeticException("Overflow");
        return (short) num;
    }

    public byte byteValueExact() {
        long num;
        num = this.longValueExact();
        if ((byte) num != num)
            throw new java.lang.ArithmeticException("Overflow");
        return (byte) num;
    }

    @Override
    public float floatValue() {

        if (intCompact != INFLATED) {
            float v = intCompact;
            if (scale == 0) {
                return v;
            }

            if ((long) v == intCompact) {
                if (0 < scale && scale < FLOAT_10_POW.length) {
                    return v / FLOAT_10_POW[scale];
                }
                if (0 > scale && scale > -FLOAT_10_POW.length) {
                    return v * FLOAT_10_POW[-scale];
                }
            }
        }
        return fullFloatValue();
    }

    private float fullFloatValue() {
        if (intCompact == 0) {
            return 0.0f;
        }
        BigInteger w = unscaledValue().abs();
        long qb = w.bitLength() - (long) Math.ceil(scale * L);
        if (qb < Q_MIN_F - 2) {
            return signum() * 0.0f;
        }
        if (qb > Q_MAX_F + P_F + 1) {
            return signum() * Float.POSITIVE_INFINITY;
        }
        if (scale < 0) {
            return signum() * w.multiply(bigTenToThe(-scale)).floatValue();
        }
        if (scale == 0) {
            return signum() * w.floatValue();
        }
        int ql = (int) qb - (P_F + 3);
        BigInteger pow10 = bigTenToThe(scale);
        BigInteger m, n;
        if (ql <= 0) {
            m = w.shiftLeft(-ql);
            n = pow10;
        } else {
            m = w;
            n = pow10.shiftLeft(ql);
        }
        BigInteger[] qr = m.divideAndRemainder(n);
        int i = qr[0].intValue();
        int sb = qr[1].signum();
        int dq = (Integer.SIZE - (P_F + 2)) - Integer.numberOfLeadingZeros(i);
        int eq = (Q_MIN_F - 2) - ql;
        if (dq >= eq) {
            return signum() * Math.scalb((float) (i | sb), ql);
        }
        int mask = (1 << eq) - 1;
        int j = i >> eq | (Integer.signum(i & mask)) | sb;
        return signum() * Math.scalb((float) j, Q_MIN_F - 2);
    }

    @Override
    public double doubleValue() {

        if (intCompact != INFLATED) {
            double v = intCompact;
            if (scale == 0) {

                return v;
            }

            if ((long) v == intCompact) {

                if (0 < scale && scale < DOUBLE_10_POW.length) {
                    return v / DOUBLE_10_POW[scale];
                }
                if (0 > scale && scale > -DOUBLE_10_POW.length) {
                    return v * DOUBLE_10_POW[-scale];
                }
            }
        }
        return fullDoubleValue();
    }

    private double fullDoubleValue() {

        if (intCompact == 0) {
            return 0.0;
        }

        BigInteger w = unscaledValue().abs();
        long qb = w.bitLength() - (long) Math.ceil(scale * L);
        if (qb < Q_MIN_D - 2) {
            return signum() * 0.0;
        }
        if (qb > Q_MAX_D + P_D + 1) {

            return signum() * Double.POSITIVE_INFINITY;
        }

        if (scale < 0) {

            return signum() * w.multiply(bigTenToThe(-scale)).doubleValue();
        }
        if (scale == 0) {
            return signum() * w.doubleValue();
        }

        int ql = (int) qb - (P_D + 3);
        BigInteger pow10 = bigTenToThe(scale);
        BigInteger m, n;
        if (ql <= 0) {
            m = w.shiftLeft(-ql);
            n = pow10;
        } else {
            m = w;
            n = pow10.shiftLeft(ql);
        }

        BigInteger[] qr = m.divideAndRemainder(n);
        long i = qr[0].longValue();
        int sb = qr[1].signum();
        int dq = (Long.SIZE - (P_D + 2)) - Long.numberOfLeadingZeros(i);
        int eq = (Q_MIN_D - 2) - ql;
        if (dq >= eq) {
            return signum() * Math.scalb((double) (i | sb), ql);
        }

        long mask = (1L << eq) - 1;
        long j = i >> eq | Long.signum(i & mask) | sb;
        return signum() * Math.scalb((double) j, Q_MIN_D - 2);
    }

    private static final double[] DOUBLE_10_POW = {
            1.0e0, 1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5,
            1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10, 1.0e11,
            1.0e12, 1.0e13, 1.0e14, 1.0e15, 1.0e16, 1.0e17,
            1.0e18, 1.0e19, 1.0e20, 1.0e21, 1.0e22
    };

    private static final float[] FLOAT_10_POW = {
            1.0e0f, 1.0e1f, 1.0e2f, 1.0e3f, 1.0e4f, 1.0e5f,
            1.0e6f, 1.0e7f, 1.0e8f, 1.0e9f, 1.0e10f
    };

    public BigDecimal ulp() {
        return BigDecimal.valueOf(1, this.scale(), 1);
    }

    private String layoutChars(boolean sci) {
        long intCompact = this.intCompact;
        int scale = this.scale;
        if (scale == 0)
            return (intCompact != INFLATED) ? Long.toString(intCompact) : intVal.toString();
        if (scale == 2 &&
                intCompact >= 0 && intCompact < Integer.MAX_VALUE) {

            // MCRe NoiseFarlands: DecimalDigits replaced with simple String conversion
            String compactStr = String.valueOf(intCompact);
            int dotPos = compactStr.indexOf('.');
            if (dotPos < 0) dotPos = compactStr.length();
            int highIntSize = dotPos;
            byte[] buf = new byte[highIntSize + 3];
            byte[] tmpBytes = compactStr.substring(0, dotPos).getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(tmpBytes, 0, buf, 0, tmpBytes.length);
            buf[highIntSize] = '.';
            int lowInt = (int) Math.abs(intCompact) % 100;
            byte[] lowBytes = String.format("%02d", lowInt).getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(lowBytes, 0, buf, highIntSize + 1, 2);
            return new String(buf, StandardCharsets.ISO_8859_1);
        }

        char[] coeff;
        int offset;

        if (intCompact != INFLATED) {

            coeff = new char[19];
            // MCRe: DecimalDigits replaced with manual conversion
            long tmp = Math.abs(intCompact);
            offset = coeff.length;
            do {
                coeff[--offset] = (char) ('0' + (tmp % 10));
                tmp /= 10;
            } while (tmp > 0 && offset > 0);
        } else {
            offset = 0;
            coeff = intVal.abs().toString().toCharArray();
        }

        StringBuilder buf = new StringBuilder(32);
        ;
        if (signum() < 0)
            buf.append('-');
        int coeffLen = coeff.length - offset;
        long adjusted = -(long) scale + (coeffLen - 1);
        if ((scale >= 0) && (adjusted >= -6)) {
            int pad = scale - coeffLen;
            if (pad >= 0) {
                buf.append('0');
                buf.append('.');
                for (; pad > 0; pad--) {
                    buf.append('0');
                }
                buf.append(coeff, offset, coeffLen);
            } else {
                buf.append(coeff, offset, -pad);
                buf.append('.');
                buf.append(coeff, -pad + offset, scale);
            }
        } else {
            if (sci) {
                buf.append(coeff[offset]);
                if (coeffLen > 1) {
                    buf.append('.');
                    buf.append(coeff, offset + 1, coeffLen - 1);
                }
            } else {
                int sig = (int) (adjusted % 3);
                if (sig < 0)
                    sig += 3;
                adjusted -= sig;
                sig++;
                if (signum() == 0) {
                    switch (sig) {
                        case 1:
                            buf.append('0');
                            break;
                        case 2:
                            buf.append("0.00");
                            adjusted += 3;
                            break;
                        case 3:
                            buf.append("0.0");
                            adjusted += 3;
                            break;
                        default:
                            throw new AssertionError("Unexpected sig value " + sig);
                    }
                } else if (sig >= coeffLen) {
                    buf.append(coeff, offset, coeffLen);

                    for (int i = sig - coeffLen; i > 0; i--) {
                        buf.append('0');
                    }
                } else {
                    buf.append(coeff, offset, sig);
                    buf.append('.');
                    buf.append(coeff, offset + sig, coeffLen - sig);
                }
            }
            if (adjusted != 0) {
                buf.append('E');
                if (adjusted > 0)
                    buf.append('+');
                buf.append(adjusted);
            }
        }
        return buf.toString();
    }

    private static BigInteger bigTenToThe(int n) {
        if (n < 0)
            return BigInteger.ZERO;
        if (n < BIG_TEN_POWERS_TABLE.length)
            return BIG_TEN_POWERS_TABLE[n];
        // 🔧 MCRe 优化：接线缓存扩展（原来直接 TEN.pow(n) 每轮重算）
        return expandBigIntegerTenPowers(n);
    }

    private static BigInteger expandBigIntegerTenPowers(int n) {
        synchronized (BigDecimal.class) {
            BigInteger[] pows = BIG_TEN_POWERS_TABLE;
            int curLen = pows.length;

            if (curLen <= n) {
                int newLen = curLen << 1;
                while (newLen <= n) {
                    newLen <<= 1;
                }
                pows = Arrays.copyOf(pows, newLen);
                for (int i = curLen; i < newLen; i++) {
                    pows[i] = pows[i - 1].multiply(BigInteger.TEN);
                }

                BIG_TEN_POWERS_TABLE = pows;
            }
            return pows[n];
        }
    }

    private static final long[] LONG_TEN_POWERS_TABLE = {
            1,
            10,
            100,
            1000,
            10000,
            100000,
            1000000,
            10000000,
            100000000,
            1000000000,
            10000000000L,
            100000000000L,
            1000000000000L,
            10000000000000L,
            100000000000000L,
            1000000000000000L,
            10000000000000000L,
            100000000000000000L,
            1000000000000000000L
    };

    private static volatile BigInteger[] BIG_TEN_POWERS_TABLE = {
            BigInteger.ONE,
            BigInteger.valueOf(10),
            BigInteger.valueOf(100),
            BigInteger.valueOf(1000),
            BigInteger.valueOf(10000),
            BigInteger.valueOf(100000),
            BigInteger.valueOf(1000000),
            BigInteger.valueOf(10000000),
            BigInteger.valueOf(100000000),
            BigInteger.valueOf(1000000000),
            BigInteger.valueOf(10000000000L),
            BigInteger.valueOf(100000000000L),
            BigInteger.valueOf(1000000000000L),
            BigInteger.valueOf(10000000000000L),
            BigInteger.valueOf(100000000000000L),
            BigInteger.valueOf(1000000000000000L),
            BigInteger.valueOf(10000000000000000L),
            BigInteger.valueOf(100000000000000000L),
            BigInteger.valueOf(1000000000000000000L)
    };

    private static final int BIG_TEN_POWERS_TABLE_INITLEN = BIG_TEN_POWERS_TABLE.length;
    private static final int BIG_TEN_POWERS_TABLE_MAX = 16 * BIG_TEN_POWERS_TABLE_INITLEN;

    private static final long[] THRESHOLDS_TABLE = {
            Long.MAX_VALUE,
            Long.MAX_VALUE / 10L,
            Long.MAX_VALUE / 100L,
            Long.MAX_VALUE / 1000L,
            Long.MAX_VALUE / 10000L,
            Long.MAX_VALUE / 100000L,
            Long.MAX_VALUE / 1000000L,
            Long.MAX_VALUE / 10000000L,
            Long.MAX_VALUE / 100000000L,
            Long.MAX_VALUE / 1000000000L,
            Long.MAX_VALUE / 10000000000L,
            Long.MAX_VALUE / 100000000000L,
            Long.MAX_VALUE / 1000000000000L,
            Long.MAX_VALUE / 10000000000000L,
            Long.MAX_VALUE / 100000000000000L,
            Long.MAX_VALUE / 1000000000000000L,
            Long.MAX_VALUE / 10000000000000000L,
            Long.MAX_VALUE / 100000000000000000L,
            Long.MAX_VALUE / 1000000000000000000L
    };

    private static long longMultiplyPowerTen(long val, int n) {
        if (val == 0 || n <= 0)
            return val;
        // 快速路径：对于小值直接查表
        if (n < LONG_TEN_POWERS_TABLE.length) {
            long tenpower = LONG_TEN_POWERS_TABLE[n];
            if (val == 1)
                return tenpower;
            // 使用 Math.multiplyExact 检测溢出，比手动检查快
            try {
                return Math.multiplyExact(val, tenpower);
            } catch (ArithmeticException e) {
                return INFLATED;
            }
        }
        return INFLATED;
    }

    private BigInteger bigMultiplyPowerTen(int n) {
        if (n <= 0)
            return this.inflated();

        if (intCompact != INFLATED)
            return bigTenToThe(n).multiply(intCompact);
        else
            return intVal.multiply(bigTenToThe(n));
    }

    private BigInteger inflated() {
        if (intVal == null) {
            return BigInteger.valueOf(intCompact);
        }
        return intVal;
    }

    private static void matchScale(BigDecimal[] val) {
        if (val[0].scale < val[1].scale) {
            val[0] = val[0].setScale(val[1].scale, ROUND_UNNECESSARY);
        } else if (val[1].scale < val[0].scale) {
            val[1] = val[1].setScale(val[0].scale, ROUND_UNNECESSARY);
        }
    }

    private static class UnsafeHolder {
        // MCRe NoiseFarlands: replaced jdk.internal.misc.Unsafe with reflection
        private static final java.lang.reflect.Field intCompactField;
        private static final java.lang.reflect.Field intValField;
        private static final java.lang.reflect.Field scaleField;

        static {
            try {
                intCompactField = BigDecimal.class.getDeclaredField("intCompact");
                intValField = BigDecimal.class.getDeclaredField("intVal");
                scaleField = BigDecimal.class.getDeclaredField("scale");
                intCompactField.setAccessible(true);
                intValField.setAccessible(true);
                scaleField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static void setIntValAndScale(BigDecimal bd, BigInteger intVal, int scale) {
            try {
                intValField.set(bd, intVal);
                scaleField.setInt(bd, scale);
                intCompactField.setLong(bd, compactValFor(intVal));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static void setIntValVolatile(BigDecimal bd, BigInteger val) {
            try {
                intValField.set(bd, val);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();
        BigInteger serialIntVal = (BigInteger) fields.get("intVal", null);

        if (serialIntVal == null) {
            throw new StreamCorruptedException("Null or missing intVal in BigDecimal stream");
        }

        serialIntVal = toStrictBigInteger(serialIntVal);

        int serialScale = fields.get("scale", 0);

        UnsafeHolder.setIntValAndScale(this, serialIntVal, serialScale);
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialized BigDecimal objects need data");
    }

    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {

        if (this.intVal == null)
            UnsafeHolder.setIntValVolatile(this, BigInteger.valueOf(this.intCompact));

        s.defaultWriteObject();
    }

    static int longDigitLength(long x) {

        assert x != BigDecimal.INFLATED;
        if (x < 0)
            x = -x;
        if (x < 10)
            return 1;
        int r = ((64 - Long.numberOfLeadingZeros(x) + 1) * 1233) >>> 12;
        long[] tab = LONG_TEN_POWERS_TABLE;

        return (r >= tab.length || x < tab[r]) ? r : r + 1;
    }

    private static int bigDigitLength(BigInteger b) {

        if (b.signum == 0)
            return 1;
        int r = digitLengthLower(b);
        return b.compareMagnitude(bigTenToThe(r)) < 0 ? r : r + 1;
    }

    private static int digitLengthLower(BigInteger b) {
        return (int) (((b.abs().bitLength() + 1L) * 646456993L) >>> 31);
    }

    private int checkScale(long val) {
        int asInt = (int) val;
        if (asInt != val) {
            asInt = val > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            BigInteger b;
            if (intCompact != 0 &&
                    ((b = intVal) == null || b.signum() != 0))
                throw new ArithmeticException(asInt > 0 ? "Underflow" : "Overflow");
        }
        return asInt;
    }

    private static long compactValFor(BigInteger b) {
        int[] m = b.mag;
        int len = m.length;
        if (len == 0)
            return 0;
        int d = m[0];
        if (len > 2 || (len == 2 && d < 0))
            return INFLATED;

        long u = (len == 2) ? (((long) m[
                                        1] & LONG_MASK) + (((long) d) << 32)) : (((long) d) & LONG_MASK);
        return (b.signum < 0) ? -u : u;
    }

    private static int longCompareMagnitude(long x, long y) {
        if (x < 0)
            x = -x;
        if (y < 0)
            y = -y;
        return Long.compare(x, y);
    }

    private static int saturateLong(long s) {
        int i = (int) s;
        return (s == i) ? i : (s < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE);
    }

    private static void print(String name, BigDecimal bd) {
        System.err.format("%s:\tintCompact %d\tintVal %d\tscale %d\tprecision %d%n",
                name,
                bd.intCompact,
                bd.intVal,
                bd.scale,
                bd.precision);
    }

    private BigDecimal audit() {
        if (intCompact == INFLATED) {
            if (intVal == null) {
                print("audit", this);
                throw new AssertionError("null intVal");
            }

            if (precision > 0 && precision != bigDigitLength(intVal)) {
                print("audit", this);
                throw new AssertionError("precision mismatch");
            }
        } else {
            if (intVal != null) {
                long val = intVal.longValue();
                if (val != intCompact) {
                    print("audit", this);
                    throw new AssertionError("Inconsistent state, intCompact=" +
                            intCompact + "\t intVal=" + val);
                }
            }

            if (precision > 0 && precision != longDigitLength(intCompact)) {
                print("audit", this);
                throw new AssertionError("precision mismatch");
            }
        }
        return this;
    }

    private static int checkScaleNonZero(long val) {
        int asInt = (int) val;
        if (asInt != val) {
            throw new ArithmeticException(asInt > 0 ? "Underflow" : "Overflow");
        }
        return asInt;
    }

    private static int checkScale(long intCompact, long val) {
        int asInt = (int) val;
        if (asInt != val) {
            asInt = val > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            if (intCompact != 0)
                throw new ArithmeticException(asInt > 0 ? "Underflow" : "Overflow");
        }
        return asInt;
    }

    private static int checkScale(BigInteger intVal, long val) {
        int asInt = (int) val;
        if (asInt != val) {
            asInt = val > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            if (intVal.signum() != 0)
                throw new ArithmeticException(asInt > 0 ? "Underflow" : "Overflow");
        }
        return asInt;
    }

    private static BigDecimal doRound(BigDecimal val, MathContext mc) {
        int mcp = mc.precision;
        boolean wasDivided = false;
        if (mcp > 0) {
            BigInteger intVal = val.intVal;
            long compactVal = val.intCompact;
            int scale = val.scale;
            int prec = val.precision();
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    wasDivided = true;
                    compactVal = compactValFor(intVal);
                    if (compactVal != INFLATED) {
                        prec = longDigitLength(compactVal);
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[
                    drop], mc.roundingMode.oldMode);
                    wasDivided = true;
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                    intVal = null;
                }
            }
            return wasDivided ? new BigDecimal(intVal, compactVal, scale, prec) : val;
        }
        return val;
    }

    private static BigDecimal doRound(long compactVal, int scale, MathContext mc) {
        int mcp = mc.precision;
        if (mcp > 0 && mcp < 19) {
            int prec = longDigitLength(compactVal);
            int drop = prec - mcp;
            while (drop > 0) {
                scale = checkScaleNonZero((long) scale - drop);
                compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[
                drop], mc.roundingMode.oldMode);
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
            }
            return valueOf(compactVal, scale, prec);
        }
        return valueOf(compactVal, scale);
    }

    private static BigDecimal doRound(BigInteger intVal, int scale, MathContext mc) {
        int mcp = mc.precision;
        int prec = 0;
        if (mcp > 0) {
            long compactVal = compactValFor(intVal);
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(intVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    compactVal = compactValFor(intVal);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[
                    drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                return valueOf(compactVal, scale, prec);
            }
        }
        return new BigDecimal(intVal, INFLATED, scale, prec);
    }

    private static BigInteger divideAndRoundByTenPow(BigInteger intVal, int tenPow, int roundingMode) {
        if (tenPow < LONG_TEN_POWERS_TABLE.length)
            intVal = divideAndRound(intVal, LONG_TEN_POWERS_TABLE[tenPow], roundingMode);
        else
            intVal = divideAndRound(intVal, bigTenToThe(tenPow), roundingMode);
        return intVal;
    }

    private static BigDecimal divideAndRound(long ldividend, long ldivisor, int scale, int roundingMode,
            int preferredScale) {

        int qsign;
        long q = ldividend / ldivisor;
        if (roundingMode == ROUND_DOWN && scale == preferredScale)
            return valueOf(q, scale);
        long r = ldividend % ldivisor;
        qsign = ((ldividend < 0) == (ldivisor < 0)) ? 1 : -1;
        if (r != 0) {
            boolean increment = needIncrement(ldivisor, roundingMode, qsign, q, r);
            return valueOf((increment ? q + qsign : q), scale);
        } else {
            if (preferredScale != scale)
                return createAndStripZerosToMatchScale(q, scale, preferredScale);
            else
                return valueOf(q, scale);
        }
    }

    private static long divideAndRound(long ldividend, long ldivisor, int roundingMode) {
        int qsign;
        long q = ldividend / ldivisor;
        if (roundingMode == ROUND_DOWN)
            return q;
        long r = ldividend % ldivisor;
        qsign = ((ldividend < 0) == (ldivisor < 0)) ? 1 : -1;
        if (r != 0) {
            boolean increment = needIncrement(ldivisor, roundingMode, qsign, q, r);
            return increment ? q + qsign : q;
        } else {
            return q;
        }
    }

    private static boolean commonNeedIncrement(int roundingMode, int qsign,
            int cmpFracHalf, boolean oddQuot) {
        switch (roundingMode) {
            case ROUND_UNNECESSARY:
                throw new ArithmeticException("Rounding necessary");

            case ROUND_UP:
                return true;

            case ROUND_DOWN:
                return false;

            case ROUND_CEILING:
                return qsign > 0;

            case ROUND_FLOOR:
                return qsign < 0;

            default:
                assert roundingMode >= ROUND_HALF_UP &&
                                roundingMode <= ROUND_HALF_EVEN
                        : "Unexpected rounding mode" + RoundingMode.valueOf(roundingMode);

                if (cmpFracHalf < 0)
                    return false;
                else if (cmpFracHalf > 0)
                    return true;
                else {
                    assert cmpFracHalf == 0;

                    return switch (roundingMode) {
                        case ROUND_HALF_DOWN -> false;
                        case ROUND_HALF_UP -> true;
                        case ROUND_HALF_EVEN -> oddQuot;

                        default ->
                                throw new AssertionError("Unexpected rounding mode" + roundingMode);
                    };
                }
        }
    }

    private static boolean needIncrement(long ldivisor, int roundingMode,
            int qsign, long q, long r) {
        assert r != 0L;

        int cmpFracHalf;
        if (r <= HALF_LONG_MIN_VALUE || r > HALF_LONG_MAX_VALUE) {
            cmpFracHalf = 1;
        } else {
            cmpFracHalf = longCompareMagnitude(2 * r, ldivisor);
        }

        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, (q & 1L) != 0L);
    }

    private static BigInteger divideAndRound(BigInteger bdividend, long ldivisor, int roundingMode) {

        MutableBigInteger mdividend = new MutableBigInteger(bdividend.mag);

        MutableBigInteger mq = new MutableBigInteger();

        long r = mdividend.divide(ldivisor, mq);

        boolean isRemainderZero = (r == 0);

        int qsign = (ldivisor < 0) ? -bdividend.signum : bdividend.signum;
        if (!isRemainderZero) {
            if (needIncrement(ldivisor, roundingMode, qsign, mq, r)) {
                mq.add(MutableBigInteger.ONE);
            }
        }
        return mq.toBigInteger(qsign);
    }

    private static BigDecimal divideAndRound(BigInteger bdividend,
            long ldivisor, int scale, int roundingMode, int preferredScale) {

        MutableBigInteger mdividend = new MutableBigInteger(bdividend.mag);

        MutableBigInteger mq = new MutableBigInteger();

        long r = mdividend.divide(ldivisor, mq);

        boolean isRemainderZero = (r == 0);

        int qsign = (ldivisor < 0) ? -bdividend.signum : bdividend.signum;
        if (!isRemainderZero) {
            if (needIncrement(ldivisor, roundingMode, qsign, mq, r)) {
                mq.add(MutableBigInteger.ONE);
            }
            return mq.toBigDecimal(qsign, scale);
        } else {
            if (preferredScale != scale) {
                long compactVal = mq.toCompactValue(qsign);
                if (compactVal != INFLATED) {
                    return createAndStripZerosToMatchScale(compactVal, scale, preferredScale);
                }
                BigInteger intVal = mq.toBigInteger(qsign);
                return createAndStripZerosToMatchScale(intVal, scale, preferredScale);
            } else {
                return mq.toBigDecimal(qsign, scale);
            }
        }
    }

    private static boolean needIncrement(long ldivisor, int roundingMode,
            int qsign, MutableBigInteger mq, long r) {
        assert r != 0L;

        int cmpFracHalf;
        if (r <= HALF_LONG_MIN_VALUE || r > HALF_LONG_MAX_VALUE) {
            cmpFracHalf = 1;
        } else {
            cmpFracHalf = longCompareMagnitude(2 * r, ldivisor);
        }

        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, mq.isOdd());
    }

    private static BigInteger divideAndRound(BigInteger bdividend, BigInteger bdivisor, int roundingMode) {
        boolean isRemainderZero;
        int qsign;

        MutableBigInteger mdividend = new MutableBigInteger(bdividend.mag);
        MutableBigInteger mq = new MutableBigInteger();
        MutableBigInteger mdivisor = new MutableBigInteger(bdivisor.mag);
        MutableBigInteger mr = mdividend.divide(mdivisor, mq);
        isRemainderZero = mr.isZero();
        qsign = (bdividend.signum != bdivisor.signum) ? -1 : 1;
        if (!isRemainderZero) {
            if (needIncrement(mdivisor, roundingMode, qsign, mq, mr)) {
                mq.add(MutableBigInteger.ONE);
            }
        }
        return mq.toBigInteger(qsign);
    }

    private static BigDecimal divideAndRound(BigInteger bdividend, BigInteger bdivisor, int scale, int roundingMode,
            int preferredScale) {
        boolean isRemainderZero;
        int qsign;

        MutableBigInteger mdividend = new MutableBigInteger(bdividend.mag);
        MutableBigInteger mq = new MutableBigInteger();
        MutableBigInteger mdivisor = new MutableBigInteger(bdivisor.mag);
        MutableBigInteger mr = mdividend.divide(mdivisor, mq);
        isRemainderZero = mr.isZero();
        qsign = (bdividend.signum != bdivisor.signum) ? -1 : 1;
        if (!isRemainderZero) {
            if (needIncrement(mdivisor, roundingMode, qsign, mq, mr)) {
                mq.add(MutableBigInteger.ONE);
            }
            return mq.toBigDecimal(qsign, scale);
        } else {
            if (preferredScale != scale) {
                long compactVal = mq.toCompactValue(qsign);
                if (compactVal != INFLATED) {
                    return createAndStripZerosToMatchScale(compactVal, scale, preferredScale);
                }
                BigInteger intVal = mq.toBigInteger(qsign);
                return createAndStripZerosToMatchScale(intVal, scale, preferredScale);
            } else {
                return mq.toBigDecimal(qsign, scale);
            }
        }
    }

    private static boolean needIncrement(MutableBigInteger mdivisor, int roundingMode,
            int qsign, MutableBigInteger mq, MutableBigInteger mr) {
        assert !mr.isZero();
        int cmpFracHalf = mr.compareHalf(mdivisor);
        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, mq.isOdd());
    }

    private static final BigInteger[] FIVE_TO_2_TO = new BigInteger[16 + 1];

    static {
        BigInteger pow = FIVE_TO_2_TO[0] = BigInteger.valueOf(5L);
        for (int i = 1; i < FIVE_TO_2_TO.length; i++)
            FIVE_TO_2_TO[i] = pow = pow.multiply(pow);
    }

    private static BigInteger fiveToTwoToThe(int n) {
        int i = Math.min(n, FIVE_TO_2_TO.length - 1);
        BigInteger pow = FIVE_TO_2_TO[i];
        for (; i < n; i++)
            pow = pow.multiply(pow);

        return pow;
    }

    private static final double LOG_5_OF_2 = 0.43067655807339306;

    private static BigDecimal createAndStripZerosToMatchScale(BigInteger intVal, int scale, long preferredScale) {

        preferredScale = Math.clamp(preferredScale, Integer.MIN_VALUE - 1L, Integer.MAX_VALUE);
        int powsOf2 = intVal.getLowestSetBit();

        long remainingZeros = Math.min(scale - preferredScale, powsOf2);
        if (remainingZeros <= 0L)
            return valueOf(intVal, scale, 0);

        final int sign = intVal.signum;
        if (sign < 0)
            intVal = intVal.negate();

        intVal = intVal.shiftRight(powsOf2);

        long maxPowsOf5 = Math.round(intVal.bitLength() * LOG_5_OF_2);
        remainingZeros = Math.min(remainingZeros, maxPowsOf5);

        BigInteger[] qr;

        for (int i = 0; remainingZeros >= 1L << i; i++) {
            final int exp = 1 << i;
            qr = intVal.divideAndRemainder(fiveToTwoToThe(i));
            if (qr[1].signum != 0) {
                remainingZeros = exp - 1;
            } else {
                intVal = qr[0];
                scale = checkScale(intVal, (long) scale - exp);
                remainingZeros -= exp;
                powsOf2 -= exp;
            }
        }

        for (int i = BigInteger.bitLengthForLong(remainingZeros) - 1; i >= 0; i--) {
            final int exp = 1 << i;
            qr = intVal.divideAndRemainder(fiveToTwoToThe(i));
            if (qr[1].signum != 0) {
                remainingZeros = exp - 1;
            } else {
                intVal = qr[0];
                scale = checkScale(intVal, (long) scale - exp);
                remainingZeros -= exp;
                powsOf2 -= exp;

                if (remainingZeros < exp >> 1)
                    i = BigInteger.bitLengthForLong(remainingZeros);
            }
        }

        intVal = intVal.shiftLeft(powsOf2);
        return valueOf(sign >= 0 ? intVal : intVal.negate(), scale, 0);
    }

    private static BigDecimal createAndStripZerosToMatchScale(long compactVal, int scale, long preferredScale) {
        while (compactVal % 10L == 0L && scale > preferredScale) {
            compactVal /= 10L;
            scale = checkScale(compactVal, scale - 1L);
        }
        return valueOf(compactVal, scale);
    }

    private static BigDecimal stripZerosToMatchScale(BigInteger intVal, long intCompact, int scale, long preferredScale) {
        return intCompact != INFLATED
                ? createAndStripZerosToMatchScale(intCompact, scale, preferredScale)
                : createAndStripZerosToMatchScale(intVal == null ? INFLATED_BIGINT : intVal, scale, preferredScale);
    }

    private static long add(long xs, long ys) {
        long sum = xs + ys;

        if ((((sum ^ xs) & (sum ^ ys))) >= 0L) {
            return sum;
        }
        return INFLATED;
    }

    private static BigDecimal add(long xs, long ys, int scale) {
        long sum = add(xs, ys);
        if (sum != INFLATED)
            return BigDecimal.valueOf(sum, scale);
        return new BigDecimal(BigInteger.valueOf(xs).add(ys), scale);
    }

    private static BigDecimal add(final long xs, int scale1, final long ys, int scale2) {
        long sdiff = (long) scale1 - scale2;
        if (sdiff == 0) {
            return add(xs, ys, scale1);
        } else if (sdiff < 0) {
            int raise = checkScale(xs, -sdiff);
            long scaledX = longMultiplyPowerTen(xs, raise);
            if (scaledX != INFLATED) {
                return add(scaledX, ys, scale2);
            } else {
                BigInteger bigsum = bigMultiplyPowerTen(xs, raise).add(ys);
                return ((xs ^ ys) >= 0) ? new BigDecimal(bigsum, INFLATED, scale2, 0)
                        : valueOf(bigsum, scale2, 0);
            }
        } else {
            int raise = checkScale(ys, sdiff);
            long scaledY = longMultiplyPowerTen(ys, raise);
            if (scaledY != INFLATED) {
                return add(xs, scaledY, scale1);
            } else {
                BigInteger bigsum = bigMultiplyPowerTen(ys, raise).add(xs);
                return ((xs ^ ys) >= 0) ? new BigDecimal(bigsum, INFLATED, scale1, 0)
                        : valueOf(bigsum, scale1, 0);
            }
        }
    }

    private static BigDecimal add(final long xs, int scale1, BigInteger snd, int scale2) {
        int rscale = scale1;
        long sdiff = (long) rscale - scale2;
        boolean sameSigns = (Long.signum(xs) == snd.signum);
        BigInteger sum;
        if (sdiff < 0) {
            int raise = checkScale(xs, -sdiff);
            rscale = scale2;
            long scaledX = longMultiplyPowerTen(xs, raise);
            if (scaledX == INFLATED) {
                sum = snd.add(bigMultiplyPowerTen(xs, raise));
            } else {
                sum = snd.add(scaledX);
            }
        } else {
            int raise = checkScale(snd, sdiff);
            snd = bigMultiplyPowerTen(snd, raise);
            sum = snd.add(xs);
        }
        return (sameSigns) ? new BigDecimal(sum, INFLATED, rscale, 0) : valueOf(sum, rscale, 0);
    }

    private static BigDecimal add(BigInteger fst, int scale1, BigInteger snd, int scale2) {
        int rscale = scale1;
        long sdiff = (long) rscale - scale2;
        if (sdiff != 0) {
            if (sdiff < 0) {
                int raise = checkScale(fst, -sdiff);
                rscale = scale2;
                fst = bigMultiplyPowerTen(fst, raise);
            } else {
                int raise = checkScale(snd, sdiff);
                snd = bigMultiplyPowerTen(snd, raise);
            }
        }
        BigInteger sum = fst.add(snd);
        return (fst.signum == snd.signum) ? new BigDecimal(sum, INFLATED, rscale, 0) : valueOf(sum, rscale, 0);
    }

    private static BigInteger bigMultiplyPowerTen(long value, int n) {
        if (n <= 0)
            return BigInteger.valueOf(value);
        return bigTenToThe(n).multiply(value);
    }

    private static BigInteger bigMultiplyPowerTen(BigInteger value, int n) {
        if (n <= 0)
            return value;
        if (n < LONG_TEN_POWERS_TABLE.length) {
            return value.multiply(LONG_TEN_POWERS_TABLE[n]);
        }
        return value.multiply(bigTenToThe(n));
    }

    private static BigDecimal divideSmallFastPath(final long xs, int xscale,
            final long ys, int yscale,
            long preferredScale, MathContext mc) {
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        assert (xscale <= yscale) && (yscale < 18) && (mcp < 18);
        int xraise = yscale - xscale;
        long scaledX = (xraise == 0) ? xs : longMultiplyPowerTen(xs, xraise);
        BigDecimal quotient;

        int cmp = longCompareMagnitude(scaledX, ys);
        if (cmp > 0) {
            yscale -= 1;
            int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
            if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {

                int raise = checkScaleNonZero((long) mcp + yscale - xscale);
                long scaledXs;
                if ((scaledXs = longMultiplyPowerTen(xs, raise)) == INFLATED) {
                    quotient = null;
                    if ((mcp - 1) >= 0 && (mcp - 1) < LONG_TEN_POWERS_TABLE.length) {
                        quotient = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[
                        mcp - 1], scaledX, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                    if (quotient == null) {
                        BigInteger rb = bigMultiplyPowerTen(scaledX, mcp - 1);
                        quotient = divideAndRound(rb, ys,
                        scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                } else {
                    quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            } else {
                int newScale = checkScaleNonZero((long) xscale - mcp);

                if (newScale == yscale) {
                    quotient = divideAndRound(xs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                } else {
                    int raise = checkScaleNonZero((long) newScale - yscale);
                    long scaledYs;
                    if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                        BigInteger rb = bigMultiplyPowerTen(ys, raise);
                        quotient = divideAndRound(BigInteger.valueOf(xs),
                        rb, scl, roundingMode, checkScaleNonZero(preferredScale));
                    } else {
                        quotient = divideAndRound(xs, scaledYs, scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                }
            }
        } else {

            int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
            if (cmp == 0) {

                quotient = roundedTenPower(((scaledX < 0) == (ys < 0)) ? 1 : -1, mcp, scl, checkScaleNonZero(preferredScale));
            } else {

                long scaledXs;
                if ((scaledXs = longMultiplyPowerTen(scaledX, mcp)) == INFLATED) {
                    quotient = null;
                    if (mcp < LONG_TEN_POWERS_TABLE.length) {
                        quotient = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[
                        mcp], scaledX, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                    if (quotient == null) {
                        BigInteger rb = bigMultiplyPowerTen(scaledX, mcp);
                        quotient = divideAndRound(rb, ys,
                        scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                } else {
                    quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            }
        }

        return doRound(quotient, mc);
    }

    private static BigDecimal divide(final long xs, int xscale, final long ys, int yscale, long preferredScale, MathContext mc) {
        int mcp = mc.precision;
        if (xscale <= yscale && yscale < 18 && mcp < 18) {
            return divideSmallFastPath(xs, xscale, ys, yscale, preferredScale, mc);
        }
        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {
            yscale -= 1;
        }
        int roundingMode = mc.roundingMode.oldMode;

        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        BigDecimal quotient;
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            long scaledXs;
            if ((scaledXs = longMultiplyPowerTen(xs, raise)) == INFLATED) {
                BigInteger rb = bigMultiplyPowerTen(xs, raise);
                quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            } else {
                quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            }
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);

            if (newScale == yscale) {
                quotient = divideAndRound(xs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            } else {
                int raise = checkScaleNonZero((long) newScale - yscale);
                long scaledYs;
                if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                    BigInteger rb = bigMultiplyPowerTen(ys, raise);
                    quotient = divideAndRound(BigInteger.valueOf(xs),
                    rb, scl, roundingMode, checkScaleNonZero(preferredScale));
                } else {
                    quotient = divideAndRound(xs, scaledYs, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            }
        }

        return doRound(quotient, mc);
    }

    private static BigDecimal divide(BigInteger xs, int xscale, long ys, int yscale, long preferredScale, MathContext mc) {

        if ((-compareMagnitudeNormalized(ys, yscale, xs, xscale)) > 0) {
            yscale -= 1;
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        BigDecimal quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            BigInteger rb = bigMultiplyPowerTen(xs, raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);

            if (newScale == yscale) {
                quotient = divideAndRound(xs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            } else {
                int raise = checkScaleNonZero((long) newScale - yscale);
                long scaledYs;
                if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                    BigInteger rb = bigMultiplyPowerTen(ys, raise);
                    quotient = divideAndRound(xs, rb, scl, roundingMode, checkScaleNonZero(preferredScale));
                } else {
                    quotient = divideAndRound(xs, scaledYs, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            }
        }

        return doRound(quotient, mc);
    }

    private static BigDecimal divide(long xs, int xscale, BigInteger ys, int yscale, long preferredScale, MathContext mc) {

        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {
            yscale -= 1;
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        BigDecimal quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            BigInteger rb = bigMultiplyPowerTen(xs, raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            int raise = checkScaleNonZero((long) newScale - yscale);
            BigInteger rb = bigMultiplyPowerTen(ys, raise);
            quotient = divideAndRound(BigInteger.valueOf(xs), rb, scl, roundingMode, checkScaleNonZero(preferredScale));
        }

        return doRound(quotient, mc);
    }

    private static BigDecimal divide(BigInteger xs, int xscale, BigInteger ys, int yscale, long preferredScale, MathContext mc) {

        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {
            yscale -= 1;
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        BigDecimal quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            BigInteger rb = bigMultiplyPowerTen(xs, raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            int raise = checkScaleNonZero((long) newScale - yscale);
            BigInteger rb = bigMultiplyPowerTen(ys, raise);
            quotient = divideAndRound(xs, rb, scl, roundingMode, checkScaleNonZero(preferredScale));
        }

        return doRound(quotient, mc);
    }

    private static BigDecimal multiplyDivideAndRound(long dividend0, long dividend1, long divisor, int scale, int roundingMode,
            int preferredScale) {
        int qsign = Long.signum(dividend0) * Long.signum(dividend1) * Long.signum(divisor);
        dividend0 = Math.abs(dividend0);
        dividend1 = Math.abs(dividend1);
        divisor = Math.abs(divisor);

        long d0_hi = dividend0 >>> 32;
        long d0_lo = dividend0 & LONG_MASK;
        long d1_hi = dividend1 >>> 32;
        long d1_lo = dividend1 & LONG_MASK;
        long product = d0_lo * d1_lo;
        long d0 = product & LONG_MASK;
        long d1 = product >>> 32;
        product = d0_hi * d1_lo + d1;
        d1 = product & LONG_MASK;
        long d2 = product >>> 32;
        product = d0_lo * d1_hi + d1;
        d1 = product & LONG_MASK;
        d2 += product >>> 32;
        long d3 = d2 >>> 32;
        d2 &= LONG_MASK;
        product = d0_hi * d1_hi + d2;
        d2 = product & LONG_MASK;
        d3 = ((product >>> 32) + d3) & LONG_MASK;
        final long dividendHi = make64(d3, d2);
        final long dividendLo = make64(d1, d0);

        return divideAndRound128(dividendHi, dividendLo, divisor, qsign, scale, roundingMode, preferredScale);
    }

    private static final long DIV_NUM_BASE = (1L << 32);

    private static BigDecimal divideAndRound128(final long dividendHi, final long dividendLo, long divisor, int sign,
            int scale, int roundingMode, int preferredScale) {
        if (dividendHi >= divisor) {
            return null;
        }

        final int shift = Long.numberOfLeadingZeros(divisor);
        divisor <<= shift;

        final long v1 = divisor >>> 32;
        final long v0 = divisor & LONG_MASK;

        long tmp = dividendLo << shift;
        long u1 = tmp >>> 32;
        long u0 = tmp & LONG_MASK;

        tmp = (dividendHi << shift) | (dividendLo >>> 64 - shift);
        long u2 = tmp & LONG_MASK;
        long q1 = Long.divideUnsigned(tmp, v1);
        long r_tmp = Long.remainderUnsigned(tmp, v1);

        while (q1 >= DIV_NUM_BASE || unsignedLongCompare(q1 * v0, make64(r_tmp, u1))) {
            q1--;
            r_tmp += v1;
            if (r_tmp >= DIV_NUM_BASE)
                break;
        }

        tmp = mulsub(u2, u1, v1, v0, q1);
        u1 = tmp & LONG_MASK;
        long q0 = Long.divideUnsigned(tmp, v1);
        r_tmp = Long.remainderUnsigned(tmp, v1);

        while (q0 >= DIV_NUM_BASE || unsignedLongCompare(q0 * v0, make64(r_tmp, u0))) {
            q0--;
            r_tmp += v1;
            if (r_tmp >= DIV_NUM_BASE)
                break;
        }

        if ((int) q1 < 0) {

            MutableBigInteger mq = new MutableBigInteger(new int[]{(int) q1, (int) q0});
            if (roundingMode == ROUND_DOWN && scale == preferredScale) {
                return mq.toBigDecimal(sign, scale);
            }
            long r = mulsub(u1, u0, v1, v0, q0) >>> shift;
            if (r != 0) {
                if (needIncrement(divisor >>> shift, roundingMode, sign, mq, r)) {
                    mq.add(MutableBigInteger.ONE);
                }
                return mq.toBigDecimal(sign, scale);
            } else {
                if (preferredScale != scale) {
                    BigInteger intVal = mq.toBigInteger(sign);
                    return createAndStripZerosToMatchScale(intVal, scale, preferredScale);
                } else {
                    return mq.toBigDecimal(sign, scale);
                }
            }
        }

        long q = make64(q1, q0);
        q *= sign;

        if (roundingMode == ROUND_DOWN && scale == preferredScale)
            return valueOf(q, scale);

        long r = mulsub(u1, u0, v1, v0, q0) >>> shift;
        if (r != 0) {
            boolean increment = needIncrement(divisor >>> shift, roundingMode, sign, q, r);
            return valueOf((increment ? q + sign : q), scale);
        } else {
            if (preferredScale != scale) {
                return createAndStripZerosToMatchScale(q, scale, preferredScale);
            } else {
                return valueOf(q, scale);
            }
        }
    }

    private static BigDecimal roundedTenPower(int qsign, int raise, int scale, int preferredScale) {
        if (scale > preferredScale) {
            int diff = scale - preferredScale;
            if (diff < raise) {
                return scaledTenPow(raise - diff, qsign, preferredScale);
            } else {
                return valueOf(qsign, scale - raise);
            }
        } else {
            return scaledTenPow(raise, qsign, scale);
        }
    }

    static BigDecimal scaledTenPow(int n, int sign, int scale) {
        if (n < LONG_TEN_POWERS_TABLE.length)
            return valueOf(sign * LONG_TEN_POWERS_TABLE[n], scale);
        else {
            BigInteger unscaledVal = bigTenToThe(n);
            if (sign == -1) {
                unscaledVal = unscaledVal.negate();
            }
            return new BigDecimal(unscaledVal, INFLATED, scale, n + 1);
        }
    }

    private static long make64(long hi, long lo) {
        return hi << 32 | lo;
    }

    private static long mulsub(long u1, long u0, final long v1, final long v0, long q0) {
        long tmp = u0 - q0 * v0;
        return make64(u1 + (tmp >>> 32) - q0 * v1, tmp & LONG_MASK);
    }

    private static boolean unsignedLongCompare(long one, long two) {
        return (one + Long.MIN_VALUE) > (two + Long.MIN_VALUE);
    }

    private static boolean unsignedLongCompareEq(long one, long two) {
        return (one + Long.MIN_VALUE) >= (two + Long.MIN_VALUE);
    }

    private static int compareMagnitudeNormalized(long xs, int xscale, long ys, int yscale) {

        int sdiff = xscale - yscale;
        if (sdiff != 0) {
            if (sdiff < 0) {
                xs = longMultiplyPowerTen(xs, -sdiff);
            } else {
                ys = longMultiplyPowerTen(ys, sdiff);
            }
        }
        if (xs != INFLATED)
            return (ys != INFLATED) ? longCompareMagnitude(xs, ys) : -1;
        else
            return 1;
    }

    private static int compareMagnitudeNormalized(long xs, int xscale, BigInteger ys, int yscale) {

        if (xs == 0)
            return -1;
        int sdiff = xscale - yscale;
        if (sdiff < 0) {
            if (longMultiplyPowerTen(xs, -sdiff) == INFLATED) {
                return bigMultiplyPowerTen(xs, -sdiff).compareMagnitude(ys);
            }
        }
        return -1;
    }

    private static int compareMagnitudeNormalized(BigInteger xs, int xscale, BigInteger ys, int yscale) {
        int sdiff = xscale - yscale;
        if (sdiff < 0) {
            return bigMultiplyPowerTen(xs, -sdiff).compareMagnitude(ys);
        } else {
            return xs.compareMagnitude(bigMultiplyPowerTen(ys, sdiff));
        }
    }

    private static long multiply(long x, long y) {
        long product = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 == 0) || (y == 0) || (product / y == x)) {
            return product;
        }
        return INFLATED;
    }

    private static BigDecimal multiply(long x, long y, int scale) {
        long product = multiply(x, y);
        if (product != INFLATED) {
            return valueOf(product, scale);
        }
        return new BigDecimal(BigInteger.valueOf(x).multiply(y), INFLATED, scale, 0);
    }

    private static BigDecimal multiply(long x, BigInteger y, int scale) {
        if (x == 0) {
            return zeroValueOf(scale);
        }
        return new BigDecimal(y.multiply(x), INFLATED, scale, 0);
    }

    private static BigDecimal multiply(BigInteger x, BigInteger y, int scale) {
        return new BigDecimal(x.multiply(y), INFLATED, scale, 0);
    }

    private static BigDecimal multiplyAndRound(long x, long y, int scale, MathContext mc) {
        long product = multiply(x, y);
        if (product != INFLATED) {
            return doRound(product, scale, mc);
        }

        int rsign = 1;
        if (x < 0) {
            x = -x;
            rsign = -1;
        }
        if (y < 0) {
            y = -y;
            rsign *= -1;
        }

        long m0_hi = x >>> 32;
        long m0_lo = x & LONG_MASK;
        long m1_hi = y >>> 32;
        long m1_lo = y & LONG_MASK;
        product = m0_lo * m1_lo;
        long m0 = product & LONG_MASK;
        long m1 = product >>> 32;
        product = m0_hi * m1_lo + m1;
        m1 = product & LONG_MASK;
        long m2 = product >>> 32;
        product = m0_lo * m1_hi + m1;
        m1 = product & LONG_MASK;
        m2 += product >>> 32;
        long m3 = m2 >>> 32;
        m2 &= LONG_MASK;
        product = m0_hi * m1_hi + m2;
        m2 = product & LONG_MASK;
        m3 = ((product >>> 32) + m3) & LONG_MASK;
        final long mHi = make64(m3, m2);
        final long mLo = make64(m1, m0);
        BigDecimal res = doRound128(mHi, mLo, rsign, scale, mc);
        if (res != null) {
            return res;
        }
        res = new BigDecimal(BigInteger.valueOf(x).multiply(y * rsign), INFLATED, scale, 0);
        return doRound(res, mc);
    }

    private static BigDecimal multiplyAndRound(long x, BigInteger y, int scale, MathContext mc) {
        if (x == 0) {
            return zeroValueOf(scale);
        }
        return doRound(y.multiply(x), scale, mc);
    }

    private static BigDecimal multiplyAndRound(BigInteger x, BigInteger y, int scale, MathContext mc) {
        return doRound(x.multiply(y), scale, mc);
    }

    private static BigDecimal doRound128(long hi, long lo, int sign, int scale, MathContext mc) {
        int mcp = mc.precision;
        int drop;
        BigDecimal res = null;
        if (((drop = precision(hi, lo) - mcp) > 0) && (drop < LONG_TEN_POWERS_TABLE.length)) {
            scale = checkScaleNonZero((long) scale - drop);
            res = divideAndRound128(hi, lo, LONG_TEN_POWERS_TABLE[
            drop], sign, scale, mc.roundingMode.oldMode, scale);
        }
        if (res != null) {
            return doRound(res, mc);
        }
        return null;
    }

    private static final long[][] LONGLONG_TEN_POWERS_TABLE = {
            {0L, 0x8AC7230489E80000L},
            {0x5L, 0x6bc75e2d63100000L},
            {0x36L, 0x35c9adc5dea00000L},
            {0x21eL, 0x19e0c9bab2400000L},
            {0x152dL, 0x02c7e14af6800000L},
            {0xd3c2L, 0x1bcecceda1000000L},
            {0x84595L, 0x161401484a000000L},
            {0x52b7d2L, 0xdcc80cd2e4000000L},
            {0x33b2e3cL, 0x9fd0803ce8000000L},
            {0x204fce5eL, 0x3e25026110000000L},
            {0x1431e0faeL, 0x6d7217caa0000000L},
            {0xc9f2c9cd0L, 0x4674edea40000000L},
            {0x7e37be2022L, 0xc0914b2680000000L},
            {0x4ee2d6d415bL, 0x85acef8100000000L},
            {0x314dc6448d93L, 0x38c15b0a00000000L},
            {0x1ed09bead87c0L, 0x378d8e6400000000L},
            {0x13426172c74d82L, 0x2b878fe800000000L},
            {0xc097ce7bc90715L, 0xb34b9f1000000000L},
            {0x785ee10d5da46d9L, 0x00f436a000000000L},
            {0x4b3b4ca85a86c47aL, 0x098a224000000000L},
    };

    private static int precision(long hi, long lo) {
        if (hi == 0) {
            if (lo >= 0) {
                return longDigitLength(lo);
            }
            return (unsignedLongCompareEq(lo, LONGLONG_TEN_POWERS_TABLE[0][1])) ? 20 : 19;
        }
        int r = ((128 - Long.numberOfLeadingZeros(hi) + 1) * 1233) >>> 12;
        int idx = r - 19;
        return (idx >= LONGLONG_TEN_POWERS_TABLE.length || longLongCompareMagnitude(hi, lo,
                        LONGLONG_TEN_POWERS_TABLE[idx][0], LONGLONG_TEN_POWERS_TABLE[idx][
                        1])) ? r : r + 1;
    }

    private static boolean longLongCompareMagnitude(long hi0, long lo0, long hi1, long lo1) {
        if (hi0 != hi1) {
            return hi0 < hi1;
        }
        return (lo0 + Long.MIN_VALUE) < (lo1 + Long.MIN_VALUE);
    }

    private static BigDecimal divide(long dividend, int dividendScale, long divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend, (long) scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            if (raise < LONG_TEN_POWERS_TABLE.length) {
                long xs = dividend;
                if ((xs = longMultiplyPowerTen(xs, raise)) != INFLATED) {
                    return divideAndRound(xs, divisor, scale, roundingMode, scale);
                }
                BigDecimal q = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[
                raise], dividend, divisor, scale, roundingMode, scale);
                if (q != null) {
                    return q;
                }
            }
            BigInteger scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor, (long) dividendScale - scale);
            int raise = newScale - divisorScale;
            if (raise < LONG_TEN_POWERS_TABLE.length) {
                long ys = divisor;
                if ((ys = longMultiplyPowerTen(ys, raise)) != INFLATED) {
                    return divideAndRound(dividend, ys, scale, roundingMode, scale);
                }
            }
            BigInteger scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(BigInteger.valueOf(dividend), scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static BigDecimal divide(BigInteger dividend, int dividendScale, long divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend, (long) scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            BigInteger scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor, (long) dividendScale - scale);
            int raise = newScale - divisorScale;
            if (raise < LONG_TEN_POWERS_TABLE.length) {
                long ys = divisor;
                if ((ys = longMultiplyPowerTen(ys, raise)) != INFLATED) {
                    return divideAndRound(dividend, ys, scale, roundingMode, scale);
                }
            }
            BigInteger scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(dividend, scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static BigDecimal divide(long dividend, int dividendScale, BigInteger divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend, (long) scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            BigInteger scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor, (long) dividendScale - scale);
            int raise = newScale - divisorScale;
            BigInteger scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(BigInteger.valueOf(dividend), scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static BigDecimal divide(BigInteger dividend, int dividendScale, BigInteger divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend, (long) scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            BigInteger scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor, (long) dividendScale - scale);
            int raise = newScale - divisorScale;
            BigInteger scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(dividend, scaledDivisor, scale, roundingMode, scale);
        }
    }

    // 用线程局部缓存重用对象
    private static final ThreadLocal<
            MutableBigInteger> MUTABLE_CACHE = ThreadLocal.withInitial(MutableBigInteger::new);

}
