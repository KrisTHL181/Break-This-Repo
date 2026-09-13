package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonBreathParticle extends SingleQuadParticle {
   private static final int COLOR_MIN = 11993298;
   private static final int COLOR_MAX = 14614777;
   private static final float COLOR_MIN_RED = 0.7176471F;
   private static final float COLOR_MIN_GREEN = 0.0F;
   private static final float COLOR_MIN_BLUE = 0.8235294F;
   private static final float COLOR_MAX_RED = 0.8745098F;
   private static final float COLOR_MAX_GREEN = 0.0F;
   private static final float COLOR_MAX_BLUE = 0.9764706F;
   private boolean hasHitGround;
   private final SpriteSet sprites;

   DragonBreathParticle(
      ClientLevel p_106005_, double p_106006_, double p_106007_, double p_106008_, double p_106009_, double p_106010_, double p_106011_, SpriteSet p_106012_
   ) {
      super(p_106005_, p_106006_, p_106007_, p_106008_, p_106012_.first());
      this.friction = 0.96F;
      this.xd = p_106009_;
      this.yd = p_106010_;
      this.zd = p_106011_;
      this.rCol = Mth.nextFloat(this.random, 0.7176471F, 0.8745098F);
      this.gCol = Mth.nextFloat(this.random, 0.0F, 0.0F);
      this.bCol = Mth.nextFloat(this.random, 0.8235294F, 0.9764706F);
      this.quadSize *= 0.75F;
      this.lifetime = (int)(20.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasHitGround = false;
      this.hasPhysics = false;
      this.sprites = p_106012_;
      this.setSpriteFromAge(p_106012_);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
         if (this.onGround) {
            this.yd = 0.0;
            this.hasHitGround = true;
         }

         if (this.hasHitGround) {
            this.yd += 0.002;
         }

         this.move(this.xd, this.yd, this.zd);
         if (this.y == this.yo) {
            this.xd *= 1.1;
            this.zd *= 1.1;
         }

         this.xd = this.xd * this.friction;
         this.zd = this.zd * this.friction;
         if (this.hasHitGround) {
            this.yd = this.yd * this.friction;
         }
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public float getQuadSize(float p_106026_) {
      return this.quadSize * Mth.clamp((this.age + p_106026_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<PowerParticleOption> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_106029_) {
         this.sprites = p_106029_;
      }

      public Particle createParticle(
         PowerParticleOption p_430770_,
         ClientLevel p_106032_,
         double p_106033_,
         double p_106034_,
         double p_106035_,
         double p_106036_,
         double p_106037_,
         double p_106038_,
         RandomSource p_424462_
      ) {
         DragonBreathParticle dragonbreathparticle = new DragonBreathParticle(
            p_106032_, p_106033_, p_106034_, p_106035_, p_106036_, p_106037_, p_106038_, this.sprites
         );
         dragonbreathparticle.setPower(p_430770_.getPower());
         return dragonbreathparticle;
      }
   }
}
