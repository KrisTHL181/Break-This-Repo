package net.minecraft.client.model.geom.builders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record UVPair(float u, float v) {
   @Override
   public String toString() {
      return "(" + this.u + "," + this.v + ")";
   }

   public static long pack(float p_460966_, float p_455642_) {
      long i = Float.floatToIntBits(p_460966_) & 4294967295L;
      long j = Float.floatToIntBits(p_455642_) & 4294967295L;
      return i << 32 | j;
   }

   public static float unpackU(long p_459196_) {
      int i = (int)(p_459196_ >> 32);
      return Float.intBitsToFloat(i);
   }

   public static float unpackV(long p_459018_) {
      return Float.intBitsToFloat((int)p_459018_);
   }
}
