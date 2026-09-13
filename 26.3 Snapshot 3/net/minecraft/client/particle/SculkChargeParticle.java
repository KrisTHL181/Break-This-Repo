package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

public class SculkChargeParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   private SculkChargeParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xd, final double yd, final double zd, final SpriteSet sprites
   ) {
      super(level, x, y, z, xd, yd, zd, sprites.first());
      this.friction = 0.96F;
      this.sprites = sprites;
      this.scale(1.5F);
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   public record Provider(SpriteSet sprite) implements ParticleProvider<SculkChargeParticleOptions> {
      public Particle createParticle(
         final SculkChargeParticleOptions options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         SculkChargeParticle particle = new SculkChargeParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
         particle.setAlpha(1.0F);
         particle.setParticleSpeed(xAux, yAux, zAux);
         particle.oRoll = options.roll();
         particle.roll = options.roll();
         particle.setLifetime(random.nextInt(12) + 8);
         return particle;
      }
   }
}
