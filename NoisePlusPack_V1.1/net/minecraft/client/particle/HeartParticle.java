package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeartParticle extends SingleQuadParticle {
   HeartParticle(ClientLevel p_106847_, double p_106848_, double p_106849_, double p_106850_, TextureAtlasSprite p_428138_) {
      super(p_106847_, p_106848_, p_106849_, p_106850_, 0.0, 0.0, 0.0, p_428138_);
      this.speedUpWhenYMotionIsBlocked = true;
      this.friction = 0.86F;
      this.xd *= 0.01F;
      this.yd *= 0.01F;
      this.zd *= 0.01F;
      this.yd += 0.1;
      this.quadSize *= 1.5F;
      this.lifetime = 16;
      this.hasPhysics = false;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public float getQuadSize(float p_106860_) {
      return this.quadSize * Mth.clamp((this.age + p_106860_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public static class AngryVillagerProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public AngryVillagerProvider(SpriteSet p_106863_) {
         this.sprite = p_106863_;
      }

      public Particle createParticle(
         SimpleParticleType p_106874_,
         ClientLevel p_106875_,
         double p_106876_,
         double p_106877_,
         double p_106878_,
         double p_106879_,
         double p_106880_,
         double p_106881_,
         RandomSource p_427368_
      ) {
         HeartParticle heartparticle = new HeartParticle(p_106875_, p_106876_, p_106877_ + 0.5, p_106878_, this.sprite.get(p_427368_));
         heartparticle.setColor(1.0F, 1.0F, 1.0F);
         return heartparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_106884_) {
         this.sprite = p_106884_;
      }

      public Particle createParticle(
         SimpleParticleType p_106895_,
         ClientLevel p_106896_,
         double p_106897_,
         double p_106898_,
         double p_106899_,
         double p_106900_,
         double p_106901_,
         double p_106902_,
         RandomSource p_424051_
      ) {
         return new HeartParticle(p_106896_, p_106897_, p_106898_, p_106899_, this.sprite.get(p_424051_));
      }
   }
}
