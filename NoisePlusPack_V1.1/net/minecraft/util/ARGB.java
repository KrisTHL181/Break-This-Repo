package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ARGB {
   private static final int LINEAR_CHANNEL_DEPTH = 1024;
   private static final short[] SRGB_TO_LINEAR = Util.make(new short[256], p_460433_ -> {
      for (int i = 0; i < p_460433_.length; i++) {
         float f = i / 255.0F;
         p_460433_[i] = (short)Math.round(computeSrgbToLinear(f) * 1023.0F);
      }
   });
   private static final byte[] LINEAR_TO_SRGB = Util.make(new byte[1024], p_453372_ -> {
      for (int i = 0; i < p_453372_.length; i++) {
         float f = i / 1023.0F;
         p_453372_[i] = (byte)Math.round(computeLinearToSrgb(f) * 255.0F);
      }
   });

   private static float computeSrgbToLinear(float p_454092_) {
      return p_454092_ >= 0.04045F ? (float)Math.pow((p_454092_ + 0.055) / 1.055, 2.4) : p_454092_ / 12.92F;
   }

   private static float computeLinearToSrgb(float p_455430_) {
      return p_455430_ >= 0.0031308F ? (float)(1.055 * Math.pow(p_455430_, 0.4166666666666667) - 0.055) : 12.92F * p_455430_;
   }

   public static float srgbToLinearChannel(int p_458774_) {
      return SRGB_TO_LINEAR[p_458774_] / 1023.0F;
   }

   public static int linearToSrgbChannel(float p_456545_) {
      return LINEAR_TO_SRGB[Mth.floor(p_456545_ * 1023.0F)] & 0xFF;
   }

   public static int meanLinear(int p_454657_, int p_450176_, int p_451556_, int p_452943_) {
      return color(
         (alpha(p_454657_) + alpha(p_450176_) + alpha(p_451556_) + alpha(p_452943_)) / 4,
         linearChannelMean(red(p_454657_), red(p_450176_), red(p_451556_), red(p_452943_)),
         linearChannelMean(green(p_454657_), green(p_450176_), green(p_451556_), green(p_452943_)),
         linearChannelMean(blue(p_454657_), blue(p_450176_), blue(p_451556_), blue(p_452943_))
      );
   }

   private static int linearChannelMean(int p_459814_, int p_459030_, int p_452850_, int p_457272_) {
      int i = (SRGB_TO_LINEAR[p_459814_] + SRGB_TO_LINEAR[p_459030_] + SRGB_TO_LINEAR[p_452850_] + SRGB_TO_LINEAR[p_457272_]) / 4;
      return LINEAR_TO_SRGB[i] & 0xFF;
   }

   public static int alpha(int p_362339_) {
      return p_362339_ >>> 24;
   }

   public static int red(int p_363530_) {
      return p_363530_ >> 16 & 0xFF;
   }

   public static int green(int p_362707_) {
      return p_362707_ >> 8 & 0xFF;
   }

   public static int blue(int p_367010_) {
      return p_367010_ & 0xFF;
   }

   public static int color(int p_365053_, int p_365624_, int p_367179_, int p_364375_) {
      return (p_365053_ & 0xFF) << 24 | (p_365624_ & 0xFF) << 16 | (p_367179_ & 0xFF) << 8 | p_364375_ & 0xFF;
   }

   public static int color(int p_368038_, int p_364189_, int p_366166_) {
      return color(255, p_368038_, p_364189_, p_366166_);
   }

   public static int color(Vec3 p_368690_) {
      return color(as8BitChannel((float)p_368690_.x()), as8BitChannel((float)p_368690_.y()), as8BitChannel((float)p_368690_.z()));
   }

   public static int multiply(int p_368908_, int p_362670_) {
      if (p_368908_ == -1) {
         return p_362670_;
      } else {
         return p_362670_ == -1
            ? p_368908_
            : color(
               alpha(p_368908_) * alpha(p_362670_) / 255,
               red(p_368908_) * red(p_362670_) / 255,
               green(p_368908_) * green(p_362670_) / 255,
               blue(p_368908_) * blue(p_362670_) / 255
            );
      }
   }

   public static int addRgb(int p_454308_, int p_451175_) {
      return color(
         alpha(p_454308_),
         Math.min(red(p_454308_) + red(p_451175_), 255),
         Math.min(green(p_454308_) + green(p_451175_), 255),
         Math.min(blue(p_454308_) + blue(p_451175_), 255)
      );
   }

   public static int subtractRgb(int p_456227_, int p_458650_) {
      return color(
         alpha(p_456227_),
         Math.max(red(p_456227_) - red(p_458650_), 0),
         Math.max(green(p_456227_) - green(p_458650_), 0),
         Math.max(blue(p_456227_) - blue(p_458650_), 0)
      );
   }

   public static int multiplyAlpha(int p_457126_, float p_450634_) {
      if (p_457126_ != 0 && !(p_450634_ <= 0.0F)) {
         return p_450634_ >= 1.0F ? p_457126_ : color(alphaFloat(p_457126_) * p_450634_, p_457126_);
      } else {
         return 0;
      }
   }

   public static int scaleRGB(int p_364590_, float p_365829_) {
      return scaleRGB(p_364590_, p_365829_, p_365829_, p_365829_);
   }

   public static int scaleRGB(int p_368386_, float p_366859_, float p_367328_, float p_364459_) {
      return color(
         alpha(p_368386_),
         Math.clamp((int)(red(p_368386_) * p_366859_), 0, 255),
         Math.clamp((int)(green(p_368386_) * p_367328_), 0, 255),
         Math.clamp((int)(blue(p_368386_) * p_364459_), 0, 255)
      );
   }

   public static int scaleRGB(int p_366038_, int p_368003_) {
      return color(
         alpha(p_366038_),
         Math.clamp((long)red(p_366038_) * p_368003_ / 255L, 0, 255),
         Math.clamp((long)green(p_366038_) * p_368003_ / 255L, 0, 255),
         Math.clamp((long)blue(p_366038_) * p_368003_ / 255L, 0, 255)
      );
   }

   public static int greyscale(int p_362330_) {
      int i = (int)(red(p_362330_) * 0.3F + green(p_362330_) * 0.59F + blue(p_362330_) * 0.11F);
      return color(alpha(p_362330_), i, i, i);
   }

   public static int alphaBlend(int p_458472_, int p_453141_) {
      int i = alpha(p_458472_);
      int j = alpha(p_453141_);
      if (j == 255) {
         return p_453141_;
      }

      if (j == 0) {
         return p_458472_;
      }

      int k = j + i * (255 - j) / 255;
      return color(
         k,
         alphaBlendChannel(k, j, red(p_458472_), red(p_453141_)),
         alphaBlendChannel(k, j, green(p_458472_), green(p_453141_)),
         alphaBlendChannel(k, j, blue(p_458472_), blue(p_453141_))
      );
   }

   private static int alphaBlendChannel(int p_456456_, int p_451527_, int p_453924_, int p_452166_) {
      return (p_452166_ * p_451527_ + p_453924_ * (p_456456_ - p_451527_)) / p_456456_;
   }

   public static int srgbLerp(float p_368280_, int p_363975_, int p_368594_) {
      int i = Mth.lerpInt(p_368280_, alpha(p_363975_), alpha(p_368594_));
      int j = Mth.lerpInt(p_368280_, red(p_363975_), red(p_368594_));
      int k = Mth.lerpInt(p_368280_, green(p_363975_), green(p_368594_));
      int l = Mth.lerpInt(p_368280_, blue(p_363975_), blue(p_368594_));
      return color(i, j, k, l);
   }

   public static int linearLerp(float p_455266_, int p_458164_, int p_457608_) {
      return color(
         Mth.lerpInt(p_455266_, alpha(p_458164_), alpha(p_457608_)),
         LINEAR_TO_SRGB[Mth.lerpInt(p_455266_, SRGB_TO_LINEAR[red(p_458164_)], SRGB_TO_LINEAR[red(p_457608_)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(p_455266_, SRGB_TO_LINEAR[green(p_458164_)], SRGB_TO_LINEAR[green(p_457608_)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(p_455266_, SRGB_TO_LINEAR[blue(p_458164_)], SRGB_TO_LINEAR[blue(p_457608_)])] & 0xFF
      );
   }

   public static int opaque(int p_363480_) {
      return p_363480_ | 0xFF000000;
   }

   public static int transparent(int p_366691_) {
      return p_366691_ & 16777215;
   }

   public static int color(int p_362407_, int p_368043_) {
      return p_362407_ << 24 | p_368043_ & 16777215;
   }

   public static int color(float p_407846_, int p_406600_) {
      return as8BitChannel(p_407846_) << 24 | p_406600_ & 16777215;
   }

   public static int white(float p_361606_) {
      return as8BitChannel(p_361606_) << 24 | 16777215;
   }

   public static int white(int p_455446_) {
      return p_455446_ << 24 | 16777215;
   }

   public static int black(float p_455781_) {
      return as8BitChannel(p_455781_) << 24;
   }

   public static int black(int p_451585_) {
      return p_451585_ << 24;
   }

   public static int colorFromFloat(float p_365014_, float p_365331_, float p_361446_, float p_367224_) {
      return color(as8BitChannel(p_365014_), as8BitChannel(p_365331_), as8BitChannel(p_361446_), as8BitChannel(p_367224_));
   }

   public static Vector3f vector3fFromRGB24(int p_368966_) {
      return new Vector3f(redFloat(p_368966_), greenFloat(p_368966_), blueFloat(p_368966_));
   }

   public static Vector4f vector4fFromARGB32(int p_460771_) {
      return new Vector4f(redFloat(p_460771_), greenFloat(p_460771_), blueFloat(p_460771_), alphaFloat(p_460771_));
   }

   public static int average(int p_368446_, int p_366831_) {
      return color(
         (alpha(p_368446_) + alpha(p_366831_)) / 2,
         (red(p_368446_) + red(p_366831_)) / 2,
         (green(p_368446_) + green(p_366831_)) / 2,
         (blue(p_368446_) + blue(p_366831_)) / 2
      );
   }

   public static int as8BitChannel(float p_367233_) {
      return Mth.floor(p_367233_ * 255.0F);
   }

   public static float alphaFloat(int p_376586_) {
      return from8BitChannel(alpha(p_376586_));
   }

   public static float redFloat(int p_375781_) {
      return from8BitChannel(red(p_375781_));
   }

   public static float greenFloat(int p_375888_) {
      return from8BitChannel(green(p_375888_));
   }

   public static float blueFloat(int p_377428_) {
      return from8BitChannel(blue(p_377428_));
   }

   private static float from8BitChannel(int p_370155_) {
      return p_370155_ / 255.0F;
   }

   public static int toABGR(int p_368147_) {
      return p_368147_ & -16711936 | (p_368147_ & 0xFF0000) >> 16 | (p_368147_ & 0xFF) << 16;
   }

   public static int fromABGR(int p_369336_) {
      return toABGR(p_369336_);
   }

   public static int setBrightness(int p_409846_, float p_408870_) {
      int i = red(p_409846_);
      int j = green(p_409846_);
      int k = blue(p_409846_);
      int l = alpha(p_409846_);
      int i1 = Math.max(Math.max(i, j), k);
      int j1 = Math.min(Math.min(i, j), k);
      float f = i1 - j1;
      float f1;
      if (i1 != 0) {
         f1 = f / i1;
      } else {
         f1 = 0.0F;
      }

      float f2;
      if (f1 == 0.0F) {
         f2 = 0.0F;
      } else {
         float f3 = (i1 - i) / f;
         float f4 = (i1 - j) / f;
         float f5 = (i1 - k) / f;
         if (i == i1) {
            f2 = f5 - f4;
         } else if (j == i1) {
            f2 = 2.0F + f3 - f5;
         } else {
            f2 = 4.0F + f4 - f3;
         }

         f2 /= 6.0F;
         if (f2 < 0.0F) {
            f2++;
         }
      }

      if (f1 == 0.0F) {
         i = j = k = Math.round(p_408870_ * 255.0F);
         return color(l, i, j, k);
      }

      float f8 = (f2 - (float)Math.floor(f2)) * 6.0F;
      float f9 = f8 - (float)Math.floor(f8);
      float f10 = p_408870_ * (1.0F - f1);
      float f6 = p_408870_ * (1.0F - f1 * f9);
      float f7 = p_408870_ * (1.0F - f1 * (1.0F - f9));
      switch ((int)f8) {
         case 0:
            i = Math.round(p_408870_ * 255.0F);
            j = Math.round(f7 * 255.0F);
            k = Math.round(f10 * 255.0F);
            break;
         case 1:
            i = Math.round(f6 * 255.0F);
            j = Math.round(p_408870_ * 255.0F);
            k = Math.round(f10 * 255.0F);
            break;
         case 2:
            i = Math.round(f10 * 255.0F);
            j = Math.round(p_408870_ * 255.0F);
            k = Math.round(f7 * 255.0F);
            break;
         case 3:
            i = Math.round(f10 * 255.0F);
            j = Math.round(f6 * 255.0F);
            k = Math.round(p_408870_ * 255.0F);
            break;
         case 4:
            i = Math.round(f7 * 255.0F);
            j = Math.round(f10 * 255.0F);
            k = Math.round(p_408870_ * 255.0F);
            break;
         case 5:
            i = Math.round(p_408870_ * 255.0F);
            j = Math.round(f10 * 255.0F);
            k = Math.round(f6 * 255.0F);
      }

      return color(l, i, j, k);
   }
}
