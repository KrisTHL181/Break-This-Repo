package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class CampfireSmokeParticle extends SingleQuadParticle {
   private CampfireSmokeParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final boolean isSignalFire,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, sprite);
      this.scale(3.0F);
      this.setSize(0.25F, 0.25F);
      if (isSignalFire) {
         this.lifetime = this.random.nextInt(50) + 280;
      } else {
         this.lifetime = this.random.nextInt(50) + 80;
      }

      this.gravity = 3.0E-6F;
      this.xd = xa;
      this.yd = ya + this.random.nextFloat() / 500.0F;
      this.zd = za;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
         this.xd = this.xd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
         this.zd = this.zd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
            this.alpha -= 0.015F;
         }
      } else {
         this.remove();
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   public static class CosyProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public CosyProvider(final SpriteSet sprites) {
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
         CampfireSmokeParticle particle = new CampfireSmokeParticle(level, x, y, z, xAux, yAux, zAux, false, this.sprites.get(random));
         particle.setAlpha(0.9F);
         return particle;
      }
   }

   public static class SignalProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SignalProvider(final SpriteSet sprites) {
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
         CampfireSmokeParticle particle = new CampfireSmokeParticle(level, x, y, z, xAux, yAux, zAux, true, this.sprites.get(random));
         particle.setAlpha(0.95F);
         return particle;
      }
   }
}
