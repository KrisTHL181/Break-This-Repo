package net.minecraft.client.particle;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireworkParticles {
   @OnlyIn(Dist.CLIENT)
   public static class FlashProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprite;

      public FlashProvider(SpriteSet p_106657_) {
         this.sprite = p_106657_;
      }

      public Particle createParticle(
         ColorParticleOption p_423479_,
         ClientLevel p_106669_,
         double p_106670_,
         double p_106671_,
         double p_106672_,
         double p_106673_,
         double p_106674_,
         double p_106675_,
         RandomSource p_429511_
      ) {
         FireworkParticles.OverlayParticle fireworkparticles$overlayparticle = new FireworkParticles.OverlayParticle(
            p_106669_, p_106670_, p_106671_, p_106672_, this.sprite.get(p_429511_)
         );
         fireworkparticles$overlayparticle.setColor(p_423479_.getRed(), p_423479_.getGreen(), p_423479_.getBlue());
         fireworkparticles$overlayparticle.setAlpha(p_423479_.getAlpha());
         return fireworkparticles$overlayparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class OverlayParticle extends SingleQuadParticle {
      OverlayParticle(ClientLevel p_106677_, double p_106678_, double p_106679_, double p_106680_, TextureAtlasSprite p_426888_) {
         super(p_106677_, p_106678_, p_106679_, p_106680_, p_426888_);
         this.lifetime = 4;
      }

      @Override
      public SingleQuadParticle.Layer getLayer() {
         return SingleQuadParticle.Layer.TRANSLUCENT;
      }

      @Override
      public void extract(QuadParticleRenderState p_425696_, Camera p_428345_, float p_430701_) {
         this.setAlpha(0.6F - (this.age + p_430701_ - 1.0F) * 0.25F * 0.5F);
         super.extract(p_425696_, p_428345_, p_430701_);
      }

      @Override
      public float getQuadSize(float p_106693_) {
         return 7.1F * Mth.sin((this.age + p_106693_ - 1.0F) * 0.25F * (float) Math.PI);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class SparkParticle extends SimpleAnimatedParticle {
      private boolean trail;
      private boolean twinkle;
      private final ParticleEngine engine;
      private float fadeR;
      private float fadeG;
      private float fadeB;
      private boolean hasFade;

      SparkParticle(
         ClientLevel p_106702_,
         double p_106703_,
         double p_106704_,
         double p_106705_,
         double p_106706_,
         double p_106707_,
         double p_106708_,
         ParticleEngine p_106709_,
         SpriteSet p_106710_
      ) {
         super(p_106702_, p_106703_, p_106704_, p_106705_, p_106710_, 0.1F);
         this.xd = p_106706_;
         this.yd = p_106707_;
         this.zd = p_106708_;
         this.engine = p_106709_;
         this.quadSize *= 0.75F;
         this.lifetime = 48 + this.random.nextInt(12);
         this.setSpriteFromAge(p_106710_);
      }

      public void setTrail(boolean p_106728_) {
         this.trail = p_106728_;
      }

      public void setTwinkle(boolean p_331075_) {
         this.twinkle = p_331075_;
      }

      @Override
      public void extract(QuadParticleRenderState p_430971_, Camera p_427685_, float p_428599_) {
         if (!this.twinkle || this.age < this.lifetime / 3 || (this.age + this.lifetime) / 3 % 2 == 0) {
            super.extract(p_430971_, p_427685_, p_428599_);
         }
      }

      @Override
      public void tick() {
         super.tick();
         if (this.trail && this.age < this.lifetime / 2 && (this.age + this.lifetime) % 2 == 0) {
            FireworkParticles.SparkParticle fireworkparticles$sparkparticle = new FireworkParticles.SparkParticle(
               this.level, this.x, this.y, this.z, 0.0, 0.0, 0.0, this.engine, this.sprites
            );
            fireworkparticles$sparkparticle.setAlpha(0.99F);
            fireworkparticles$sparkparticle.setColor(this.rCol, this.gCol, this.bCol);
            fireworkparticles$sparkparticle.age = fireworkparticles$sparkparticle.lifetime / 2;
            if (this.hasFade) {
               fireworkparticles$sparkparticle.hasFade = true;
               fireworkparticles$sparkparticle.fadeR = this.fadeR;
               fireworkparticles$sparkparticle.fadeG = this.fadeG;
               fireworkparticles$sparkparticle.fadeB = this.fadeB;
            }

            fireworkparticles$sparkparticle.twinkle = this.twinkle;
            this.engine.add(fireworkparticles$sparkparticle);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SparkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SparkProvider(SpriteSet p_106733_) {
         this.sprites = p_106733_;
      }

      public Particle createParticle(
         SimpleParticleType p_106744_,
         ClientLevel p_106745_,
         double p_106746_,
         double p_106747_,
         double p_106748_,
         double p_106749_,
         double p_106750_,
         double p_106751_,
         RandomSource p_428942_
      ) {
         FireworkParticles.SparkParticle fireworkparticles$sparkparticle = new FireworkParticles.SparkParticle(
            p_106745_, p_106746_, p_106747_, p_106748_, p_106749_, p_106750_, p_106751_, Minecraft.getInstance().particleEngine, this.sprites
         );
         fireworkparticles$sparkparticle.setAlpha(0.99F);
         return fireworkparticles$sparkparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Starter extends NoRenderParticle {
      private static final double[][] CREEPER_PARTICLE_COORDS = new double[][]{
         {0.0, 0.2}, {0.2, 0.2}, {0.2, 0.6}, {0.6, 0.6}, {0.6, 0.2}, {0.2, 0.2}, {0.2, 0.0}, {0.4, 0.0}, {0.4, -0.6}, {0.2, -0.6}, {0.2, -0.4}, {0.0, -0.4}
      };
      private static final double[][] STAR_PARTICLE_COORDS = new double[][]{
         {0.0, 1.0},
         {0.3455, 0.309},
         {0.9511, 0.309},
         {0.3795918367346939, -0.12653061224489795},
         {0.6122448979591837, -0.8040816326530612},
         {0.0, -0.35918367346938773}
      };
      private int life;
      private final ParticleEngine engine;
      private final List<FireworkExplosion> explosions;
      private boolean twinkleDelay;

      public Starter(
         ClientLevel p_106757_,
         double p_106758_,
         double p_106759_,
         double p_106760_,
         double p_106761_,
         double p_106762_,
         double p_106763_,
         ParticleEngine p_106764_,
         List<FireworkExplosion> p_332725_
      ) {
         super(p_106757_, p_106758_, p_106759_, p_106760_);
         this.xd = p_106761_;
         this.yd = p_106762_;
         this.zd = p_106763_;
         this.engine = p_106764_;
         if (p_332725_.isEmpty()) {
            throw new IllegalArgumentException("Cannot create firework starter with no explosions");
         }

         this.explosions = p_332725_;
         this.lifetime = p_332725_.size() * 2 - 1;

         for (FireworkExplosion fireworkexplosion : p_332725_) {
            if (fireworkexplosion.hasTwinkle()) {
               this.twinkleDelay = true;
               this.lifetime += 15;
               break;
            }
         }
      }

      @Override
      public void tick() {
         if (this.life == 0) {
            boolean flag = this.isFarAwayFromCamera();
            boolean flag1 = false;
            if (this.explosions.size() >= 3) {
               flag1 = true;
            } else {
               for (FireworkExplosion fireworkexplosion : this.explosions) {
                  if (fireworkexplosion.shape() == FireworkExplosion.Shape.LARGE_BALL) {
                     flag1 = true;
                     break;
                  }
               }
            }

            SoundEvent soundevent1;
            if (flag1) {
               soundevent1 = flag ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST;
            } else {
               soundevent1 = flag ? SoundEvents.FIREWORK_ROCKET_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_BLAST;
            }

            this.level.playLocalSound(this.x, this.y, this.z, soundevent1, SoundSource.AMBIENT, 20.0F, 0.95F + this.random.nextFloat() * 0.1F, true);
         }

         if (this.life % 2 == 0 && this.life / 2 < this.explosions.size()) {
            int j = this.life / 2;
            FireworkExplosion fireworkexplosion1 = this.explosions.get(j);
            boolean flag3 = fireworkexplosion1.hasTrail();
            boolean flag4 = fireworkexplosion1.hasTwinkle();
            IntList intlist = fireworkexplosion1.colors();
            IntList intlist1 = fireworkexplosion1.fadeColors();
            if (intlist.isEmpty()) {
               intlist = IntList.of(DyeColor.BLACK.getFireworkColor());
            }

            switch (fireworkexplosion1.shape()) {
               case SMALL_BALL:
                  this.createParticleBall(0.25, 2, intlist, intlist1, flag3, flag4);
                  break;
               case LARGE_BALL:
                  this.createParticleBall(0.5, 4, intlist, intlist1, flag3, flag4);
                  break;
               case STAR:
                  this.createParticleShape(0.5, STAR_PARTICLE_COORDS, intlist, intlist1, flag3, flag4, false);
                  break;
               case CREEPER:
                  this.createParticleShape(0.5, CREEPER_PARTICLE_COORDS, intlist, intlist1, flag3, flag4, true);
                  break;
               case BURST:
                  this.createParticleBurst(intlist, intlist1, flag3, flag4);
            }

            int i = intlist.getInt(0);
            this.engine.createParticle(ColorParticleOption.create(ParticleTypes.FLASH, i), this.x, this.y, this.z, 0.0, 0.0, 0.0);
         }

         this.life++;
         if (this.life > this.lifetime) {
            if (this.twinkleDelay) {
               boolean flag2 = this.isFarAwayFromCamera();
               SoundEvent soundevent = flag2 ? SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR : SoundEvents.FIREWORK_ROCKET_TWINKLE;
               this.level.playLocalSound(this.x, this.y, this.z, soundevent, SoundSource.AMBIENT, 20.0F, 0.9F + this.random.nextFloat() * 0.15F, true);
            }

            this.remove();
         }
      }

      private boolean isFarAwayFromCamera() {
         Minecraft minecraft = Minecraft.getInstance();
         return minecraft.gameRenderer.getMainCamera().position().distanceToSqr(this.x, this.y, this.z) >= 256.0;
      }

      private void createParticle(
         double p_106768_,
         double p_106769_,
         double p_106770_,
         double p_106771_,
         double p_106772_,
         double p_106773_,
         IntList p_329729_,
         IntList p_330193_,
         boolean p_106776_,
         boolean p_106777_
      ) {
         FireworkParticles.SparkParticle fireworkparticles$sparkparticle = (FireworkParticles.SparkParticle)this.engine
            .createParticle(ParticleTypes.FIREWORK, p_106768_, p_106769_, p_106770_, p_106771_, p_106772_, p_106773_);
         fireworkparticles$sparkparticle.setTrail(p_106776_);
         fireworkparticles$sparkparticle.setTwinkle(p_106777_);
         fireworkparticles$sparkparticle.setAlpha(0.99F);
         fireworkparticles$sparkparticle.setColor(Util.<Integer>getRandom(p_329729_, this.random));
         if (!p_330193_.isEmpty()) {
            fireworkparticles$sparkparticle.setFadeColor(Util.<Integer>getRandom(p_330193_, this.random));
         }
      }

      private void createParticleBall(double p_106779_, int p_106780_, IntList p_331387_, IntList p_331480_, boolean p_106783_, boolean p_106784_) {
         double d0 = this.x;
         double d1 = this.y;
         double d2 = this.z;

         for (int i = -p_106780_; i <= p_106780_; i++) {
            for (int j = -p_106780_; j <= p_106780_; j++) {
               for (int k = -p_106780_; k <= p_106780_; k++) {
                  double d3 = j + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double d4 = i + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double d5 = k + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
                  double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5) / p_106779_ + this.random.nextGaussian() * 0.05;
                  this.createParticle(d0, d1, d2, d3 / d6, d4 / d6, d5 / d6, p_331387_, p_331480_, p_106783_, p_106784_);
                  if (i != -p_106780_ && i != p_106780_ && j != -p_106780_ && j != p_106780_) {
                     k += p_106780_ * 2 - 1;
                  }
               }
            }
         }
      }

      private void createParticleShape(
         double p_106786_, double[][] p_106787_, IntList p_330103_, IntList p_332201_, boolean p_106790_, boolean p_106791_, boolean p_106792_
      ) {
         double d0 = p_106787_[0][0];
         double d1 = p_106787_[0][1];
         this.createParticle(this.x, this.y, this.z, d0 * p_106786_, d1 * p_106786_, 0.0, p_330103_, p_332201_, p_106790_, p_106791_);
         float f = this.random.nextFloat() * (float) Math.PI;
         double d2 = p_106792_ ? 0.034 : 0.34;

         for (int i = 0; i < 3; i++) {
            double d3 = f + i * (float) Math.PI * d2;
            double d4 = d0;
            double d5 = d1;

            for (int j = 1; j < p_106787_.length; j++) {
               double d6 = p_106787_[j][0];
               double d7 = p_106787_[j][1];

               for (double d8 = 0.25; d8 <= 1.0; d8 += 0.25) {
                  double d9 = Mth.lerp(d8, d4, d6) * p_106786_;
                  double d10 = Mth.lerp(d8, d5, d7) * p_106786_;
                  double d11 = d9 * Math.sin(d3);
                  d9 *= Math.cos(d3);

                  for (double d12 = -1.0; d12 <= 1.0; d12 += 2.0) {
                     this.createParticle(this.x, this.y, this.z, d9 * d12, d10, d11 * d12, p_330103_, p_332201_, p_106790_, p_106791_);
                  }
               }

               d4 = d6;
               d5 = d7;
            }
         }
      }

      private void createParticleBurst(IntList p_336354_, IntList p_328829_, boolean p_106796_, boolean p_106797_) {
         double d0 = this.random.nextGaussian() * 0.05;
         double d1 = this.random.nextGaussian() * 0.05;

         for (int i = 0; i < 70; i++) {
            double d2 = this.xd * 0.5 + this.random.nextGaussian() * 0.15 + d0;
            double d3 = this.zd * 0.5 + this.random.nextGaussian() * 0.15 + d1;
            double d4 = this.yd * 0.5 + this.random.nextDouble() * 0.5;
            this.createParticle(this.x, this.y, this.z, d2, d4, d3, p_336354_, p_328829_, p_106796_, p_106797_);
         }
      }
   }
}
