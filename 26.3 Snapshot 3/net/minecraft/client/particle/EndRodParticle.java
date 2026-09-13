package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class EndRodParticle extends SimpleAnimatedParticle {
   private EndRodParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final SpriteSet sprites
   ) {
      super(level, x, y, z, sprites, 0.0125F);
      this.xd = xa;
      this.yd = ya;
      this.zd = za;
      this.quadSize *= 0.75F;
      this.lifetime = 60 + this.random.nextInt(12);
      this.setFadeColor(15916745);
      this.setSpriteFromAge(sprites);
   }

   @Override
   public void move(final double xa, final double ya, final double za) {
      this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
      this.setLocationFromBoundingbox();
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
         return new EndRodParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
      }
   }
}
