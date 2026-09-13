package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyStraightTowardsParticle extends SingleQuadParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;
   private final int startColor;
   private final int endColor;

   FlyStraightTowardsParticle(
      ClientLevel p_331392_,
      double p_328454_,
      double p_335936_,
      double p_334729_,
      double p_335747_,
      double p_333574_,
      double p_334122_,
      int p_328231_,
      int p_329614_,
      TextureAtlasSprite p_430070_
   ) {
      super(p_331392_, p_328454_, p_335936_, p_334729_, p_430070_);
      this.xd = p_335747_;
      this.yd = p_333574_;
      this.zd = p_334122_;
      this.xStart = p_328454_;
      this.yStart = p_335936_;
      this.zStart = p_334729_;
      this.xo = p_328454_ + p_335747_;
      this.yo = p_335936_ + p_333574_;
      this.zo = p_334729_ + p_334122_;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      this.hasPhysics = false;
      this.lifetime = (int)(this.random.nextFloat() * 5.0F) + 25;
      this.startColor = p_328231_;
      this.endColor = p_329614_;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void move(double p_328657_, double p_332590_, double p_331282_) {
   }

   @Override
   public int getLightColor(float p_334272_) {
      return 240;
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
         float f1 = 1.0F - f;
         this.x = this.xStart + this.xd * f1;
         this.y = this.yStart + this.yd * f1;
         this.z = this.zStart + this.zd * f1;
         int i = ARGB.srgbLerp(f, this.startColor, this.endColor);
         this.setColor(ARGB.red(i) / 255.0F, ARGB.green(i) / 255.0F, ARGB.blue(i) / 255.0F);
         this.setAlpha(ARGB.alpha(i) / 255.0F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class OminousSpawnProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public OminousSpawnProvider(SpriteSet p_327679_) {
         this.sprite = p_327679_;
      }

      public Particle createParticle(
         SimpleParticleType p_331905_,
         ClientLevel p_330985_,
         double p_334366_,
         double p_328334_,
         double p_330716_,
         double p_333567_,
         double p_329124_,
         double p_335449_,
         RandomSource p_422519_
      ) {
         FlyStraightTowardsParticle flystraighttowardsparticle = new FlyStraightTowardsParticle(
            p_330985_, p_334366_, p_328334_, p_330716_, p_333567_, p_329124_, p_335449_, -12210434, -1, this.sprite.get(p_422519_)
         );
         flystraighttowardsparticle.scale(Mth.randomBetween(p_330985_.getRandom(), 3.0F, 5.0F));
         return flystraighttowardsparticle;
      }
   }
}
