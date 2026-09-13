package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected GustParticle(ClientLevel p_311562_, double p_310593_, double p_310162_, double p_311255_, SpriteSet p_312103_) {
      super(p_311562_, p_310593_, p_310162_, p_311255_, p_312103_.first());
      this.sprites = p_312103_;
      this.setSpriteFromAge(p_312103_);
      this.lifetime = 12 + this.random.nextInt(4);
      this.quadSize = 1.0F;
      this.setSize(1.0F, 1.0F);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightColor(float p_312195_) {
      return 15728880;
   }

   @Override
   public void tick() {
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_312576_) {
         this.sprites = p_312576_;
      }

      public Particle createParticle(
         SimpleParticleType p_311903_,
         ClientLevel p_312025_,
         double p_310149_,
         double p_310815_,
         double p_311365_,
         double p_310449_,
         double p_312082_,
         double p_312890_,
         RandomSource p_431299_
      ) {
         return new GustParticle(p_312025_, p_310149_, p_310815_, p_311365_, this.sprites);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SmallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SmallProvider(SpriteSet p_334883_) {
         this.sprites = p_334883_;
      }

      public Particle createParticle(
         SimpleParticleType p_331310_,
         ClientLevel p_331219_,
         double p_336275_,
         double p_329893_,
         double p_327953_,
         double p_328032_,
         double p_328861_,
         double p_336248_,
         RandomSource p_426896_
      ) {
         Particle particle = new GustParticle(p_331219_, p_336275_, p_329893_, p_327953_, this.sprites);
         particle.scale(0.15F);
         return particle;
      }
   }
}
