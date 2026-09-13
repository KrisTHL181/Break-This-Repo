package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteSmokeParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteSmokeParticle(
      ClientLevel p_310197_,
      double p_309917_,
      double p_311482_,
      double p_311581_,
      double p_311949_,
      double p_312227_,
      double p_312663_,
      float p_313081_,
      SpriteSet p_311457_
   ) {
      super(p_310197_, p_309917_, p_311482_, p_311581_, 0.1F, 0.1F, 0.1F, p_311949_, p_312227_, p_312663_, p_313081_, p_311457_, 0.3F, 8, -0.1F, true);
      this.rCol = 0.7294118F;
      this.gCol = 0.69411767F;
      this.bCol = 0.7607843F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_309673_) {
         this.sprites = p_309673_;
      }

      public Particle createParticle(
         SimpleParticleType p_311074_,
         ClientLevel p_309967_,
         double p_312945_,
         double p_312815_,
         double p_311211_,
         double p_311415_,
         double p_310534_,
         double p_312986_,
         RandomSource p_423161_
      ) {
         return new WhiteSmokeParticle(p_309967_, p_312945_, p_312815_, p_311211_, p_311415_, p_310534_, p_312986_, 1.0F, this.sprites);
      }
   }
}
