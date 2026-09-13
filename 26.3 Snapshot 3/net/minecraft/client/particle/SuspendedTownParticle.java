package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SuspendedTownParticle extends SingleQuadParticle {
   private SuspendedTownParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, xa, ya, za, sprite);
      float br = this.random.nextFloat() * 0.1F + 0.2F;
      this.rCol = br;
      this.gCol = br;
      this.bCol = br;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
      this.xd *= 0.02F;
      this.yd *= 0.02F;
      this.zd *= 0.02F;
      this.lifetime = (int)(20.0 / (this.random.nextFloat() * 0.8 + 0.2));
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
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.99;
         this.yd *= 0.99;
         this.zd *= 0.99;
      }
   }

   public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ComposterFillProvider(final SpriteSet sprite) {
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
         SuspendedTownParticle particle = new SuspendedTownParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
         particle.setColor(1.0F, 1.0F, 1.0F);
         particle.setLifetime(3 + level.getRandom().nextInt(5));
         return particle;
      }
   }

   public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DolphinSpeedProvider(final SpriteSet sprite) {
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
         SuspendedTownParticle particle = new SuspendedTownParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
         particle.setColor(0.3F, 0.5F, 1.0F);
         particle.setAlpha(1.0F - random.nextFloat() * 0.7F);
         particle.setLifetime(particle.getLifetime() / 2);
         return particle;
      }
   }

   public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EggCrackProvider(final SpriteSet sprite) {
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
         SuspendedTownParticle particle = new SuspendedTownParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
         particle.setColor(1.0F, 1.0F, 1.0F);
         return particle;
      }
   }

   public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HappyVillagerProvider(final SpriteSet sprite) {
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
         SuspendedTownParticle particle = new SuspendedTownParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
         particle.setColor(1.0F, 1.0F, 1.0F);
         return particle;
      }
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
         return new SuspendedTownParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
      }
   }
}
