package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReversePortalParticle extends PortalParticle {
   ReversePortalParticle(
      ClientLevel p_107590_,
      double p_107591_,
      double p_107592_,
      double p_107593_,
      double p_107594_,
      double p_107595_,
      double p_107596_,
      TextureAtlasSprite p_422559_
   ) {
      super(p_107590_, p_107591_, p_107592_, p_107593_, p_107594_, p_107595_, p_107596_, p_422559_);
      this.quadSize *= 1.5F;
      this.lifetime = (int)(this.random.nextFloat() * 2.0F) + 60;
   }

   @Override
   public float getQuadSize(float p_107608_) {
      float f = 1.0F - (this.age + p_107608_) / (this.lifetime * 1.5F);
      return this.quadSize * f;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = (float)this.age / this.lifetime;
         this.x = this.x + this.xd * f;
         this.y = this.y + this.yd * f;
         this.z = this.z + this.zd * f;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ReversePortalProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ReversePortalProvider(SpriteSet p_107611_) {
         this.sprite = p_107611_;
      }

      public Particle createParticle(
         SimpleParticleType p_429391_,
         ClientLevel p_107614_,
         double p_107615_,
         double p_107616_,
         double p_107617_,
         double p_107618_,
         double p_107619_,
         double p_107620_,
         RandomSource p_428839_
      ) {
         return new ReversePortalParticle(p_107614_, p_107615_, p_107616_, p_107617_, p_107618_, p_107619_, p_107620_, this.sprite.get(p_428839_));
      }
   }
}
