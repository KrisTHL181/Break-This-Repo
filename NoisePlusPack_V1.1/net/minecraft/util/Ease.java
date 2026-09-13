package net.minecraft.util;

public class Ease {
   public static float inBack(float p_461003_) {
      float f = 1.70158F;
      float f1 = 2.70158F;
      return Mth.square(p_461003_) * (2.70158F * p_461003_ - 1.70158F);
   }

   public static float inBounce(float p_452498_) {
      return 1.0F - outBounce(1.0F - p_452498_);
   }

   public static float inCubic(float p_450203_) {
      return Mth.cube(p_450203_);
   }

   public static float inElastic(float p_451404_) {
      if (p_451404_ == 0.0F) {
         return 0.0F;
      }

      if (p_451404_ == 1.0F) {
         return 1.0F;
      }

      float f = (float) (Math.PI * 2.0 / 3.0);
      return (float)(-Math.pow(2.0, 10.0 * p_451404_ - 10.0) * Math.sin((p_451404_ * 10.0 - 10.75) * (float) (Math.PI * 2.0 / 3.0)));
   }

   public static float inExpo(float p_460064_) {
      return p_460064_ == 0.0F ? 0.0F : (float)Math.pow(2.0, 10.0 * p_460064_ - 10.0);
   }

   public static float inQuart(float p_450516_) {
      return Mth.square(Mth.square(p_450516_));
   }

   public static float inQuint(float p_450499_) {
      return Mth.square(Mth.square(p_450499_)) * p_450499_;
   }

   public static float inSine(float p_458675_) {
      return 1.0F - Mth.cos(p_458675_ * (float) (Math.PI / 2));
   }

   public static float inOutBounce(float p_452207_) {
      return p_452207_ < 0.5F ? (1.0F - outBounce(1.0F - 2.0F * p_452207_)) / 2.0F : (1.0F + outBounce(2.0F * p_452207_ - 1.0F)) / 2.0F;
   }

   public static float inOutCirc(float p_450195_) {
      return p_450195_ < 0.5F
         ? (float)((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * p_450195_, 2.0))) / 2.0)
         : (float)((Math.sqrt(1.0 - Math.pow(-2.0 * p_450195_ + 2.0, 2.0)) + 1.0) / 2.0);
   }

   public static float inOutCubic(float p_454062_) {
      return p_454062_ < 0.5F ? 4.0F * Mth.cube(p_454062_) : (float)(1.0 - Math.pow(-2.0 * p_454062_ + 2.0, 3.0) / 2.0);
   }

   public static float inOutQuad(float p_454456_) {
      return p_454456_ < 0.5F ? 2.0F * Mth.square(p_454456_) : (float)(1.0 - Math.pow(-2.0 * p_454456_ + 2.0, 2.0) / 2.0);
   }

   public static float inOutQuart(float p_455383_) {
      return p_455383_ < 0.5F ? 8.0F * Mth.square(Mth.square(p_455383_)) : (float)(1.0 - Math.pow(-2.0 * p_455383_ + 2.0, 4.0) / 2.0);
   }

   public static float inOutQuint(float p_456681_) {
      return p_456681_ < 0.5 ? 16.0F * p_456681_ * p_456681_ * p_456681_ * p_456681_ * p_456681_ : (float)(1.0 - Math.pow(-2.0 * p_456681_ + 2.0, 5.0) / 2.0);
   }

   public static float outBounce(float p_458505_) {
      float f = 7.5625F;
      float f1 = 2.75F;
      if (p_458505_ < 0.36363637F) {
         return 7.5625F * Mth.square(p_458505_);
      } else if (p_458505_ < 0.72727275F) {
         return 7.5625F * Mth.square(p_458505_ - 0.54545456F) + 0.75F;
      } else {
         return p_458505_ < 0.9090909090909091
            ? 7.5625F * Mth.square(p_458505_ - 0.8181818F) + 0.9375F
            : 7.5625F * Mth.square(p_458505_ - 0.95454544F) + 0.984375F;
      }
   }

   public static float outElastic(float p_459720_) {
      float f = (float) (Math.PI * 2.0 / 3.0);
      if (p_459720_ == 0.0F) {
         return 0.0F;
      } else {
         return p_459720_ == 1.0F
            ? 1.0F
            : (float)(Math.pow(2.0, -10.0 * p_459720_) * Math.sin((p_459720_ * 10.0 - 0.75) * (float) (Math.PI * 2.0 / 3.0)) + 1.0);
      }
   }

   public static float outExpo(float p_457208_) {
      return p_457208_ == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * p_457208_);
   }

   public static float outQuad(float p_452624_) {
      return 1.0F - Mth.square(1.0F - p_452624_);
   }

   public static float outQuint(float p_460195_) {
      return 1.0F - (float)Math.pow(1.0 - p_460195_, 5.0);
   }

   public static float outSine(float p_454952_) {
      return Mth.sin(p_454952_ * (float) (Math.PI / 2));
   }

   public static float inOutSine(float p_456309_) {
      return -(Mth.cos((float) Math.PI * p_456309_) - 1.0F) / 2.0F;
   }

   public static float outBack(float p_452574_) {
      float f = 1.70158F;
      float f1 = 2.70158F;
      return 1.0F + 2.70158F * Mth.cube(p_452574_ - 1.0F) + 1.70158F * Mth.square(p_452574_ - 1.0F);
   }

   public static float outQuart(float p_458626_) {
      return 1.0F - Mth.square(Mth.square(1.0F - p_458626_));
   }

   public static float outCubic(float p_460256_) {
      return 1.0F - Mth.cube(1.0F - p_460256_);
   }

   public static float inOutExpo(float p_450945_) {
      if (p_450945_ < 0.5F) {
         return p_450945_ == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0 * p_450945_ - 10.0) / 2.0);
      } else {
         return p_450945_ == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0 * p_450945_ + 10.0)) / 2.0);
      }
   }

   public static float inQuad(float p_451318_) {
      return p_451318_ * p_451318_;
   }

   public static float outCirc(float p_450332_) {
      return (float)Math.sqrt(1.0F - Mth.square(p_450332_ - 1.0F));
   }

   public static float inOutElastic(float p_454101_) {
      float f = (float) Math.PI * 4.0F / 9.0F;
      if (p_454101_ == 0.0F) {
         return 0.0F;
      }

      if (p_454101_ == 1.0F) {
         return 1.0F;
      }

      double d0 = Math.sin((20.0 * p_454101_ - 11.125) * (float) Math.PI * 4.0 / 9.0);
      return p_454101_ < 0.5F
         ? (float)(-(Math.pow(2.0, 20.0 * p_454101_ - 10.0) * d0) / 2.0)
         : (float)(Math.pow(2.0, -20.0 * p_454101_ + 10.0) * d0 / 2.0 + 1.0);
   }

   public static float inCirc(float p_454893_) {
      return (float)(-Math.sqrt(1.0F - p_454893_ * p_454893_)) + 1.0F;
   }

   public static float inOutBack(float p_456978_) {
      float f = 1.70158F;
      float f1 = 2.5949094F;
      if (p_456978_ < 0.5F) {
         return 4.0F * p_456978_ * p_456978_ * (7.189819F * p_456978_ - 2.5949094F) / 2.0F;
      }

      float f2 = 2.0F * p_456978_ - 2.0F;
      return (f2 * f2 * (3.5949094F * f2 + 2.5949094F) + 2.0F) / 2.0F;
   }
}
