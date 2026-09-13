package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FallingLeavesParticle extends FallingParticle {
   protected FallingLeavesParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final TextureAtlasSprite sprite,
      final float fallAcceleration,
      final float sideAcceleration,
      final boolean swirl,
      final boolean flowAway,
      final float scale,
      final float startVelocity
   ) {
      super(level, x, y, z, sprite, fallAcceleration, sideAcceleration, swirl, flowAway, scale, startVelocity);
   }

   public static class CherryProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public CherryProvider(final SpriteSet sprites) {
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
         return new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.25F, 2.0F, false, true, 1.0F, 0.0F);
      }
   }

   public static class PaleOakProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public PaleOakProvider(final SpriteSet sprites) {
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
         return new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.07F, 10.0F, true, false, 2.0F, 0.021F);
      }
   }

   public static class PoplarProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public PoplarProvider(final SpriteSet sprites) {
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
         return new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.07F, 10.0F, true, false, 2.0F, 0.021F);
      }
   }

   public static class TintedLeavesProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprites;

      public TintedLeavesProvider(final SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         final ColorParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         FallingLeavesParticle particle = new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.07F, 10.0F, true, false, 2.0F, 0.021F);
         particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
         return particle;
      }
   }
}
