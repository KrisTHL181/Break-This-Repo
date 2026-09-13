package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class GustParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected GustParticle(final ClientLevel level, final double x, final double y, final double z, final SpriteSet sprites) {
      super(level, x, y, z, sprites.first());
      this.sprites = sprites;
      this.setSpriteFromAge(sprites);
      this.lifetime = 12 + this.random.nextInt(4);
      this.quadSize = 1.0F;
      this.setSize(1.0F, 1.0F);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightCoords(final float a) {
      return 15728880;
   }

   @Override
   public void tick() {
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
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
         return new GustParticle(level, x, y, z, this.sprites);
      }
   }

   public static class SmallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SmallProvider(final SpriteSet sprites) {
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
         Particle particle = new GustParticle(level, x, y, z, this.sprites);
         particle.scale(0.15F);
         return particle;
      }
   }
}
