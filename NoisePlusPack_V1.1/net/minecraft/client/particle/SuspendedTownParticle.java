package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedTownParticle extends SingleQuadParticle {
   SuspendedTownParticle(
      ClientLevel p_108104_,
      double p_108105_,
      double p_108106_,
      double p_108107_,
      double p_108108_,
      double p_108109_,
      double p_108110_,
      TextureAtlasSprite p_430016_
   ) {
      super(p_108104_, p_108105_, p_108106_, p_108107_, p_108108_, p_108109_, p_108110_, p_430016_);
      float f = this.random.nextFloat() * 0.1F + 0.2F;
      this.rCol = f;
      this.gCol = f;
      this.bCol = f;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
      this.xd *= 0.02F;
      this.yd *= 0.02F;
      this.zd *= 0.02F;
      this.lifetime = (int)(20.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void move(double p_108122_, double p_108123_, double p_108124_) {
      this.setBoundingBox(this.getBoundingBox().move(p_108122_, p_108123_, p_108124_));
      this.setLocationFromBoundingbox();
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.99;
         this.yd *= 0.99;
         this.zd *= 0.99;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ComposterFillProvider(SpriteSet p_108128_) {
         this.sprite = p_108128_;
      }

      public Particle createParticle(
         SimpleParticleType p_426186_,
         ClientLevel p_108131_,
         double p_108132_,
         double p_108133_,
         double p_108134_,
         double p_108135_,
         double p_108136_,
         double p_108137_,
         RandomSource p_428244_
      ) {
         SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
            p_108131_, p_108132_, p_108133_, p_108134_, p_108135_, p_108136_, p_108137_, this.sprite.get(p_428244_)
         );
         suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
         suspendedtownparticle.setLifetime(3 + p_108131_.getRandom().nextInt(5));
         return suspendedtownparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DolphinSpeedProvider(SpriteSet p_108149_) {
         this.sprite = p_108149_;
      }

      public Particle createParticle(
         SimpleParticleType p_108160_,
         ClientLevel p_108161_,
         double p_108162_,
         double p_108163_,
         double p_108164_,
         double p_108165_,
         double p_108166_,
         double p_108167_,
         RandomSource p_427093_
      ) {
         SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
            p_108161_, p_108162_, p_108163_, p_108164_, p_108165_, p_108166_, p_108167_, this.sprite.get(p_427093_)
         );
         suspendedtownparticle.setColor(0.3F, 0.5F, 1.0F);
         suspendedtownparticle.setAlpha(1.0F - p_427093_.nextFloat() * 0.7F);
         suspendedtownparticle.setLifetime(suspendedtownparticle.getLifetime() / 2);
         return suspendedtownparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EggCrackProvider(SpriteSet p_277756_) {
         this.sprite = p_277756_;
      }

      public Particle createParticle(
         SimpleParticleType p_277584_,
         ClientLevel p_277587_,
         double p_277722_,
         double p_277508_,
         double p_277797_,
         double p_277537_,
         double p_277578_,
         double p_277397_,
         RandomSource p_426611_
      ) {
         SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
            p_277587_, p_277722_, p_277508_, p_277797_, p_277537_, p_277578_, p_277397_, this.sprite.get(p_426611_)
         );
         suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
         return suspendedtownparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HappyVillagerProvider(SpriteSet p_108170_) {
         this.sprite = p_108170_;
      }

      public Particle createParticle(
         SimpleParticleType p_108181_,
         ClientLevel p_108182_,
         double p_108183_,
         double p_108184_,
         double p_108185_,
         double p_108186_,
         double p_108187_,
         double p_108188_,
         RandomSource p_424125_
      ) {
         SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
            p_108182_, p_108183_, p_108184_, p_108185_, p_108186_, p_108187_, p_108188_, this.sprite.get(p_424125_)
         );
         suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
         return suspendedtownparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_108191_) {
         this.sprite = p_108191_;
      }

      public Particle createParticle(
         SimpleParticleType p_426001_,
         ClientLevel p_108194_,
         double p_108195_,
         double p_108196_,
         double p_108197_,
         double p_108198_,
         double p_108199_,
         double p_108200_,
         RandomSource p_427704_
      ) {
         return new SuspendedTownParticle(p_108194_, p_108195_, p_108196_, p_108197_, p_108198_, p_108199_, p_108200_, this.sprite.get(p_427704_));
      }
   }
}
