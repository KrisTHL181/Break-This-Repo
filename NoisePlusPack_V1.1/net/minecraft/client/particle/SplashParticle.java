package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplashParticle extends WaterDropParticle {
   SplashParticle(
      ClientLevel p_107929_,
      double p_107930_,
      double p_107931_,
      double p_107932_,
      double p_107933_,
      double p_107934_,
      double p_107935_,
      TextureAtlasSprite p_423176_
   ) {
      super(p_107929_, p_107930_, p_107931_, p_107932_, p_423176_);
      this.gravity = 0.04F;
      if (p_107934_ == 0.0 && (p_107933_ != 0.0 || p_107935_ != 0.0)) {
         this.xd = p_107933_;
         this.yd = 0.1;
         this.zd = p_107935_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_107947_) {
         this.sprite = p_107947_;
      }

      public Particle createParticle(
         SimpleParticleType p_429292_,
         ClientLevel p_107950_,
         double p_107951_,
         double p_107952_,
         double p_107953_,
         double p_107954_,
         double p_107955_,
         double p_107956_,
         RandomSource p_428943_
      ) {
         return new SplashParticle(p_107950_, p_107951_, p_107952_, p_107953_, p_107954_, p_107955_, p_107956_, this.sprite.get(p_428943_));
      }
   }
}
