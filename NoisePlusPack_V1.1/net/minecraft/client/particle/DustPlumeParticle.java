package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustPlumeParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected DustPlumeParticle(
      ClientLevel p_310558_,
      double p_313232_,
      double p_311124_,
      double p_309990_,
      double p_312124_,
      double p_313045_,
      double p_310834_,
      float p_312915_,
      SpriteSet p_312671_
   ) {
      super(p_310558_, p_313232_, p_311124_, p_309990_, 0.7F, 0.6F, 0.7F, p_312124_, p_313045_ + 0.15F, p_310834_, p_312915_, p_312671_, 0.5F, 7, 0.5F, false);
      float f = this.random.nextFloat() * 0.2F;
      this.rCol = ARGB.red(12235202) / 255.0F - f;
      this.gCol = ARGB.green(12235202) / 255.0F - f;
      this.bCol = ARGB.blue(12235202) / 255.0F - f;
   }

   @Override
   public void tick() {
      this.gravity = 0.88F * this.gravity;
      this.friction = 0.92F * this.friction;
      super.tick();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_310852_) {
         this.sprites = p_310852_;
      }

      public Particle createParticle(
         SimpleParticleType p_309734_,
         ClientLevel p_310371_,
         double p_310904_,
         double p_310946_,
         double p_312810_,
         double p_309747_,
         double p_311225_,
         double p_310480_,
         RandomSource p_425301_
      ) {
         return new DustPlumeParticle(p_310371_, p_310904_, p_310946_, p_312810_, p_309747_, p_311225_, p_310480_, 1.0F, this.sprites);
      }
   }
}
