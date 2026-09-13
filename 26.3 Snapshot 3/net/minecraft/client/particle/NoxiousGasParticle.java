package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class NoxiousGasParticle extends BaseAshSmokeParticle {
   private final float fadeOutStartingPoint;

   protected NoxiousGasParticle(
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
      super(level, x, y, z, 0.1F, 0.1F, 0.1F, xa, ya, za, scale, sprites, 0.3F, 5, -0.02F, true);
      this.rCol = 1.0F;
      this.gCol = 1.0F;
      this.bCol = 1.0F;
      this.lifetime = (int)(6.0 / (this.random.nextFloat() * 0.5 + 0.5) * scale);
      this.fadeOutStartingPoint = this.lifetime / 2.0F;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.age > this.fadeOutStartingPoint) {
         float framesSinceFadeOutStart = this.age - this.fadeOutStartingPoint;
         this.setAlpha((this.lifetime - framesSinceFadeOutStart) / this.lifetime);
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
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
         return new NoxiousGasParticle(level, x, y, z, xAux, yAux, zAux, 3.0F, this.sprites);
      }
   }
}
