package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
   public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
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
         sin[i] = (float)Math.sin(i / 10430.378350470453);
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
      return SIN[(int)((long)(i * 10430.378350470453) & 65535L)];
   }

   public static float cos(final double i) {
      return SIN[(int)((long)(i * 10430.378350470453 + 16384.0) & 65535L)];
   }

   public static float sqrt(final float x) {
      return (float)Math.sqrt(x);
   }

   public static int floor(final float v) {
      return (int)Math.floor(v);
   }

   public static int floor(final double v) {
      return (int)Math.floor(v);
   }

   public static long lfloor(final double v) {
      return (long)Math.floor(v);
   }

   public static float abs(final float v) {
      return Math.abs(v);
   }

   public static int abs(final int v) {
      return Math.abs(v);
   }

   public static int ceil(final float v) {
      return (int)Math.ceil(v);
   }

   public static int ceil(final double v) {
      return (int)Math.ceil(v);
   }

   public static long ceilLong(final double v) {
      return (long)Math.ceil(v);
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
      return (byte)floor(angle * 256.0F / 360.0F);
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
      float normalizedAngle = (float)(angle % 360L);
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
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(input * 125613361L >> 27) & 31];
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
      int index = (int)Double.doubleToRawLongBits(yp);
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
      int h = (int)(hue * 6.0F) % 6;
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

      return ARGB.color(alpha, clamp((int)(red * 255.0F), 0, 255), clamp((int)(green * 255.0F), 0, 255), clamp((int)(blue * 255.0F), 0, 255));
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
      final double x111
   ) {
      return lerp(alpha3, lerp2(alpha1, alpha2, x000, x100, x010, x110), lerp2(alpha1, alpha2, x001, x101, x011, x111));
   }

   public static float catmullrom(final float alpha, final float p0, final float p1, final float p2, final float p3) {
      return 0.5F
         * (
            2.0F * p1
               + (p2 - p0) * alpha
               + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * alpha * alpha
               + (3.0F * p1 - p0 - 3.0F * p2 + p3) * alpha * alpha * alpha
         );
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
      return mean + (float)random.nextGaussian() * deviation;
   }

   public static double lengthSquared(final double x, final double y) {
      return x * x + y * y;
   }

   public static double length(final double x, final double y) {
      return Math.sqrt(lengthSquared(x, y));
   }

   public static float length(final float x, final float y) {
      return (float)Math.sqrt(lengthSquared(x, y));
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

   static {
      for (int ind = 0; ind < 257; ind++) {
         double v = ind / 256.0;
         double asinv = Math.asin(v);
         COS_TAB[ind] = Math.cos(asinv);
         ASIN_TAB[ind] = asinv;
      }
   }
}
