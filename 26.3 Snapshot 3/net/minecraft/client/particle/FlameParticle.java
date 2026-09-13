package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

public class FlameParticle extends RisingParticle {
   private FlameParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, xd, yd, zd, sprite);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void move(final double xa, final double ya, final double za) {
      this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
      this.setLocationFromBoundingbox();
   }

   @Override
   public float getQuadSize(final float a) {
      float s = (this.age + a) / this.lifetime;
      return this.quadSize * (1.0F - s * s * 0.5F);
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), (this.age + a) / this.lifetime);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(final SpriteSet sprite) {
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
         return new FlameParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
      }
   }

   public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SmallFlameProvider(final SpriteSet sprite) {
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
         FlameParticle particle = new FlameParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
         particle.scale(0.5F);
         return particle;
      }
   }
}
