package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SuspendedParticle extends SingleQuadParticle {
   private SuspendedParticle(final ClientLevel level, final double x, final double y, final double z, final TextureAtlasSprite sprite) {
      super(level, x, y - 0.125, z, sprite);
      this.setSize(0.01F, 0.01F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   private SuspendedParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y - 0.125, z, xd, yd, zd, sprite);
      this.setSize(0.01F, 0.01F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public CrimsonSporeProvider(final SpriteSet sprite) {
         this.sprite = sprite;
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
         double xa = random.nextGaussian() * 1.0E-6F;
         double ya = random.nextGaussian() * 1.0E-4F;
         double za = random.nextGaussian() * 1.0E-6F;
         SuspendedParticle particle = new SuspendedParticle(level, x, y, z, xa, ya, za, this.sprite.get(random));
         particle.setColor(0.9F, 0.4F, 0.5F);
         return particle;
      }
   }

   public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomAirProvider(final SpriteSet sprite) {
         this.sprite = sprite;
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
         SuspendedParticle particle = new SuspendedParticle(level, x, y, z, 0.0, -0.8F, 0.0, this.sprite.get(random)) {
            @Override
            public Optional<ParticleLimit> getParticleLimit() {
               return Optional.of(ParticleLimit.SPORE_BLOSSOM);
            }
         };
         particle.lifetime = Mth.randomBetweenInclusive(random, 500, 1000);
         particle.gravity = 0.01F;
         particle.setColor(0.32F, 0.5F, 0.22F);
         return particle;
      }
   }

   public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public UnderwaterProvider(final SpriteSet sprite) {
         this.sprite = sprite;
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
         SuspendedParticle particle = new SuspendedParticle(level, x, y, z, this.sprite.get(random));
         particle.setColor(0.4F, 0.4F, 0.7F);
         return particle;
      }
   }

   public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WarpedSporeProvider(final SpriteSet sprite) {
         this.sprite = sprite;
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
         double ya = random.nextFloat() * -1.9 * random.nextFloat() * 0.1;
         SuspendedParticle particle = new SuspendedParticle(level, x, y, z, 0.0, ya, 0.0, this.sprite.get(random));
         particle.setColor(0.1F, 0.1F, 0.3F);
         particle.setSize(0.001F, 0.001F);
         return particle;
      }
   }
}
