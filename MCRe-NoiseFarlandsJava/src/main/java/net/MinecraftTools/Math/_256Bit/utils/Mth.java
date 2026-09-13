package net.MinecraftTools.Math._256Bit.utils;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.util.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.MinecraftTools.Math._256Bit.DynamicNumber;
import net.MinecraftTools.Math._256Bit.Float256;
import net.MinecraftTools.Math._256Bit.Int256;
import net.MinecraftTools.Math._256Bit.UFloat256;
import net.MinecraftTools.Math._256Bit.UInt256;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Mth {
    private static final long UUID_VERSION = 61440L;
    private static final long UUID_VERSION_TYPE_4 = 16384L;
    private static final long UUID_VARIANT = -4611686018427387904L;
    private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
    public static final float PI = (float) Math.PI;
    public static final float HALF_PI = (float) (Math.PI / 2);
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    public static final float RAD_TO_DEG = 180.0F / (float) Math.PI;
    public static final float EPSILON = 1.0E-5F;
    public static final float SQRT_OF_TWO = sqrt(2.0F);
    public static final Vector3fc Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Vector3fc X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
    public static final Vector3fc Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final int SIN_QUANTIZATION = 65536;
    private static final int SIN_MASK = 65535;
    private static final int COS_OFFSET = 16384;
    private static final double SIN_SCALE = 10430.378350470453;
    private static final float[] SIN = Util.make(new float[65536], sin -> {
        for (int i = 0; i < sin.length; i++) {
            sin[i] = (float) Math.sin(i / 10430.378350470453);
        }
    });
    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
            0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
    };
    private static final double ONE_SIXTH = 0.16666666666666666;
    private static final int FRAC_EXP = 8;
    private static final int LUT_SIZE = 257;
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static float sin(final double i) {
        return SIN[(int) ((long) (i * 10430.378350470453) & 65535L)];
    }

    public static float cos(final double i) {
        return SIN[(int) ((long) (i * 10430.378350470453 + 16384.0) & 65535L)];
    }

    public static float sqrt(final float x) {
        return (float) Math.sqrt(x);
    }

    public static int floor(final float v) {
        return (int) Math.floor(v);
    }

    public static int floor(final double v) {
        return (int) Math.floor(v);
    }

    public static long lfloor(final double v) {
        return (long) Math.floor(v);
    }

    public static float abs(final float v) {
        return Math.abs(v);
    }

    public static int abs(final int v) {
        return Math.abs(v);
    }

    public static int ceil(final float v) {
        return (int) Math.ceil(v);
    }

    public static int ceil(final double v) {
        return (int) Math.ceil(v);
    }

    public static long ceilLong(final double v) {
        return (long) Math.ceil(v);
    }

    public static int clamp(final int value, final int min, final int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static long clamp(final long value, final long min, final long max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(final float value, final float min, final float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clamp(final double value, final double min, final double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clampedLerp(final double factor, final double min, final double max) {
        if (factor < 0.0) {
            return min;
        } else {
            return factor > 1.0 ? max : lerp(factor, min, max);
        }
    }

    public static float clampedLerp(final float factor, final float min, final float max) {
        if (factor < 0.0F) {
            return min;
        } else {
            return factor > 1.0F ? max : lerp(factor, min, max);
        }
    }

    public static int absMax(final int a, final int b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    public static float absMax(final float a, final float b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    public static double absMax(final double a, final double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    public static int chessboardDistance(final int x0, final int z0, final int x1, final int z1) {
        return absMax(x1 - x0, z1 - z0);
    }

    public static int floorDiv(final int a, final int b) {
        return Math.floorDiv(a, b);
    }

    public static int nextInt(final RandomSource random, final int minInclusive, final int maxInclusive) {
        return minInclusive >= maxInclusive ? minInclusive : random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    public static float nextFloat(final RandomSource random, final float min, final float max) {
        return min >= max ? min : random.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(final RandomSource random, final double min, final double max) {
        return min >= max ? min : random.nextDouble() * (max - min) + min;
    }

    public static boolean equal(final float a, final float b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static boolean equal(final double a, final double b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static int positiveModulo(final int input, final int mod) {
        return Math.floorMod(input, mod);
    }

    public static float positiveModulo(final float input, final float mod) {
        return (input % mod + mod) % mod;
    }

    public static double positiveModulo(final double input, final double mod) {
        return (input % mod + mod) % mod;
    }

    public static boolean isMultipleOf(final int dividend, final int divisor) {
        return dividend % divisor == 0;
    }

    public static byte packDegrees(final float angle) {
        return (byte) floor(angle * 256.0F / 360.0F);
    }

    public static float unpackDegrees(final byte rot) {
        return rot * 360 / 256.0F;
    }

    public static int wrapDegrees(final int angle) {
        int normalizedAngle = angle % 360;
        if (normalizedAngle >= 180) {
            normalizedAngle -= 360;
        }

        if (normalizedAngle < -180) {
            normalizedAngle += 360;
        }

        return normalizedAngle;
    }

    public static float wrapDegrees(final long angle) {
        float normalizedAngle = (float) (angle % 360L);
        if (normalizedAngle >= 180.0F) {
            normalizedAngle -= 360.0F;
        }

        if (normalizedAngle < -180.0F) {
            normalizedAngle += 360.0F;
        }

        return normalizedAngle;
    }

    public static float wrapDegrees(final float angle) {
        float normalizedAngle = angle % 360.0F;
        if (normalizedAngle >= 180.0F) {
            normalizedAngle -= 360.0F;
        }

        if (normalizedAngle < -180.0F) {
            normalizedAngle += 360.0F;
        }

        return normalizedAngle;
    }

    public static double wrapDegrees(final double angle) {
        double normalizedAngle = angle % 360.0;
        if (normalizedAngle >= 180.0) {
            normalizedAngle -= 360.0;
        }

        if (normalizedAngle < -180.0) {
            normalizedAngle += 360.0;
        }

        return normalizedAngle;
    }

    public static float wrapDegrees90(final float angle) {
        float normalizedAngle = angle % 90.0F;
        if (normalizedAngle >= 45.0F) {
            normalizedAngle -= 90.0F;
        }

        if (normalizedAngle < -45.0F) {
            normalizedAngle += 90.0F;
        }

        return normalizedAngle;
    }

    public static float degreesDifference(final float fromAngle, final float toAngle) {
        return wrapDegrees(toAngle - fromAngle);
    }

    public static float degreesDifferenceAbs(final float angleA, final float angleB) {
        return abs(degreesDifference(angleA, angleB));
    }

    public static float rotateIfNecessary(final float baseAngle, final float targetAngle, final float maxAngleDiff) {
        float deltaAngle = degreesDifference(baseAngle, targetAngle);
        float deltaAngleClamped = clamp(deltaAngle, -maxAngleDiff, maxAngleDiff);
        return targetAngle - deltaAngleClamped;
    }

    public static float approach(final float current, final float target, float increment) {
        increment = abs(increment);
        return current < target ? clamp(current + increment, current, target) : clamp(current - increment, target, current);
    }

    public static float approachDegrees(final float current, final float target, final float increment) {
        float difference = degreesDifference(current, target);
        return approach(current, current + difference, increment);
    }

    public static int getInt(final String input, final int def) {
        return NumberUtils.toInt(input, def);
    }

    public static int smallestEncompassingPowerOfTwo(final int input) {
        int result = input - 1;
        result |= result >> 1;
        result |= result >> 2;
        result |= result >> 4;
        result |= result >> 8;
        result |= result >> 16;
        return result + 1;
    }

    public static int smallestSquareSide(final int itemCount) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be greater than or equal to zero");
        } else {
            return ceil(Math.sqrt(itemCount));
        }
    }

    public static boolean isPowerOfTwo(final int input) {
        return input != 0 && (input & input - 1) == 0;
    }

    public static boolean isPowerOfTwo(final long input) {
        return input != 0L && (input & input - 1L) == 0L;
    }

    public static int ceillog2(int input) {
        input = isPowerOfTwo(input) ? input : smallestEncompassingPowerOfTwo(input);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) (input * 125613361L >> 27) & 31];
    }

    public static int log2(final int input) {
        return ceillog2(input) - (isPowerOfTwo(input) ? 0 : 1);
    }

    public static float frac(final float num) {
        return num - floor(num);
    }

    public static double frac(final double num) {
        return num - lfloor(num);
    }

    @Deprecated
    public static long getSeed(final Vec3i vec) {
        return getSeed(vec.getX(), vec.getY(), vec.getZ());
    }

    @Deprecated
    public static long getSeed(final int x, final int y, final int z) {
        long seed = x * 3129871 ^ z * 116129781L ^ y;
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }

    public static UUID createInsecureUUID(final RandomSource random) {
        long most = random.nextLong() & -61441L | 16384L;
        long least = random.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
        return new UUID(most, least);
    }

    public static double inverseLerp(final double value, final double min, final double max) {
        return (value - min) / (max - min);
    }

    public static float inverseLerp(final float value, final float min, final float max) {
        return (value - min) / (max - min);
    }

    public static boolean rayIntersectsAABB(final Vec3 rayStart, final Vec3 rayDir, final AABB aabb) {
        double centerX = (aabb.minX + aabb.maxX) * 0.5;
        double boxExtentX = (aabb.maxX - aabb.minX) * 0.5;
        double diffX = rayStart.x - centerX;
        if (Math.abs(diffX) > boxExtentX && diffX * rayDir.x >= 0.0) {
            return false;
        }

        double centerY = (aabb.minY + aabb.maxY) * 0.5;
        double boxExtentY = (aabb.maxY - aabb.minY) * 0.5;
        double diffY = rayStart.y - centerY;
        if (Math.abs(diffY) > boxExtentY && diffY * rayDir.y >= 0.0) {
            return false;
        }

        double centerZ = (aabb.minZ + aabb.maxZ) * 0.5;
        double boxExtentZ = (aabb.maxZ - aabb.minZ) * 0.5;
        double diffZ = rayStart.z - centerZ;
        if (Math.abs(diffZ) > boxExtentZ && diffZ * rayDir.z >= 0.0) {
            return false;
        }

        double andrewWooDiffX = Math.abs(rayDir.x);
        double andrewWooDiffY = Math.abs(rayDir.y);
        double andrewWooDiffZ = Math.abs(rayDir.z);
        double f = rayDir.y * diffZ - rayDir.z * diffY;
        if (Math.abs(f) > boxExtentY * andrewWooDiffZ + boxExtentZ * andrewWooDiffY) {
            return false;
        }

        f = rayDir.z * diffX - rayDir.x * diffZ;
        if (Math.abs(f) > boxExtentX * andrewWooDiffZ + boxExtentZ * andrewWooDiffX) {
            return false;
        }

        f = rayDir.x * diffY - rayDir.y * diffX;
        return Math.abs(f) < boxExtentX * andrewWooDiffY + boxExtentY * andrewWooDiffX;
    }

    public static double atan2(double y, double x) {
        double d2 = x * x + y * y;
        if (Double.isNaN(d2)) {
            return Double.NaN;
        }

        boolean negY = y < 0.0;
        if (negY) {
            y = -y;
        }

        boolean negX = x < 0.0;
        if (negX) {
            x = -x;
        }

        boolean steep = y > x;
        if (steep) {
            double t = x;
            x = y;
            y = t;
        }

        double rinv = fastInvSqrt(d2);
        x *= rinv;
        y *= rinv;
        double yp = FRAC_BIAS + y;
        int index = (int) Double.doubleToRawLongBits(yp);
        double phi = ASIN_TAB[index];
        double cPhi = COS_TAB[index];
        double sPhi = yp - FRAC_BIAS;
        double sd = y * cPhi - x * sPhi;
        double d = (6.0 + sd * sd) * sd * 0.16666666666666666;
        double theta = phi + d;
        if (steep) {
            theta = (Math.PI / 2) - theta;
        }

        if (negX) {
            theta = Math.PI - theta;
        }

        if (negY) {
            theta = -theta;
        }

        return theta;
    }

    public static float invSqrt(final float x) {
        return org.joml.Math.invsqrt(x);
    }

    public static double invSqrt(final double x) {
        return org.joml.Math.invsqrt(x);
    }

    @Deprecated
    public static double fastInvSqrt(double x) {
        double xhalf = 0.5 * x;
        long i = Double.doubleToRawLongBits(x);
        i = 6910469410427058090L - (i >> 1);
        x = Double.longBitsToDouble(i);
        return x * (1.5 - xhalf * x * x);
    }

    public static float fastInvCubeRoot(final float x) {
        int i = Float.floatToIntBits(x);
        i = 1419967116 - i / 3;
        float y = Float.intBitsToFloat(i);
        y = 0.6666667F * y + 1.0F / (3.0F * y * y * x);
        return 0.6666667F * y + 1.0F / (3.0F * y * y * x);
    }

    public static int hsvToRgb(final float hue, final float saturation, final float value) {
        return hsvToArgb(hue, saturation, value, 0);
    }

    public static int hsvToArgb(final float hue, final float saturation, final float value, final int alpha) {
        int h = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - h;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - f * saturation);
        float t = value * (1.0F - (1.0F - f) * saturation);
        float red;
        float green;
        float blue;
        switch (h) {
            case 0:
                red = value;
                green = t;
                blue = p;
                break;
            case 1:
                red = q;
                green = value;
                blue = p;
                break;
            case 2:
                red = p;
                green = value;
                blue = t;
                break;
            case 3:
                red = p;
                green = q;
                blue = value;
                break;
            case 4:
                red = t;
                green = p;
                blue = value;
                break;
            case 5:
                red = value;
                green = p;
                blue = q;
                break;
            default:
                throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }

        return ARGB.color(alpha, clamp((int) (red * 255.0F), 0, 255), clamp((int) (green * 255.0F), 0, 255), clamp((int) (blue * 255.0F), 0, 255));
    }

    public static int murmurHash3Mixer(int hash) {
        hash ^= hash >>> 16;
        hash *= -2048144789;
        hash ^= hash >>> 13;
        hash *= -1028477387;
        return hash ^ hash >>> 16;
    }

    public static int binarySearch(int from, final int to, final IntPredicate condition) {
        int len = to - from;

        while (len > 0) {
            int half = len / 2;
            int middle = from + half;
            if (condition.test(middle)) {
                len = half;
            } else {
                from = middle + 1;
                len -= half + 1;
            }
        }

        return from;
    }

    public static int lerpInt(final float alpha1, final int p0, final int p1) {
        return p0 + floor(alpha1 * (p1 - p0));
    }

    public static int lerpDiscrete(final float alpha1, final int p0, final int p1) {
        int delta = p1 - p0;
        return p0 + floor(alpha1 * (delta - 1)) + (alpha1 > 0.0F ? 1 : 0);
    }

    public static float lerp(final float alpha1, final float p0, final float p1) {
        return p0 + alpha1 * (p1 - p0);
    }

    public static Vec3 lerp(final double alpha, final Vec3 p1, final Vec3 p2) {
        return new Vec3(lerp(alpha, p1.x, p2.x), lerp(alpha, p1.y, p2.y), lerp(alpha, p1.z, p2.z));
    }

    public static double lerp(final double alpha1, final double p0, final double p1) {
        return p0 + alpha1 * (p1 - p0);
    }

    public static double lerp2(final double alpha1, final double alpha2, final double x00, final double x10, final double x01, final double x11) {
        return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
    }

    public static double lerp3(
            final double alpha1,
            final double alpha2,
            final double alpha3,
            final double x000,
            final double x100,
            final double x010,
            final double x110,
            final double x001,
            final double x101,
            final double x011,
            final double x111) {
        return lerp(alpha3, lerp2(alpha1, alpha2, x000, x100, x010, x110), lerp2(alpha1, alpha2, x001, x101, x011, x111));
    }

    public static float catmullrom(final float alpha, final float p0, final float p1, final float p2, final float p3) {
        return 0.5F
                * (2.0F * p1
                        + (p2 - p0) * alpha
                        + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * alpha * alpha
                        + (3.0F * p1 - p0 - 3.0F * p2 + p3) * alpha * alpha * alpha);
    }

    public static double smoothstep(final double x) {
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    public static double smoothstepDerivative(final double x) {
        return 30.0 * x * x * (x - 1.0) * (x - 1.0);
    }

    public static int sign(final double number) {
        if (number == 0.0) {
            return 0;
        } else {
            return number > 0.0 ? 1 : -1;
        }
    }

    public static float rotLerp(final float a, final float from, final float to) {
        return from + a * wrapDegrees(to - from);
    }

    public static double rotLerp(final double a, final double from, final double to) {
        return from + a * wrapDegrees(to - from);
    }

    public static float rotLerpRad(final float a, final float from, final float to) {
        float diff = to - from;

        while (diff < (float) -Math.PI) {
            diff += (float) (Math.PI * 2);
        }

        while (diff >= (float) Math.PI) {
            diff -= (float) (Math.PI * 2);
        }

        return from + a * diff;
    }

    public static float triangleWave(final float index, final float period) {
        return (Math.abs(index % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    public static float square(final float x) {
        return x * x;
    }

    public static float cube(final float x) {
        return x * x * x;
    }

    public static double square(final double x) {
        return x * x;
    }

    public static int square(final int x) {
        return x * x;
    }

    public static long square(final long x) {
        return x * x;
    }

    public static double clampedMap(final double value, final double fromMin, final double fromMax, final double toMin, final double toMax) {
        return clampedLerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static float clampedMap(final float value, final float fromMin, final float fromMax, final float toMin, final float toMax) {
        return clampedLerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static double map(final double value, final double fromMin, final double fromMax, final double toMin, final double toMax) {
        return lerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static float map(final float value, final float fromMin, final float fromMax, final float toMin, final float toMax) {
        return lerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static double wobble(final double coord) {
        return coord + (2.0 * RandomSource.createThreadLocalInstance(floor(coord * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
    }

    public static int roundToward(final int input, final int multiple) {
        return positiveCeilDiv(input, multiple) * multiple;
    }

    public static long roundToward(final long input, final long multiple) {
        return positiveCeilDiv(input, multiple) * multiple;
    }

    public static int positiveCeilDiv(final int input, final int divisor) {
        return -Math.floorDiv(-input, divisor);
    }

    public static long positiveCeilDiv(final long input, final long divisor) {
        return -Math.floorDiv(-input, divisor);
    }

    public static int randomBetweenInclusive(final RandomSource random, final int min, final int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }

    public static float randomBetween(final RandomSource random, final float min, final float maxExclusive) {
        return random.nextFloat() * (maxExclusive - min) + min;
    }

    public static float normal(final RandomSource random, final float mean, final float deviation) {
        return mean + (float) random.nextGaussian() * deviation;
    }

    public static double lengthSquared(final double x, final double y) {
        return x * x + y * y;
    }

    public static double length(final double x, final double y) {
        return Math.sqrt(lengthSquared(x, y));
    }

    public static float length(final float x, final float y) {
        return (float) Math.sqrt(lengthSquared(x, y));
    }

    public static double lengthSquared(final double x, final double y, final double z) {
        return x * x + y * y + z * z;
    }

    public static double length(final double x, final double y, final double z) {
        return Math.sqrt(lengthSquared(x, y, z));
    }

    public static float lengthSquared(final float x, final float y, final float z) {
        return x * x + y * y + z * z;
    }

    public static int quantize(final double value, final int quantizeResolution) {
        return floor(value / quantizeResolution) * quantizeResolution;
    }

    public static IntStream outFromOrigin(final int origin, final int lowerBound, final int upperBound) {
        return outFromOrigin(origin, lowerBound, upperBound, 1);
    }

    public static IntStream outFromOrigin(final int origin, final int lowerBound, final int upperBound, final int stepSize) {
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "upperBound %d expected to be > lowerBound %d", upperBound, lowerBound));
        }

        if (stepSize < 1) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "step size expected to be >= 1, was %d", stepSize));
        }

        int clampedOrigin = clamp(origin, lowerBound, upperBound);
        return IntStream.iterate(clampedOrigin, cursor -> {
            int currentDistance = Math.abs(clampedOrigin - cursor);
            return clampedOrigin - currentDistance >= lowerBound || clampedOrigin + currentDistance <= upperBound;
        }, cursor -> {
            boolean previousWasNegative = cursor <= clampedOrigin;
            int currentDistance = Math.abs(clampedOrigin - cursor);
            boolean canMovePositive = clampedOrigin + currentDistance + stepSize <= upperBound;
            if (!previousWasNegative || !canMovePositive) {
                int attemptedStep = clampedOrigin - currentDistance - (previousWasNegative ? stepSize : 0);
                if (attemptedStep >= lowerBound) {
                    return attemptedStep;
                }
            }

            return clampedOrigin + currentDistance + stepSize;
        });
    }

    public static Quaternionf rotationAroundAxis(final Vector3fc axis, final Quaternionf rotation, final Quaternionf result) {
        float projectedLength = axis.dot(rotation.x, rotation.y, rotation.z);
        return result.set(axis.x() * projectedLength, axis.y() * projectedLength, axis.z() * projectedLength, rotation.w).normalize();
    }

    public static int mulAndTruncate(final Fraction fraction, final int factor) {
        return fraction.getNumerator() * factor / fraction.getDenominator();
    }

    // ═══════════════════════ 256-bit 数学（MCRe NoiseFarlands 专属） ═══════════════════════
    // 原版 Mth 只支持 float/double/int/long；以下为 Int256/UInt256/Float256/UFloat256/DynamicNumber
    // 提供全套重载，覆盖边境之地探索 / Perlin 噪声折叠 / 高精度坐标运算场景。

    // ─── 256-bit 常量（PerlinNoise.wrap 折叠周期 2^25） ───
    private static final Int256 I256_PERIOD = Int256.of(33_554_432);      // 2^25
    private static final Int256 I256_HALF_PERIOD = Int256.of(16_777_216); // 2^24
    private static final Float256 F256_PERIOD = Float256.of(33_554_432.0);
    private static final Float256 F256_HALF = Float256.of(0.5);
    private static final UFloat256 UF256_PERIOD = UFloat256.of(33_554_432.0);
    private static final UFloat256 UF256_HALF = UFloat256.of(0.5);
    private static final UFloat256 UF256_HALF_PERIOD = UFloat256.of(16_777_216.0); // 2^24

    // ───────────────── Int256 ─────────────────

    public static Int256 abs(final Int256 v) {
        return v.abs();
    }

    public static Int256 min(final Int256 a, final Int256 b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static Int256 max(final Int256 a, final Int256 b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Int256 clamp(final Int256 value, final Int256 min, final Int256 max) {
        return max(min(value, min), max);
    }

    public static int sign(final Int256 v) {
        return v.signum();
    }

    public static Int256 square(final Int256 v) {
        return v.multiply(v);
    }

    public static Int256 absMax(final Int256 a, final Int256 b) {
        return abs(a).compareTo(abs(b)) >= 0 ? abs(a) : abs(b);
    }

    /** 向 -∞ 整除（原版 floorDiv 的 256-bit 版） */
    public static Int256 floorDiv(final Int256 a, final Int256 b) {
        if (b.isZero()) throw new ArithmeticException("/ by zero");
        Int256 q = a.divide(b); // 截断除法（向零）
        Int256 r = a.subtract(q.multiply(b));
        if (a.isNegative() != b.isNegative() && !r.isZero()) q = q.subtract(Int256.ONE);
        return q;
    }

    /** 余数符号与除数一致（原版 floorMod 的 256-bit 版） */
    public static Int256 floorMod(final Int256 a, final Int256 b) {
        return a.subtract(floorDiv(a, b).multiply(b));
    }

    /** 噪声取模核心：结果恒在 [0, mod)，与 Java 的 floorMod 一致 */
    public static Int256 positiveModulo(final Int256 input, final Int256 mod) {
        if (mod.isZero()) throw new ArithmeticException("/ by zero");
        Int256 r = input.remainder(mod);
        return r.isNegative() ? r.add(mod) : r;
    }

    /**
     * PerlinNoise.wrap() 的 Int256 精确版：
     * 原版: x - lfloor(x / 3.3554432E7 + 0.5) * 3.3554432E7，折叠到 ±2^24
     */
    public static Int256 wrap(final Int256 x) {
        Int256 q = floorDiv(x.add(I256_HALF_PERIOD), I256_PERIOD);
        return x.subtract(q.multiply(I256_PERIOD));
    }

    public static boolean isPowerOfTwo(final Int256 v) {
        return !v.isZero() && v.and(v.subtract(Int256.ONE)).isZero();
    }

    /** log2（向下取整；0 返回 -1） */
    public static int log2(final Int256 v) {
        int bits = v.bitLength();
        return bits == 0 ? -1 : bits - 1;
    }

    /** 整数线性插值（alpha 为 256-bit 浮点，结果四舍五入） */
    public static Int256 lerp(final Float256 alpha, final Int256 p0, final Int256 p1) {
        Float256 delta = Float256.of(p1.subtract(p0));
        return p0.add(alpha.multiply(delta).round());
    }

    // ───────────────── UInt256 ─────────────────

    public static UInt256 min(final UInt256 a, final UInt256 b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static UInt256 max(final UInt256 a, final UInt256 b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static UInt256 clamp(final UInt256 value, final UInt256 min, final UInt256 max) {
        return max(min(value, min), max);
    }

    public static int sign(final UInt256 v) {
        return v.signum();
    }

    public static UInt256 square(final UInt256 v) {
        return v.multiply(v);
    }

    public static UInt256 positiveModulo(final UInt256 input, final UInt256 mod) {
        if (mod.isZero()) throw new ArithmeticException("/ by zero");
        return input.remainder(mod);
    }

    /** PerlinNoise.wrap 的无符号版（折叠到 [0, 2^25)） */
    public static UInt256 wrap(final UInt256 x) {
        UInt256 q = x.add(UF256_HALF_PERIOD.toUInt256()).divide(UF256_PERIOD.toUInt256());
        return x.subtract(q.multiply(UF256_PERIOD.toUInt256()));
    }

    // ───────────────── Float256 ─────────────────

    public static Float256 abs(final Float256 v) {
        return v.abs();
    }

    public static Float256 min(final Float256 a, final Float256 b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static Float256 max(final Float256 a, final Float256 b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Float256 clamp(final Float256 value, final Float256 min, final Float256 max) {
        return value.compareTo(min) < 0 ? min : (value.compareTo(max) > 0 ? max : value);
    }

    public static int sign(final Float256 v) {
        return v.signum();
    }

    public static Float256 square(final Float256 v) {
        return v.multiply(v);
    }

    public static Int256 floor(final Float256 v) {
        return v.floor();
    }

    public static Int256 lfloor(final Float256 v) {
        return v.floor();
    }

    public static Int256 ceil(final Float256 v) {
        return v.ceil();
    }

    public static Int256 round(final Float256 v) {
        return v.round();
    }

    public static Int256 truncate(final Float256 v) {
        return v.truncate();
    }

    /** 小数部分（恒正，Float256） */
    public static Float256 frac(final Float256 v) {
        return v.subtract(Float256.of(v.floor()));
    }

    public static Float256 lerp(final Float256 alpha, final Float256 p0, final Float256 p1) {
        return p0.add(alpha.multiply(p1.subtract(p0)));
    }

    public static Float256 clampedLerp(final Float256 factor, final Float256 min, final Float256 max) {
        if (factor.compareTo(Float256.ZERO) < 0) return min;
        if (factor.compareTo(Float256.ONE) > 0) return max;
        return lerp(factor, min, max);
    }

    public static Float256 inverseLerp(final Float256 value, final Float256 min, final Float256 max) {
        return value.subtract(min).divide(max.subtract(min));
    }

    public static Float256 map(final Float256 value, final Float256 fromMin, final Float256 fromMax, final Float256 toMin, final Float256 toMax) {
        return lerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static Float256 clampedMap(final Float256 value, final Float256 fromMin, final Float256 fromMax, final Float256 toMin, final Float256 toMax) {
        return clampedLerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    /** 噪声取模：结果恒在 [0, mod) */
    public static Float256 positiveModulo(final Float256 input, final Float256 mod) {
        Float256 r = input.subtract(Float256.of(input.divide(mod).floor()).multiply(mod));
        return r.compareTo(Float256.ZERO) < 0 ? r.add(mod) : r;
    }

    /** 原版 smoothstep：x³(6x² - 15x + 10) */
    public static Float256 smoothstep(final Float256 x) {
        Float256 x2 = x.multiply(x);
        Float256 x3 = x2.multiply(x);
        return x3.multiply(x2.multiply(Float256.of(6)).subtract(x.multiply(Float256.of(15))).add(Float256.of(10)));
    }

    public static Float256 smoothstepDerivative(final Float256 x) {
        Float256 xm1 = x.subtract(Float256.ONE);
        return Float256.of(30).multiply(x.multiply(x)).multiply(xm1).multiply(xm1);
    }

    public static Float256 lengthSquared(final Float256 x, final Float256 y) {
        return x.multiply(x).add(y.multiply(y));
    }

    public static Float256 length(final Float256 x, final Float256 y) {
        return sqrt(lengthSquared(x, y));
    }

    public static Float256 lengthSquared(final Float256 x, final Float256 y, final Float256 z) {
        return x.multiply(x).add(y.multiply(y)).add(z.multiply(z));
    }

    public static Float256 length(final Float256 x, final Float256 y, final Float256 z) {
        return sqrt(lengthSquared(x, y, z));
    }

    /** 牛顿迭代平方根（收敛到 176-bit 尾数，比 Float256.sqrt 的 double 近似更精确） */
    public static Float256 sqrt(final Float256 x) {
        if (x.isZero()) return Float256.ZERO;
        if (x.isNaN()) return Float256.NaN;
        if (x.isInfinity()) return x.signum() < 0 ? Float256.NaN : Float256.POS_INF;
        if (x.signum() < 0) return Float256.NaN;
        Float256 guess = Float256.of(Math.sqrt(x.doubleValue()));
        if (!guess.isFinite()) return guess; // double 溢出保护
        for (int i = 0; i < 6; i++) {
            guess = guess.add(x.divide(guess)).multiply(F256_HALF);
        }
        return guess;
    }

    /** PerlinNoise.wrap 的 Float256 版：x - floor(x/2^25 + 0.5) × 2^25 */
    public static Float256 wrap(final Float256 x) {
        Int256 q = x.divide(F256_PERIOD).add(F256_HALF).floor();
        return x.subtract(Float256.of(q).multiply(F256_PERIOD));
    }

    public static Float256 triangleWave(final Float256 index, final Float256 period) {
        Float256 half = period.multiply(F256_HALF);
        Float256 quarter = period.multiply(F256_HALF).multiply(F256_HALF);
        // index % period（有符号余数，与 Java % 语义一致）
        Int256 q = index.divide(period).truncate();
        Float256 rem = index.subtract(Float256.of(q).multiply(period));
        return rem.subtract(half).abs().subtract(quarter).divide(quarter);
    }

    // ───────────────── UFloat256 ─────────────────

    public static UFloat256 abs(final UFloat256 v) {
        return v;
    }

    public static UFloat256 min(final UFloat256 a, final UFloat256 b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static UFloat256 max(final UFloat256 a, final UFloat256 b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static UFloat256 clamp(final UFloat256 value, final UFloat256 min, final UFloat256 max) {
        return value.compareTo(min) < 0 ? min : (value.compareTo(max) > 0 ? max : value);
    }

    public static int sign(final UFloat256 v) {
        return v.isZero() ? 0 : 1;
    }

    public static UFloat256 square(final UFloat256 v) {
        return v.multiply(v);
    }

    public static UInt256 floor(final UFloat256 v) {
        return v.floor();
    }

    public static UInt256 ceil(final UFloat256 v) {
        return v.ceil();
    }

    public static UInt256 round(final UFloat256 v) {
        return v.round();
    }

    public static UFloat256 frac(final UFloat256 v) {
        return v.subtract(UFloat256.of(v.floor()));
    }

    public static UFloat256 lerp(final UFloat256 alpha, final UFloat256 p0, final UFloat256 p1) {
        return p0.add(alpha.multiply(p1.subtract(p0)));
    }

    public static UFloat256 clampedLerp(final UFloat256 factor, final UFloat256 min, final UFloat256 max) {
        if (factor.compareTo(UFloat256.ZERO) < 0) return min;
        if (factor.compareTo(UFloat256.ONE) > 0) return max;
        return lerp(factor, min, max);
    }

    public static UFloat256 inverseLerp(final UFloat256 value, final UFloat256 min, final UFloat256 max) {
        return value.subtract(min).divide(max.subtract(min));
    }

    public static UFloat256 positiveModulo(final UFloat256 input, final UFloat256 mod) {
        return input.subtract(UFloat256.of(input.divide(mod).floor()).multiply(mod));
    }

    public static UFloat256 smoothstep(final UFloat256 x) {
        UFloat256 x2 = x.multiply(x);
        UFloat256 x3 = x2.multiply(x);
        return x3.multiply(x2.multiply(UFloat256.of(6)).subtract(x.multiply(UFloat256.of(15))).add(UFloat256.of(10)));
    }

    public static UFloat256 lengthSquared(final UFloat256 x, final UFloat256 y, final UFloat256 z) {
        return x.multiply(x).add(y.multiply(y)).add(z.multiply(z));
    }

    public static UFloat256 length(final UFloat256 x, final UFloat256 y, final UFloat256 z) {
        return sqrt(lengthSquared(x, y, z));
    }

    /** 牛顿迭代平方根 */
    public static UFloat256 sqrt(final UFloat256 x) {
        if (x.isZero()) return UFloat256.ZERO;
        if (x.isNaN()) return UFloat256.NaN;
        if (x.isInfinity()) return UFloat256.INF;
        UFloat256 guess = UFloat256.of(Math.sqrt(x.doubleValue()));
        if (guess.isNaN() || guess.isInfinity()) return guess;
        for (int i = 0; i < 6; i++) {
            guess = guess.add(x.divide(guess)).multiply(UF256_HALF);
        }
        return guess;
    }

    /** PerlinNoise.wrap 的 UFloat256 版 */
    public static UFloat256 wrap(final UFloat256 x) {
        UInt256 q = x.divide(UF256_PERIOD).add(UF256_HALF).floor();
        return x.subtract(UFloat256.of(q).multiply(UF256_PERIOD));
    }

    // ───────────────── DynamicNumber（统一容器便捷重载） ─────────────────

    public static DynamicNumber abs(final DynamicNumber v) {
        return v.abs();
    }

    public static DynamicNumber min(final DynamicNumber a, final DynamicNumber b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static DynamicNumber max(final DynamicNumber a, final DynamicNumber b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static DynamicNumber clamp(final DynamicNumber value, final DynamicNumber min, final DynamicNumber max) {
        return max(min(value, min), max);
    }

    public static int sign(final DynamicNumber v) {
        return v.compareTo(DynamicNumber.ZERO);
    }

    public static DynamicNumber lerp(final DynamicNumber alpha, final DynamicNumber p0, final DynamicNumber p1) {
        return p0.add(alpha.multiply(p1.subtract(p0)));
    }

    static {
        for (int ind = 0; ind < 257; ind++) {
            double v = ind / 256.0;
            double asinv = Math.asin(v);
            COS_TAB[ind] = Math.cos(asinv);
            ASIN_TAB[ind] = asinv;
        }
    }
}