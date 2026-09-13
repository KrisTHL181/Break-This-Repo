package net.minecraft.network.protocol.game;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.world.phys.Vec3;

public class VecDeltaCodec {
   private static final double TRUNCATION_STEPS = 4096.0;
   private Vec3 base = Vec3.ZERO;

   @VisibleForTesting
   static long encode(double p_238018_) {
      return Math.round(p_238018_ * 4096.0);
   }

   @VisibleForTesting
   static double decode(long p_238020_) {
      return p_238020_ / 4096.0;
   }

   public Vec3 decode(long p_238022_, long p_238023_, long p_238024_) {
      if (p_238022_ == 0L && p_238023_ == 0L && p_238024_ == 0L) {
         return this.base;
      }

      double d0 = p_238022_ == 0L ? this.base.x : decode(encode(this.base.x) + p_238022_);
      double d1 = p_238023_ == 0L ? this.base.y : decode(encode(this.base.y) + p_238023_);
      double d2 = p_238024_ == 0L ? this.base.z : decode(encode(this.base.z) + p_238024_);
      return new Vec3(d0, d1, d2);
   }

   public long encodeX(Vec3 p_238026_) {
      return encode(p_238026_.x) - encode(this.base.x);
   }

   public long encodeY(Vec3 p_238028_) {
      return encode(p_238028_.y) - encode(this.base.y);
   }

   public long encodeZ(Vec3 p_238030_) {
      return encode(p_238030_.z) - encode(this.base.z);
   }

   public Vec3 delta(Vec3 p_238032_) {
      return p_238032_.subtract(this.base);
   }

   public void setBase(Vec3 p_238034_) {
      this.base = p_238034_;
   }

   public Vec3 getBase() {
      return this.base;
   }
}
