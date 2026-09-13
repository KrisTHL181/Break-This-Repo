package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class TrailParticle extends SingleQuadParticle {
   private final Vec3 target;

   private TrailParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xAux,
      final double yAux,
      final double zAux,
      final Vec3 target,
      int color,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, xAux, yAux, zAux, sprite);
      color = ARGB.scaleRGB(color, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F);
      this.rCol = ARGB.red(color) / 255.0F;
      this.gCol = ARGB.green(color) / 255.0F;
      this.bCol = ARGB.blue(color) / 255.0F;
      this.quadSize = 0.26F;
      this.target = target;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         int ticksRemaining = this.lifetime - this.age;
         double alpha = 1.0 / ticksRemaining;
         this.x = Mth.lerp(alpha, this.x, this.target.x());
         this.y = Mth.lerp(alpha, this.y, this.target.y());
         this.z = Mth.lerp(alpha, this.z, this.target.z());
      }
   }

   @Override
   public int getLightCoords(final float a) {
      return 15728880;
   }

   public static class Provider implements ParticleProvider<TrailParticleOption> {
      private final SpriteSet sprite;

      public Provider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         final TrailParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         TrailParticle particle = new TrailParticle(level, x, y, z, xAux, yAux, zAux, options.target(), options.color(), this.sprite.get(random));
         particle.setLifetime(options.duration());
         return particle;
      }
   }
}
