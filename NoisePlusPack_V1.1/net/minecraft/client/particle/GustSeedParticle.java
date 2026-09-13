package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
   private final double scale;
   private final int tickDelayInBetween;

   GustSeedParticle(ClientLevel p_312399_, double p_312363_, double p_309505_, double p_311805_, double p_330876_, int p_331354_, int p_334045_) {
      super(p_312399_, p_312363_, p_309505_, p_311805_, 0.0, 0.0, 0.0);
      this.scale = p_330876_;
      this.lifetime = p_331354_;
      this.tickDelayInBetween = p_334045_;
   }

   @Override
   public void tick() {
      if (this.age % (this.tickDelayInBetween + 1) == 0) {
         for (int i = 0; i < 3; i++) {
            double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            this.level.addParticle(ParticleTypes.GUST, d0, d1, d2, (float)this.age / this.lifetime, 0.0, 0.0);
         }
      }

      if (this.age++ == this.lifetime) {
         this.remove();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final double scale;
      private final int lifetime;
      private final int tickDelayInBetween;

      public Provider(double p_331106_, int p_334776_, int p_330209_) {
         this.scale = p_331106_;
         this.lifetime = p_334776_;
         this.tickDelayInBetween = p_330209_;
      }

      public Particle createParticle(
         SimpleParticleType p_309959_,
         ClientLevel p_312995_,
         double p_310097_,
         double p_313201_,
         double p_310511_,
         double p_310468_,
         double p_310282_,
         double p_311555_,
         RandomSource p_431614_
      ) {
         return new GustSeedParticle(p_312995_, p_310097_, p_313201_, p_310511_, this.scale, this.lifetime, this.tickDelayInBetween);
      }
   }
}
