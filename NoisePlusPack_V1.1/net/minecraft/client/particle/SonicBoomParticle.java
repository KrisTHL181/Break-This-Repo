package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SonicBoomParticle extends HugeExplosionParticle {
   protected SonicBoomParticle(ClientLevel p_234028_, double p_234029_, double p_234030_, double p_234031_, double p_234032_, SpriteSet p_234033_) {
      super(p_234028_, p_234029_, p_234030_, p_234031_, p_234032_, p_234033_);
      this.lifetime = 16;
      this.quadSize = 1.5F;
      this.setSpriteFromAge(p_234033_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_234036_) {
         this.sprites = p_234036_;
      }

      public Particle createParticle(
         SimpleParticleType p_234047_,
         ClientLevel p_234048_,
         double p_234049_,
         double p_234050_,
         double p_234051_,
         double p_234052_,
         double p_234053_,
         double p_234054_,
         RandomSource p_429647_
      ) {
         return new SonicBoomParticle(p_234048_, p_234049_, p_234050_, p_234051_, p_234052_, this.sprites);
      }
   }
}
