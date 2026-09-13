package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyParticle extends SingleQuadParticle {
   private static final float PARTICLE_FADE_OUT_LIGHT_TIME = 0.3F;
   private static final float PARTICLE_FADE_IN_LIGHT_TIME = 0.1F;
   private static final float PARTICLE_FADE_OUT_ALPHA_TIME = 0.5F;
   private static final float PARTICLE_FADE_IN_ALPHA_TIME = 0.3F;
   private static final int PARTICLE_MIN_LIFETIME = 200;
   private static final int PARTICLE_MAX_LIFETIME = 300;

   FireflyParticle(
      ClientLevel p_392461_,
      double p_395781_,
      double p_397251_,
      double p_392758_,
      double p_391215_,
      double p_396621_,
      double p_394403_,
      TextureAtlasSprite p_427115_
   ) {
      super(p_392461_, p_395781_, p_397251_, p_392758_, p_391215_, p_396621_, p_394403_, p_427115_);
      this.speedUpWhenYMotionIsBlocked = true;
      this.friction = 0.96F;
      this.quadSize *= 0.75F;
      this.yd *= 0.8F;
      this.xd *= 0.8F;
      this.zd *= 0.8F;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public int getLightColor(float p_391946_) {
      return (int)(255.0F * getFadeAmount(this.getLifetimeProgress(this.age + p_391946_), 0.1F, 0.3F));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
         this.remove();
      } else {
         this.setAlpha(getFadeAmount(this.getLifetimeProgress(this.age), 0.3F, 0.5F));
         if (this.random.nextFloat() > 0.95F || this.age == 1) {
            this.setParticleSpeed(-0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat());
         }
      }
   }

   private float getLifetimeProgress(float p_391220_) {
      return Mth.clamp(p_391220_ / this.lifetime, 0.0F, 1.0F);
   }

   private static float getFadeAmount(float p_392433_, float p_396209_, float p_393676_) {
      if (p_392433_ >= 1.0F - p_396209_) {
         return (1.0F - p_392433_) / p_396209_;
      } else {
         return p_392433_ <= p_393676_ ? p_392433_ / p_393676_ : 1.0F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class FireflyProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public FireflyProvider(SpriteSet p_391324_) {
         this.sprite = p_391324_;
      }

      public Particle createParticle(
         SimpleParticleType p_392220_,
         ClientLevel p_392739_,
         double p_395400_,
         double p_396183_,
         double p_392361_,
         double p_397702_,
         double p_393784_,
         double p_397769_,
         RandomSource p_425068_
      ) {
         FireflyParticle fireflyparticle = new FireflyParticle(
            p_392739_,
            p_395400_,
            p_396183_,
            p_392361_,
            0.5 - p_425068_.nextDouble(),
            p_425068_.nextBoolean() ? p_393784_ : -p_393784_,
            0.5 - p_425068_.nextDouble(),
            this.sprite.get(p_425068_)
         );
         fireflyparticle.setLifetime(p_425068_.nextIntBetweenInclusive(200, 300));
         fireflyparticle.scale(1.5F);
         fireflyparticle.setAlpha(0.0F);
         return fireflyparticle;
      }
   }
}
