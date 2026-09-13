package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   GlowParticle(
      ClientLevel p_172136_, double p_172137_, double p_172138_, double p_172139_, double p_172140_, double p_172141_, double p_172142_, SpriteSet p_172143_
   ) {
      super(p_172136_, p_172137_, p_172138_, p_172139_, p_172140_, p_172141_, p_172142_, p_172143_.first());
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = p_172143_;
      this.quadSize *= 0.75F;
      this.hasPhysics = false;
      this.setSpriteFromAge(p_172143_);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public int getLightColor(float p_172146_) {
      float f = (this.age + p_172146_) / this.lifetime;
      f = Mth.clamp(f, 0.0F, 1.0F);
      int i = super.getLightColor(p_172146_);
      int j = i & 0xFF;
      int k = i >> 16 & 0xFF;
      j += (int)(f * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @OnlyIn(Dist.CLIENT)
   public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.25;
      private final SpriteSet sprite;

      public ElectricSparkProvider(SpriteSet p_172151_) {
         this.sprite = p_172151_;
      }

      public Particle createParticle(
         SimpleParticleType p_172162_,
         ClientLevel p_172163_,
         double p_172164_,
         double p_172165_,
         double p_172166_,
         double p_172167_,
         double p_172168_,
         double p_172169_,
         RandomSource p_430367_
      ) {
         GlowParticle glowparticle = new GlowParticle(p_172163_, p_172164_, p_172165_, p_172166_, 0.0, 0.0, 0.0, this.sprite);
         glowparticle.setColor(1.0F, 0.9F, 1.0F);
         glowparticle.setParticleSpeed(p_172167_ * 0.25, p_172168_ * 0.25, p_172169_ * 0.25);
         int i = 2;
         int j = 4;
         glowparticle.setLifetime(p_430367_.nextInt(2) + 2);
         return glowparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public GlowSquidProvider(SpriteSet p_172172_) {
         this.sprite = p_172172_;
      }

      public Particle createParticle(
         SimpleParticleType p_425016_,
         ClientLevel p_172175_,
         double p_172176_,
         double p_172177_,
         double p_172178_,
         double p_172179_,
         double p_172180_,
         double p_172181_,
         RandomSource p_426398_
      ) {
         GlowParticle glowparticle = new GlowParticle(
            p_172175_, p_172176_, p_172177_, p_172178_, 0.5 - p_426398_.nextDouble(), p_172180_, 0.5 - p_426398_.nextDouble(), this.sprite
         );
         if (p_426398_.nextBoolean()) {
            glowparticle.setColor(0.6F, 1.0F, 0.8F);
         } else {
            glowparticle.setColor(0.08F, 0.4F, 0.4F);
         }

         glowparticle.yd *= 0.2F;
         if (p_172179_ == 0.0 && p_172181_ == 0.0) {
            glowparticle.xd *= 0.1F;
            glowparticle.zd *= 0.1F;
         }

         glowparticle.setLifetime((int)(8.0 / (p_426398_.nextDouble() * 0.8 + 0.2)));
         return glowparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public ScrapeProvider(SpriteSet p_172194_) {
         this.sprite = p_172194_;
      }

      public Particle createParticle(
         SimpleParticleType p_423836_,
         ClientLevel p_172197_,
         double p_172198_,
         double p_172199_,
         double p_172200_,
         double p_172201_,
         double p_172202_,
         double p_172203_,
         RandomSource p_424261_
      ) {
         GlowParticle glowparticle = new GlowParticle(p_172197_, p_172198_, p_172199_, p_172200_, 0.0, 0.0, 0.0, this.sprite);
         if (p_424261_.nextBoolean()) {
            glowparticle.setColor(0.29F, 0.58F, 0.51F);
         } else {
            glowparticle.setColor(0.43F, 0.77F, 0.62F);
         }

         glowparticle.setParticleSpeed(p_172201_ * 0.01, p_172202_ * 0.01, p_172203_ * 0.01);
         int i = 10;
         int j = 40;
         glowparticle.setLifetime(p_424261_.nextInt(30) + 10);
         return glowparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOffProvider(SpriteSet p_172216_) {
         this.sprite = p_172216_;
      }

      public Particle createParticle(
         SimpleParticleType p_428388_,
         ClientLevel p_172219_,
         double p_172220_,
         double p_172221_,
         double p_172222_,
         double p_172223_,
         double p_172224_,
         double p_172225_,
         RandomSource p_422366_
      ) {
         GlowParticle glowparticle = new GlowParticle(p_172219_, p_172220_, p_172221_, p_172222_, 0.0, 0.0, 0.0, this.sprite);
         glowparticle.setColor(1.0F, 0.9F, 1.0F);
         glowparticle.setParticleSpeed(p_172223_ * 0.01 / 2.0, p_172224_ * 0.01, p_172225_ * 0.01 / 2.0);
         int i = 10;
         int j = 40;
         glowparticle.setLifetime(p_422366_.nextInt(30) + 10);
         return glowparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
      private static final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOnProvider(SpriteSet p_172238_) {
         this.sprite = p_172238_;
      }

      public Particle createParticle(
         SimpleParticleType p_422861_,
         ClientLevel p_172241_,
         double p_172242_,
         double p_172243_,
         double p_172244_,
         double p_172245_,
         double p_172246_,
         double p_172247_,
         RandomSource p_430182_
      ) {
         GlowParticle glowparticle = new GlowParticle(p_172241_, p_172242_, p_172243_, p_172244_, 0.0, 0.0, 0.0, this.sprite);
         glowparticle.setColor(0.91F, 0.55F, 0.08F);
         glowparticle.setParticleSpeed(p_172245_ * 0.01 / 2.0, p_172246_ * 0.01, p_172247_ * 0.01 / 2.0);
         int i = 10;
         int j = 40;
         glowparticle.setLifetime(p_430182_.nextInt(30) + 10);
         return glowparticle;
      }
   }
}
