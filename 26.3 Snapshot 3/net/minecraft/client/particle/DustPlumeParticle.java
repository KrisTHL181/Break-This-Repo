package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

public class DustPlumeParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected DustPlumeParticle(
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
      super(level, x, y, z, 0.7F, 0.6F, 0.7F, xa, ya + 0.15F, za, scale, sprites, 0.5F, 7, 0.5F, false);
      float colorShift = this.random.nextFloat() * 0.2F;
      this.rCol = ARGB.red(12235202) / 255.0F - colorShift;
      this.gCol = ARGB.green(12235202) / 255.0F - colorShift;
      this.bCol = ARGB.blue(12235202) / 255.0F - colorShift;
   }

   @Override
   public void tick() {
      this.gravity = 0.88F * this.gravity;
      this.friction = 0.92F * this.friction;
      super.tick();
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
         return new DustPlumeParticle(level, x, y, z, xAux, yAux, zAux, 1.0F, this.sprites);
      }
   }
}
