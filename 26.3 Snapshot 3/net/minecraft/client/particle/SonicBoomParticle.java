package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SonicBoomParticle extends HugeExplosionParticle {
   protected SonicBoomParticle(final ClientLevel level, final double x, final double y, final double z, final double size, final SpriteSet sprites) {
      super(level, x, y, z, size, sprites);
      this.lifetime = 16;
      this.quadSize = 1.5F;
      this.setSpriteFromAge(sprites);
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
         return new SonicBoomParticle(level, x, y, z, xAux, this.sprites);
      }
   }
}
