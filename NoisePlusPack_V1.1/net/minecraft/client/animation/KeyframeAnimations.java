package net.minecraft.client.animation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimations {
   public static Vector3f posVec(float p_253691_, float p_254046_, float p_254461_) {
      return new Vector3f(p_253691_, -p_254046_, p_254461_);
   }

   public static Vector3f degreeVec(float p_254402_, float p_253917_, float p_254397_) {
      return new Vector3f(p_254402_ * (float) (Math.PI / 180.0), p_253917_ * (float) (Math.PI / 180.0), p_254397_ * (float) (Math.PI / 180.0));
   }

   public static Vector3f scaleVec(double p_253806_, double p_253647_, double p_254396_) {
      return new Vector3f((float)(p_253806_ - 1.0), (float)(p_253647_ - 1.0), (float)(p_254396_ - 1.0));
   }
}
