package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public abstract class ScalableParticleOptionsBase implements ParticleOptions {
   public static final float MIN_SCALE = 0.01F;
   public static final float MAX_SCALE = 4.0F;
   protected static final Codec<Float> SCALE = Codec.FLOAT
      .validate(
         p_334592_ -> p_334592_ >= 0.01F && p_334592_ <= 4.0F
            ? DataResult.success(p_334592_)
            : DataResult.error(() -> "Value must be within range [0.01;4.0]: " + p_334592_)
      );
   private final float scale;

   public ScalableParticleOptionsBase(float p_334616_) {
      this.scale = Mth.clamp(p_334616_, 0.01F, 4.0F);
   }

   public float getScale() {
      return this.scale;
   }
}
