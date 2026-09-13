package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

public class GlowParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   private GlowParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final SpriteSet sprites
   ) {
      super(level, x, y, z, xa, ya, za, sprites.first());
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = sprites;
      this.quadSize *= 0.75F;
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), (this.age + a) / this.lifetime);
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.25;
      private final SpriteSet sprite;

      public ElectricSparkProvider(final SpriteSet sprite) {
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
         GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setParticleSpeed(xAux * 0.25, yAux * 0.25, zAux * 0.25);
         int minLifespan = 2;
         int maxLifespan = 4;
         glowParticle.setLifetime(random.nextInt(2) + 2);
         return glowParticle;
      }
   }

   public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public GlowSquidProvider(final SpriteSet sprite) {
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
         GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.5 - random.nextDouble(), yAux, 0.5 - random.nextDouble(), this.sprite);
         if (random.nextBoolean()) {
            glowParticle.setColor(0.6F, 1.0F, 0.8F);
         } else {
            glowParticle.setColor(0.08F, 0.4F, 0.4F);
         }

         glowParticle.yd *= 0.2F;
         if (xAux == 0.0 && zAux == 0.0) {
            glowParticle.xd *= 0.1F;
            glowParticle.zd *= 0.1F;
         }

         glowParticle.setLifetime((int)(8.0 / (random.nextDouble() * 0.8 + 0.2)));
         return glowParticle;
      }
   }

   public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public ScrapeProvider(final SpriteSet sprite) {
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
         GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
         if (random.nextBoolean()) {
            glowParticle.setColor(0.29F, 0.58F, 0.51F);
         } else {
            glowParticle.setColor(0.43F, 0.77F, 0.62F);
         }

         glowParticle.setParticleSpeed(xAux * 0.01, yAux * 0.01, zAux * 0.01);
         int minLifespan = 10;
         int maxLifespan = 40;
         glowParticle.setLifetime(random.nextInt(30) + 10);
         return glowParticle;
      }
   }

   public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOffProvider(final SpriteSet sprite) {
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
         GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setParticleSpeed(xAux * 0.01 / 2.0, yAux * 0.01, zAux * 0.01 / 2.0);
         int minLifespan = 10;
         int maxLifespan = 40;
         glowParticle.setLifetime(random.nextInt(30) + 10);
         return glowParticle;
      }
   }

   public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOnProvider(final SpriteSet sprite) {
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
         GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
         glowParticle.setColor(0.91F, 0.55F, 0.08F);
         glowParticle.setParticleSpeed(xAux * 0.01 / 2.0, yAux * 0.01, zAux * 0.01 / 2.0);
         int minLifespan = 10;
         int maxLifespan = 40;
         glowParticle.setLifetime(random.nextInt(30) + 10);
         return glowParticle;
      }
   }
}
