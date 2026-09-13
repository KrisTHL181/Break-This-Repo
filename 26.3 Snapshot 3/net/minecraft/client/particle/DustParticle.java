package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class DustParticle extends DustParticleBase<DustParticleOptions> {
   protected DustParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xAux,
      final double yAux,
      final double zAux,
      final DustParticleOptions options,
      final SpriteSet sprites
   ) {
      super(level, x, y, z, xAux, yAux, zAux, options, sprites);
      float baseFactor = this.random.nextFloat() * 0.4F + 0.6F;
      Vector3f color = options.getColor();
      this.rCol = this.randomizeColor(color.x(), baseFactor);
      this.gCol = this.randomizeColor(color.y(), baseFactor);
      this.bCol = this.randomizeColor(color.z(), baseFactor);
   }

   public static class Provider implements ParticleProvider<DustParticleOptions> {
      private final SpriteSet sprites;

      public Provider(final SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         final DustParticleOptions options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         return new DustParticle(level, x, y, z, xAux, yAux, zAux, options, this.sprites);
      }
   }
}
