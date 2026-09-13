package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class WakeParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   private WakeParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final SpriteSet sprites
   ) {
      super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
      this.sprites = sprites;
      this.xd *= 0.3F;
      this.yd = this.random.nextFloat() * 0.2F + 0.1F;
      this.zd *= 0.3F;
      this.setSize(0.01F, 0.01F);
      this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.setSpriteFromAge(sprites);
      this.gravity = 0.0F;
      this.xd = xa;
      this.yd = ya;
      this.zd = za;
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
      int life = 60 - this.lifetime;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.98F;
         this.yd *= 0.98F;
         this.zd *= 0.98F;
         float size = life * 0.001F;
         this.setSize(size, size);
         this.setSprite(this.sprites.get(life % 4, 4));
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
         return new WakeParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
      }
   }
}
