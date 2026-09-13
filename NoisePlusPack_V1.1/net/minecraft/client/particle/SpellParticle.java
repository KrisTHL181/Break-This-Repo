package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpellParticle extends SingleQuadParticle {
   private static final RandomSource RANDOM = RandomSource.create();
   private final SpriteSet sprites;
   private float originalAlpha = 1.0F;

   SpellParticle(
      ClientLevel p_107762_, double p_107763_, double p_107764_, double p_107765_, double p_107766_, double p_107767_, double p_107768_, SpriteSet p_107769_
   ) {
      super(p_107762_, p_107763_, p_107764_, p_107765_, 0.5 - RANDOM.nextDouble(), p_107767_, 0.5 - RANDOM.nextDouble(), p_107769_.first());
      this.friction = 0.96F;
      this.gravity = -0.1F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = p_107769_;
      this.yd *= 0.2F;
      if (p_107766_ == 0.0 && p_107768_ == 0.0) {
         this.xd *= 0.1F;
         this.zd *= 0.1F;
      }

      this.quadSize *= 0.75F;
      this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.setSpriteFromAge(p_107769_);
      if (this.isCloseToScopingPlayer()) {
         this.setAlpha(0.0F);
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      if (this.isCloseToScopingPlayer()) {
         this.alpha = 0.0F;
      } else {
         this.alpha = Mth.lerp(0.05F, this.alpha, this.originalAlpha);
      }
   }

   @Override
   protected void setAlpha(float p_332254_) {
      super.setAlpha(p_332254_);
      this.originalAlpha = p_332254_;
   }

   private boolean isCloseToScopingPlayer() {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localplayer = minecraft.player;
      return localplayer != null
         && localplayer.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0
         && minecraft.options.getCameraType().isFirstPerson()
         && localplayer.isScoping();
   }

   @OnlyIn(Dist.CLIENT)
   public static class InstantProvider implements ParticleProvider<SpellParticleOption> {
      private final SpriteSet sprite;

      public InstantProvider(SpriteSet p_107805_) {
         this.sprite = p_107805_;
      }

      public Particle createParticle(
         SpellParticleOption p_422621_,
         ClientLevel p_107808_,
         double p_107809_,
         double p_107810_,
         double p_107811_,
         double p_107812_,
         double p_107813_,
         double p_107814_,
         RandomSource p_424003_
      ) {
         SpellParticle spellparticle = new SpellParticle(p_107808_, p_107809_, p_107810_, p_107811_, p_107812_, p_107813_, p_107814_, this.sprite);
         spellparticle.setColor(p_422621_.getRed(), p_422621_.getGreen(), p_422621_.getBlue());
         spellparticle.setPower(p_422621_.getPower());
         return spellparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class MobEffectProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprite;

      public MobEffectProvider(SpriteSet p_328237_) {
         this.sprite = p_328237_;
      }

      public Particle createParticle(
         ColorParticleOption p_329447_,
         ClientLevel p_333235_,
         double p_327722_,
         double p_329690_,
         double p_335059_,
         double p_332176_,
         double p_334375_,
         double p_330165_,
         RandomSource p_425607_
      ) {
         SpellParticle spellparticle = new SpellParticle(p_333235_, p_327722_, p_329690_, p_335059_, p_332176_, p_334375_, p_330165_, this.sprite);
         spellparticle.setColor(p_329447_.getRed(), p_329447_.getGreen(), p_329447_.getBlue());
         spellparticle.setAlpha(p_329447_.getAlpha());
         return spellparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_107847_) {
         this.sprite = p_107847_;
      }

      public Particle createParticle(
         SimpleParticleType p_107858_,
         ClientLevel p_107859_,
         double p_107860_,
         double p_107861_,
         double p_107862_,
         double p_107863_,
         double p_107864_,
         double p_107865_,
         RandomSource p_430604_
      ) {
         return new SpellParticle(p_107859_, p_107860_, p_107861_, p_107862_, p_107863_, p_107864_, p_107865_, this.sprite);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WitchProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WitchProvider(SpriteSet p_107868_) {
         this.sprite = p_107868_;
      }

      public Particle createParticle(
         SimpleParticleType p_423758_,
         ClientLevel p_107871_,
         double p_107872_,
         double p_107873_,
         double p_107874_,
         double p_107875_,
         double p_107876_,
         double p_107877_,
         RandomSource p_431328_
      ) {
         SpellParticle spellparticle = new SpellParticle(p_107871_, p_107872_, p_107873_, p_107874_, p_107875_, p_107876_, p_107877_, this.sprite);
         float f = p_431328_.nextFloat() * 0.5F + 0.35F;
         spellparticle.setColor(1.0F * f, 0.0F * f, 1.0F * f);
         return spellparticle;
      }
   }
}
