package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerDetectionParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private static final int BASE_LIFETIME = 8;

   protected TrialSpawnerDetectionParticle(
      ClientLevel p_310929_,
      double p_311438_,
      double p_312516_,
      double p_312471_,
      double p_311930_,
      double p_310570_,
      double p_311049_,
      float p_311264_,
      SpriteSet p_313038_
   ) {
      super(p_310929_, p_311438_, p_312516_, p_312471_, 0.0, 0.0, 0.0, p_313038_.first());
      this.sprites = p_313038_;
      this.friction = 0.96F;
      this.gravity = -0.1F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.xd *= 0.0;
      this.yd *= 0.9;
      this.zd *= 0.0;
      this.xd += p_311930_;
      this.yd += p_310570_;
      this.zd += p_311049_;
      this.quadSize *= 0.75F * p_311264_;
      this.lifetime = (int)(8.0F / Mth.randomBetween(this.random, 0.5F, 1.0F) * p_311264_);
      this.lifetime = Math.max(this.lifetime, 1);
      this.setSpriteFromAge(p_313038_);
      this.hasPhysics = true;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightColor(float p_312792_) {
      return 240;
   }

   @Override
   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return SingleQuadParticle.FacingCameraMode.LOOKAT_Y;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @Override
   public float getQuadSize(float p_313241_) {
      return this.quadSize * Mth.clamp((this.age + p_313241_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_311649_) {
         this.sprites = p_311649_;
      }

      public Particle createParticle(
         SimpleParticleType p_312519_,
         ClientLevel p_310081_,
         double p_312198_,
         double p_312389_,
         double p_310385_,
         double p_312116_,
         double p_312285_,
         double p_312651_,
         RandomSource p_430992_
      ) {
         return new TrialSpawnerDetectionParticle(p_310081_, p_312198_, p_312389_, p_310385_, p_312116_, p_312285_, p_312651_, 1.5F, this.sprites);
      }
   }
}
