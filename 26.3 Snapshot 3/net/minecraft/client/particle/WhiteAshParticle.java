package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(
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
      super(level, x, y, z, 0.1F, -0.1F, 0.1F, xa, ya, za, scale, sprites, 0.0F, 20, 0.0125F, false);
      this.rCol = ARGB.red(12235202) / 255.0F;
      this.gCol = ARGB.green(12235202) / 255.0F;
      this.bCol = ARGB.blue(12235202) / 255.0F;
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
         double xa = random.nextFloat() * -1.9 * random.nextFloat() * 0.1;
         double ya = random.nextFloat() * -0.5 * random.nextFloat() * 0.1 * 5.0;
         double za = random.nextFloat() * -1.9 * random.nextFloat() * 0.1;
         return new WhiteAshParticle(level, x, y, z, xa, ya, za, 1.0F, this.sprites);
      }
   }
}
