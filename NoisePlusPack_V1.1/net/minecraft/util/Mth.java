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
   public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
   public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
   public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
   private static final int SIN_QUANTIZATION = 65536;
   private static final int SIN_MASK = 65535;
   private static final int COS_OFFSET = 16384;
   private static final double SIN_SCALE = 10430.378350470453;
   private static final float[] SIN = Util.make(new float[65536], p_449284_ -> {
      for (int ix = 0; ix < p_449284_.length; ix++) {
         p_449284_[ix] = (float)Math.sin(ix / 10430.378350470453);
      }
   });
   private static final RandomSource RANDOM = RandomSource.createThreadSafe();
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
   };
   private static final double ONE_SIXTH = 0.16666666666666666;
   private static final int FRAC_EXP = 8;
   private static final int LUT_SIZE = 257;
   private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
   private static final double[] ASIN_TAB = new double[257];
   private static final double[] COS_TAB = new double[257];

   public static float sin(double p_457266_) {
      return SIN[(int)((long)(p_457266_ * 10430.378350470453) & 65535L)];
   }

   public static float cos(double p_455002_) {
      return SIN[(int)((long)(p_455002_ * 10430.378350470453 + 16384.0) & 65535L)];
   }

   public static float sqrt(float p_14117_) {
      return (float)Math.sqrt(p_14117_);
   }

   public static int floor(float p_14144_) {
      int i = (int)p_14144_;
      return p_14144_ < i ? i - 1 : i;
   }

   public static int floor(double p_14108_) {
      int i = (int)p_14108_;
      return p_14108_ < i ? i - 1 : i;
   }

   public static long lfloor(double p_14135_) {
      long i = (long)p_14135_;
      return p_14135_ < i ? i - 1L : i;
   }

   public static float abs(float p_14155_) {
      return Math.abs(p_14155_);
   }

   public static int abs(int p_14041_) {
      return Math.abs(p_14041_);
   }

   public static int ceil(float p_14168_) {
      int i = (int)p_14168_;
      return p_14168_ > i ? i + 1 : i;
   }

   public static int ceil(double p_14166_) {
      int i = (int)p_14166_;
      return p_14166_ > i ? i + 1 : i;
   }

   public static long ceilLong(double p_426023_) {
      long i = (long)p_426023_;
      return p_426023_ > i ? i + 1L : i;
   }

   public static int clamp(int p_14046_, int p_14047_, int p_14048_) {
      return Math.min(Math.max(p_14046_, p_14047_), p_14048_);
   }

   public static long clamp(long p_300696_, long p_298059_, long p_299237_) {
      return Math.min(Math.max(p_300696_, p_298059_), p_299237_);
   }

   public static float clamp(float p_14037_, float p_14038_, float p_14039_) {
      return p_14037_ < p_14038_ ? p_14038_ : Math.min(p_14037_, p_14039_);
   }

   public static double clamp(double p_14009_, double p_14010_, double p_14011_) {
      return p_14009_ < p_14010_ ? p_14010_ : Math.min(p_14009_, p_14011_);
   }

   public static double clampedLerp(double p_14086_, double p_14087_, double p_14088_) {
      if (p_14086_ < 0.0) {
         return p_14087_;
      } else {
         return p_14086_ > 1.0 ? p_14088_ : lerp(p_14086_, p_14087_, p_14088_);
      }
   }

   public static float clampedLerp(float p_144921_, float p_144922_, float p_144923_) {
      if (p_144921_ < 0.0F) {
         return p_144922_;
      } else {
         return p_144921_ > 1.0F ? p_144923_ : lerp(p_144921_, p_144922_, p_144923_);
      }
   }

   public static int absMax(int p_461125_, int p_461119_) {
      return Math.max(Math.abs(p_461125_), Math.abs(p_461119_));
   }

   public static float absMax(float p_461123_, float p_461121_) {
      return Math.max(Math.abs(p_461123_), Math.abs(p_461121_));
   }

   public static double absMax(double p_14006_, double p_14007_) {
      return Math.max(Math.abs(p_14006_), Math.abs(p_14007_));
   }

   public static int chessboardDistance(int p_461126_, int p_461122_, int p_461116_, int p_461120_) {
      return absMax(p_461116_ - p_461126_, p_461120_ - p_461122_);
   }

   public static int floorDiv(int p_14043_, int p_14044_) {
      return Math.floorDiv(p_14043_, p_14044_);
   }

   public static int nextInt(RandomSource p_216272_, int p_216273_, int p_216274_) {
      return p_216273_ >= p_216274_ ? p_216273_ : p_216272_.nextInt(p_216274_ - p_216273_ + 1) + p_216273_;
   }

   public static float nextFloat(RandomSource p_216268_, float p_216269_, float p_216270_) {
      return p_216269_ >= p_216270_ ? p_216269_ : p_216268_.nextFloat() * (p_216270_ - p_216269_) + p_216269_;
   }

   public static double nextDouble(RandomSource p_216264_, double p_216265_, double p_216266_) {
      return p_216265_ >= p_216266_ ? p_216265_ : p_216264_.nextDouble() * (p_216266_ - p_216265_) + p_216265_;
   }

   public static boolean equal(float p_14034_, float p_14035_) {
      return Math.abs(p_14035_ - p_14034_) < 1.0E-5F;
   }

   public static boolean equal(double p_14083_, double p_14084_) {
      return Math.abs(p_14084_ - p_14083_) < 1.0E-5F;
   }

   public static int positiveModulo(int p_14101_, int p_14102_) {
      return Math.floorMod(p_14101_, p_14102_);
   }

   public static float positiveModulo(float p_14092_, float p_14093_) {
      return (p_14092_ % p_14093_ + p_14093_) % p_14093_;
   }

   public static double positiveModulo(double p_14110_, double p_14111_) {
      return (p_14110_ % p_14111_ + p_14111_) % p_14111_;
   }

   public static boolean isMultipleOf(int p_265754_, int p_265543_) {
      return p_265754_ % p_265543_ == 0;
   }

   public static byte packDegrees(float p_362809_) {
      return (byte)floor(p_362809_ * 256.0F / 360.0F);
   }

   public static float unpackDegrees(byte p_363805_) {
      return p_363805_ * 360 / 256.0F;
   }

   public static int wrapDegrees(int p_14099_) {
      int i = p_14099_ % 360;
      if (i >= 180) {
         i -= 360;
      }

      if (i < -180) {
         i += 360;
      }

      return i;
   }

   public static float wrapDegrees(long p_367823_) {
      float f = (float)(p_367823_ % 360L);
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   public static float wrapDegrees(float p_14178_) {
      float f = p_14178_ % 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   public static double wrapDegrees(double p_14176_) {
      double d0 = p_14176_ % 360.0;
      if (d0 >= 180.0) {
         d0 -= 360.0;
      }

      if (d0 < -180.0) {
         d0 += 360.0;
      }

      return d0;
   }

   public static float degreesDifference(float p_14119_, float p_14120_) {
      return wrapDegrees(p_14120_ - p_14119_);
   }

   public static float degreesDifferenceAbs(float p_14146_, float p_14147_) {
      return abs(degreesDifference(p_14146_, p_14147_));
   }

   public static float rotateIfNecessary(float p_14095_, float p_14096_, float p_14097_) {
      float f = degreesDifference(p_14095_, p_14096_);
      float f1 = clamp(f, -p_14097_, p_14097_);
      return p_14096_ - f1;
   }

   public static float approach(float p_14122_, float p_14123_, float p_14124_) {
      p_14124_ = abs(p_14124_);
      return p_14122_ < p_14123_ ? clamp(p_14122_ + p_14124_, p_14122_, p_14123_) : clamp(p_14122_ - p_14124_, p_14123_, p_14122_);
   }

   public static float approachDegrees(float p_14149_, float p_14150_, float p_14151_) {
      float f = degreesDifference(p_14149_, p_14150_);
      return approach(p_14149_, p_14149_ + f, p_14151_);
   }

   public static int getInt(String p_14060_, int p_14061_) {
      return NumberUtils.toInt(p_14060_, p_14061_);
   }

   public static int smallestEncompassingPowerOfTwo(int p_14126_) {
      int i = p_14126_ - 1;
      i |= i >> 1;
      i |= i >> 2;
      i |= i >> 4;
      i |= i >> 8;
      i |= i >> 16;
      return i + 1;
   }

   public static int smallestSquareSide(int p_410615_) {
      if (p_410615_ < 0) {
         throw new IllegalArgumentException("itemCount must be greater than or equal to zero");
      } else {
         return ceil(Math.sqrt(p_410615_));
      }
   }

   public static boolean isPowerOfTwo(int p_14153_) {
      return p_14153_ != 0 && (p_14153_ & p_14153_ - 1) == 0;
   }

   public static int ceillog2(int p_14164_) {
      p_14164_ = isPowerOfTwo(p_14164_) ? p_14164_ : smallestEncompassingPowerOfTwo(p_14164_);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(p_14164_ * 125613361L >> 27) & 31];
   }

   public static int log2(int p_14174_) {
      return ceillog2(p_14174_) - (isPowerOfTwo(p_14174_) ? 0 : 1);
   }

   public static float frac(float p_14188_) {
      return p_14188_ - floor(p_14188_);
   }

   public static double frac(double p_14186_) {
      return p_14186_ - lfloor(p_14186_);
   }

   @Deprecated
   public static long getSeed(Vec3i p_14058_) {
      return getSeed(p_14058_.getX(), p_14058_.getY(), p_14058_.getZ());
   }

   @Deprecated
   public static long getSeed(int p_14131_, int p_14132_, int p_14133_) {
      long i = p_14131_ * 3129871 ^ p_14133_ * 116129781L ^ p_14132_;
      i = i * i * 42317861L + i * 11L;
      return i >> 16;
   }

   public static UUID createInsecureUUID(RandomSource p_216262_) {
      long i = p_216262_.nextLong() & -61441L | 16384L;
      long j = p_216262_.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
      return new UUID(i, j);
   }

   public static UUID createInsecureUUID() {
      return createInsecureUUID(RANDOM);
   }

   public static double inverseLerp(double p_14113_, double p_14114_, double p_14115_) {
      return (p_14113_ - p_14114_) / (p_14115_ - p_14114_);
   }

   public static float inverseLerp(float p_184656_, float p_184657_, float p_184658_) {
      return (p_184656_ - p_184657_) / (p_184658_ - p_184657_);
   }

   public static boolean rayIntersectsAABB(Vec3 p_144889_, Vec3 p_144890_, AABB p_144891_) {
      double d0 = (p_144891_.minX + p_144891_.maxX) * 0.5;
      double d1 = (p_144891_.maxX - p_144891_.minX) * 0.5;
      double d2 = p_144889_.x - d0;
      if (Math.abs(d2) > d1 && d2 * p_144890_.x >= 0.0) {
         return false;
      }

      double d3 = (p_144891_.minY + p_144891_.maxY) * 0.5;
      double d4 = (p_144891_.maxY - p_144891_.minY) * 0.5;
      double d5 = p_144889_.y - d3;
      if (Math.abs(d5) > d4 && d5 * p_144890_.y >= 0.0) {
         return false;
      }

      double d6 = (p_144891_.minZ + p_144891_.maxZ) * 0.5;
      double d7 = (p_144891_.maxZ - p_144891_.minZ) * 0.5;
      double d8 = p_144889_.z - d6;
      if (Math.abs(d8) > d7 && d8 * p_144890_.z >= 0.0) {
         return false;
      }

      double d9 = Math.abs(p_144890_.x);
      double d10 = Math.abs(p_144890_.y);
      double d11 = Math.abs(p_144890_.z);
      double d12 = p_144890_.y * d8 - p_144890_.z * d5;
      if (Math.abs(d12) > d4 * d11 + d7 * d10) {
         return false;
      }

      d12 = p_144890_.z * d2 - p_144890_.x * d8;
      if (Math.abs(d12) > d1 * d11 + d7 * d9) {
         return false;
      }

      d12 = p_144890_.x * d5 - p_144890_.y * d2;
      return Math.abs(d12) < d1 * d10 + d4 * d9;
   }

   public static double atan2(double p_14137_, double p_14138_) {
      double d0 = p_14138_ * p_14138_ + p_14137_ * p_14137_;
      if (Double.isNaN(d0)) {
         return Double.NaN;
      }

      boolean flag = p_14137_ < 0.0;
      if (flag) {
         p_14137_ = -p_14137_;
      }

      boolean flag1 = p_14138_ < 0.0;
      if (flag1) {
         p_14138_ = -p_14138_;
      }

      boolean flag2 = p_14137_ > p_14138_;
      if (flag2) {
         double d1 = p_14138_;
         p_14138_ = p_14137_;
         p_14137_ = d1;
      }

      double d9 = fastInvSqrt(d0);
      p_14138_ *= d9;
      p_14137_ *= d9;
      double d2 = FRAC_BIAS + p_14137_;
      int i = (int)Double.doubleToRawLongBits(d2);
      double d3 = ASIN_TAB[i];
      double d4 = COS_TAB[i];
      double d5 = d2 - FRAC_BIAS;
      double d6 = p_14137_ * d4 - p_14138_ * d5;
      double d7 = (6.0 + d6 * d6) * d6 * 0.16666666666666666;
      double d8 = d3 + d7;
      if (flag2) {
         d8 = (Math.PI / 2) - d8;
      }

      if (flag1) {
         d8 = Math.PI - d8;
      }

      if (flag) {
         d8 = -d8;
      }

      return d8;
   }

   public static float invSqrt(float p_265060_) {
      return org.joml.Math.invsqrt(p_265060_);
   }

   public static double invSqrt(double p_265088_) {
      return org.joml.Math.invsqrt(p_265088_);
   }

   @Deprecated
   public static double fastInvSqrt(double p_14194_) {
      double d0 = 0.5 * p_14194_;
      long i = Double.doubleToRawLongBits(p_14194_);
      i = 6910469410427058090L - (i >> 1);
      p_14194_ = Double.longBitsToDouble(i);
      return p_14194_ * (1.5 - d0 * p_14194_ * p_14194_);
   }

   public static float fastInvCubeRoot(float p_14200_) {
      int i = Float.floatToIntBits(p_14200_);
      i = 1419967116 - i / 3;
      float f = Float.intBitsToFloat(i);
      f = 0.6666667F * f + 1.0F / (3.0F * f * f * p_14200_);
      return 0.6666667F * f + 1.0F / (3.0F * f * f * p_14200_);
   }

   public static int hsvToRgb(float p_14170_, float p_14171_, float p_14172_) {
      return hsvToArgb(p_14170_, p_14171_, p_14172_, 0);
   }

   public static int hsvToArgb(float p_343715_, float p_343707_, float p_342108_, int p_343055_) {
      int i = (int)(p_343715_ * 6.0F) % 6;
      float f = p_343715_ * 6.0F - i;
      float f1 = p_342108_ * (1.0F - p_343707_);
      float f2 = p_342108_ * (1.0F - f * p_343707_);
      float f3 = p_342108_ * (1.0F - (1.0F - f) * p_343707_);
      float f4;
      float f5;
      float f6;
      switch (i) {
         case 0:
            f4 = p_342108_;
            f5 = f3;
            f6 = f1;
            break;
         case 1:
            f4 = f2;
            f5 = p_342108_;
            f6 = f1;
            break;
         case 2:
            f4 = f1;
            f5 = p_342108_;
            f6 = f3;
            break;
         case 3:
            f4 = f1;
            f5 = f2;
            f6 = p_342108_;
            break;
         case 4:
            f4 = f3;
            f5 = f1;
            f6 = p_342108_;
            break;
         case 5:
            f4 = p_342108_;
            f5 = f1;
            f6 = f2;
            break;
         default:
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + p_343715_ + ", " + p_343707_ + ", " + p_342108_);
      }

      return ARGB.color(p_343055_, clamp((int)(f4 * 255.0F), 0, 255), clamp((int)(f5 * 255.0F), 0, 255), clamp((int)(f6 * 255.0F), 0, 255));
   }

   public static int murmurHash3Mixer(int p_14184_) {
      p_14184_ ^= p_14184_ >>> 16;
      p_14184_ *= -2048144789;
      p_14184_ ^= p_14184_ >>> 13;
      p_14184_ *= -1028477387;
      return p_14184_ ^ p_14184_ >>> 16;
   }

   public static int binarySearch(int p_14050_, int p_14051_, IntPredicate p_14052_) {
      int i = p_14051_ - p_14050_;

      while (i > 0) {
         int j = i / 2;
         int k = p_14050_ + j;
         if (p_14052_.test(k)) {
            i = j;
         } else {
            p_14050_ = k + 1;
            i -= j + 1;
         }
      }

      return p_14050_;
   }

   public static int lerpInt(float p_270245_, int p_270597_, int p_270301_) {
      return p_270597_ + floor(p_270245_ * (p_270301_ - p_270597_));
   }

   public static int lerpDiscrete(float p_298397_, int p_298459_, int p_300125_) {
      int i = p_300125_ - p_298459_;
      return p_298459_ + floor(p_298397_ * (i - 1)) + (p_298397_ > 0.0F ? 1 : 0);
   }

   public static float lerp(float p_14180_, float p_14181_, float p_14182_) {
      return p_14181_ + p_14180_ * (p_14182_ - p_14181_);
   }

   public static Vec3 lerp(double p_364482_, Vec3 p_369943_, Vec3 p_369682_) {
      return new Vec3(lerp(p_364482_, p_369943_.x, p_369682_.x), lerp(p_364482_, p_369943_.y, p_369682_.y), lerp(p_364482_, p_369943_.z, p_369682_.z));
   }

   public static double lerp(double p_14140_, double p_14141_, double p_14142_) {
      return p_14141_ + p_14140_ * (p_14142_ - p_14141_);
   }

   public static double lerp2(double p_14013_, double p_14014_, double p_14015_, double p_14016_, double p_14017_, double p_14018_) {
      return lerp(p_14014_, lerp(p_14013_, p_14015_, p_14016_), lerp(p_14013_, p_14017_, p_14018_));
   }

   public static double lerp3(
      double p_14020_,
      double p_14021_,
      double p_14022_,
      double p_14023_,
      double p_14024_,
      double p_14025_,
      double p_14026_,
      double p_14027_,
      double p_14028_,
      double p_14029_,
      double p_14030_
   ) {
      return lerp(
         p_14022_, lerp2(p_14020_, p_14021_, p_14023_, p_14024_, p_14025_, p_14026_), lerp2(p_14020_, p_14021_, p_14027_, p_14028_, p_14029_, p_14030_)
      );
   }

   public static float catmullrom(float p_216245_, float p_216246_, float p_216247_, float p_216248_, float p_216249_) {
      return 0.5F
         * (
            2.0F * p_216247_
               + (p_216248_ - p_216246_) * p_216245_
               + (2.0F * p_216246_ - 5.0F * p_216247_ + 4.0F * p_216248_ - p_216249_) * p_216245_ * p_216245_
               + (3.0F * p_216247_ - p_216246_ - 3.0F * p_216248_ + p_216249_) * p_216245_ * p_216245_ * p_216245_
         );
   }

   public static double smoothstep(double p_14198_) {
      return p_14198_ * p_14198_ * p_14198_ * (p_14198_ * (p_14198_ * 6.0 - 15.0) + 10.0);
   }

   public static double smoothstepDerivative(double p_144947_) {
      return 30.0 * p_144947_ * p_144947_ * (p_144947_ - 1.0) * (p_144947_ - 1.0);
   }

   public static int sign(double p_14206_) {
      if (p_14206_ == 0.0) {
         return 0;
      } else {
         return p_14206_ > 0.0 ? 1 : -1;
      }
   }

   public static float rotLerp(float p_14190_, float p_14191_, float p_14192_) {
      return p_14191_ + p_14190_ * wrapDegrees(p_14192_ - p_14191_);
   }

   public static double rotLerp(double p_299352_, double p_301019_, double p_299027_) {
      return p_301019_ + p_299352_ * wrapDegrees(p_299027_ - p_301019_);
   }

   public static float rotLerpRad(float p_368518_, float p_366477_, float p_363810_) {
      float f = p_363810_ - p_366477_;

      while (f < (float) -Math.PI) {
         f += (float) (Math.PI * 2);
      }

      while (f >= (float) Math.PI) {
         f -= (float) (Math.PI * 2);
      }

      return p_366477_ + p_368518_ * f;
   }

   public static float triangleWave(float p_14157_, float p_14158_) {
      return (Math.abs(p_14157_ % p_14158_ - p_14158_ * 0.5F) - p_14158_ * 0.25F) / (p_14158_ * 0.25F);
   }

   public static float square(float p_14208_) {
      return p_14208_ * p_14208_;
   }

   public static float cube(float p_457020_) {
      return p_457020_ * p_457020_ * p_457020_;
   }

   public static double square(double p_144953_) {
      return p_144953_ * p_144953_;
   }

   public static int square(int p_144945_) {
      return p_144945_ * p_144945_;
   }

   public static long square(long p_184644_) {
      return p_184644_ * p_184644_;
   }

   public static double clampedMap(double p_144852_, double p_144853_, double p_144854_, double p_144855_, double p_144856_) {
      return clampedLerp(inverseLerp(p_144852_, p_144853_, p_144854_), p_144855_, p_144856_);
   }

   public static float clampedMap(float p_184632_, float p_184633_, float p_184634_, float p_184635_, float p_184636_) {
      return clampedLerp(inverseLerp(p_184632_, p_184633_, p_184634_), p_184635_, p_184636_);
   }

   public static double map(double p_144915_, double p_144916_, double p_144917_, double p_144918_, double p_144919_) {
      return lerp(inverseLerp(p_144915_, p_144916_, p_144917_), p_144918_, p_144919_);
   }

   public static float map(float p_184638_, float p_184639_, float p_184640_, float p_184641_, float p_184642_) {
      return lerp(inverseLerp(p_184638_, p_184639_, p_184640_), p_184641_, p_184642_);
   }

   public static double wobble(double p_144955_) {
      return p_144955_ + (2.0 * RandomSource.create(floor(p_144955_ * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
   }

   public static int roundToward(int p_144942_, int p_144943_) {
      return positiveCeilDiv(p_144942_, p_144943_) * p_144943_;
   }

   public static int positiveCeilDiv(int p_184653_, int p_184654_) {
      return -Math.floorDiv(-p_184653_, p_184654_);
   }

   public static int randomBetweenInclusive(RandomSource p_216288_, int p_216289_, int p_216290_) {
      return p_216288_.nextInt(p_216290_ - p_216289_ + 1) + p_216289_;
   }

   public static float randomBetween(RandomSource p_216284_, float p_216285_, float p_216286_) {
      return p_216284_.nextFloat() * (p_216286_ - p_216285_) + p_216285_;
   }

   public static float normal(RandomSource p_216292_, float p_216293_, float p_216294_) {
      return p_216293_ + (float)p_216292_.nextGaussian() * p_216294_;
   }

   public static double lengthSquared(double p_211590_, double p_211591_) {
      return p_211590_ * p_211590_ + p_211591_ * p_211591_;
   }

   public static double length(double p_184646_, double p_184647_) {
      return Math.sqrt(lengthSquared(p_184646_, p_184647_));
   }

   public static float length(float p_366616_, float p_361952_) {
      return (float)Math.sqrt(lengthSquared(p_366616_, p_361952_));
   }

   public static double lengthSquared(double p_211593_, double p_211594_, double p_211595_) {
      return p_211593_ * p_211593_ + p_211594_ * p_211594_ + p_211595_ * p_211595_;
   }

   public static double length(double p_184649_, double p_184650_, double p_184651_) {
      return Math.sqrt(lengthSquared(p_184649_, p_184650_, p_184651_));
   }

   public static float lengthSquared(float p_343908_, float p_343799_, float p_344922_) {
      return p_343908_ * p_343908_ + p_343799_ * p_343799_ + p_344922_ * p_344922_;
   }

   public static int quantize(double p_184629_, int p_184630_) {
      return floor(p_184629_ / p_184630_) * p_184630_;
   }

   public static IntStream outFromOrigin(int p_216296_, int p_216297_, int p_216298_) {
      return outFromOrigin(p_216296_, p_216297_, p_216298_, 1);
   }

   public static IntStream outFromOrigin(int p_216251_, int p_216252_, int p_216253_, int p_216254_) {
      if (p_216252_ > p_216253_) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "upperBound %d expected to be > lowerBound %d", p_216253_, p_216252_));
      }

      if (p_216254_ < 1) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "step size expected to be >= 1, was %d", p_216254_));
      }

      int i = clamp(p_216251_, p_216252_, p_216253_);
      return IntStream.iterate(i, p_216282_ -> {
         int j = Math.abs(i - p_216282_);
         return i - j >= p_216252_ || i + j <= p_216253_;
      }, p_216260_ -> {
         boolean flag = p_216260_ <= i;
         int j = Math.abs(i - p_216260_);
         boolean flag1 = i + j + p_216254_ <= p_216253_;
         if (!flag || !flag1) {
            int k = i - j - (flag ? p_216254_ : 0);
            if (k >= p_216252_) {
               return k;
            }
         }

         return i + j + p_216254_;
      });
   }

   public static Quaternionf rotationAroundAxis(Vector3f p_311556_, Quaternionf p_311192_, Quaternionf p_310462_) {
      float f = p_311556_.dot(p_311192_.x, p_311192_.y, p_311192_.z);
      return p_310462_.set(p_311556_.x * f, p_311556_.y * f, p_311556_.z * f, p_311192_.w).normalize();
   }

   public static int mulAndTruncate(Fraction p_331603_, int p_334561_) {
      return p_331603_.getNumerator() * p_334561_ / p_331603_.getDenominator();
   }

   static {
      for (int i = 0; i < 257; i++) {
         double d0 = i / 256.0;
         double d1 = Math.asin(d0);
         COS_TAB[i] = Math.cos(d1);
         ASIN_TAB[i] = d1;
      }
   }
}
