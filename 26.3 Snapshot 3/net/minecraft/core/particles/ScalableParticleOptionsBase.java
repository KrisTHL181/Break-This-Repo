package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public abstract class ScalableParticleOptionsBase implements ParticleOptions {
   public static final float MIN_SCALE = 0.01F;
   public static final float MAX_SCALE = 4.0F;
   protected static final Codec<Float> SCALE = Codec.FLOAT
      .validate(v -> v >= 0.01F && v <= 4.0F ? DataResult.success(v) : DataResult.error(() -> "Value must be within range [0.01;4.0]: " + v));
   private final float scale;

   public ScalableParticleOptionsBase(final float scale) {
      this.scale = Mth.clamp(scale, 0.01F, 4.0F);
   }

   public float getScale() {
      return this.scale;
   }
}
