package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedParticle extends SingleQuadParticle {
   SuspendedParticle(ClientLevel p_172409_, double p_172411_, double p_172412_, double p_172413_, TextureAtlasSprite p_428743_) {
      super(p_172409_, p_172411_, p_172412_ - 0.125, p_172413_, p_428743_);
      this.setSize(0.01F, 0.01F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   SuspendedParticle(
      ClientLevel p_172403_,
      double p_172405_,
      double p_172406_,
      double p_172407_,
      double p_428262_,
      double p_427527_,
      double p_429865_,
      TextureAtlasSprite p_430318_
   ) {
      super(p_172403_, p_172405_, p_172406_ - 0.125, p_172407_, p_428262_, p_427527_, p_429865_, p_430318_);
      this.setSize(0.01F, 0.01F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @OnlyIn(Dist.CLIENT)
   public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public CrimsonSporeProvider(SpriteSet p_108042_) {
         this.sprite = p_108042_;
      }

      public Particle createParticle(
         SimpleParticleType p_108053_,
         ClientLevel p_108054_,
         double p_108055_,
         double p_108056_,
         double p_108057_,
         double p_108058_,
         double p_108059_,
         double p_108060_,
         RandomSource p_429495_
      ) {
         double d0 = p_429495_.nextGaussian() * 1.0E-6F;
         double d1 = p_429495_.nextGaussian() * 1.0E-4F;
         double d2 = p_429495_.nextGaussian() * 1.0E-6F;
         SuspendedParticle suspendedparticle = new SuspendedParticle(p_108054_, p_108055_, p_108056_, p_108057_, d0, d1, d2, this.sprite.get(p_429495_));
         suspendedparticle.setColor(0.9F, 0.4F, 0.5F);
         return suspendedparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomAirProvider(SpriteSet p_172419_) {
         this.sprite = p_172419_;
      }

      public Particle createParticle(
         SimpleParticleType p_428968_,
         ClientLevel p_172422_,
         double p_172423_,
         double p_172424_,
         double p_172425_,
         double p_172426_,
         double p_172427_,
         double p_172428_,
         RandomSource p_425028_
      ) {
         SuspendedParticle suspendedparticle = new SuspendedParticle(p_172422_, p_172423_, p_172424_, p_172425_, 0.0, -0.8F, 0.0, this.sprite.get(p_425028_)) {
            @Override
            public Optional<ParticleLimit> getParticleLimit() {
               return Optional.of(ParticleLimit.SPORE_BLOSSOM);
            }
         };
         suspendedparticle.lifetime = Mth.randomBetweenInclusive(p_425028_, 500, 1000);
         suspendedparticle.gravity = 0.01F;
         suspendedparticle.setColor(0.32F, 0.5F, 0.22F);
         return suspendedparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public UnderwaterProvider(SpriteSet p_108063_) {
         this.sprite = p_108063_;
      }

      public Particle createParticle(
         SimpleParticleType p_425285_,
         ClientLevel p_108066_,
         double p_108067_,
         double p_108068_,
         double p_108069_,
         double p_108070_,
         double p_108071_,
         double p_108072_,
         RandomSource p_423565_
      ) {
         SuspendedParticle suspendedparticle = new SuspendedParticle(p_108066_, p_108067_, p_108068_, p_108069_, this.sprite.get(p_423565_));
         suspendedparticle.setColor(0.4F, 0.4F, 0.7F);
         return suspendedparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WarpedSporeProvider(SpriteSet p_108084_) {
         this.sprite = p_108084_;
      }

      public Particle createParticle(
         SimpleParticleType p_108095_,
         ClientLevel p_108096_,
         double p_108097_,
         double p_108098_,
         double p_108099_,
         double p_108100_,
         double p_108101_,
         double p_108102_,
         RandomSource p_431712_
      ) {
         double d0 = p_431712_.nextFloat() * -1.9 * p_431712_.nextFloat() * 0.1;
         SuspendedParticle suspendedparticle = new SuspendedParticle(p_108096_, p_108097_, p_108098_, p_108099_, 0.0, d0, 0.0, this.sprite.get(p_431712_));
         suspendedparticle.setColor(0.1F, 0.1F, 0.3F);
         suspendedparticle.setSize(0.001F, 0.001F);
         return suspendedparticle;
      }
   }
}
