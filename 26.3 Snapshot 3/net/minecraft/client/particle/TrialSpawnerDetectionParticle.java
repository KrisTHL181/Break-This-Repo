package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class TrialSpawnerDetectionParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private static final int BASE_LIFETIME = 8;

   protected TrialSpawnerDetectionParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final float scale,
      final SpriteSet sprites
   ) {
      super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
      this.sprites = sprites;
      this.friction = 0.96F;
      this.gravity = -0.1F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.xd *= 0.0;
      this.yd *= 0.9;
      this.zd *= 0.0;
      this.xd += xa;
      this.yd += ya;
      this.zd += za;
      this.quadSize *= 0.75F * scale;
      this.lifetime = (int)(8.0F / Mth.randomBetween(this.random, 0.5F, 1.0F) * scale);
      this.lifetime = Math.max(this.lifetime, 1);
      this.setSpriteFromAge(sprites);
      this.hasPhysics = true;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
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
   public float getQuadSize(final float a) {
      return this.quadSize * Mth.clamp((this.age + a) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(final SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         final SimpleParticleType options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         return new TrialSpawnerDetectionParticle(level, x, y, z, xAux, yAux, zAux, 1.5F, this.sprites);
      }
   }
}
