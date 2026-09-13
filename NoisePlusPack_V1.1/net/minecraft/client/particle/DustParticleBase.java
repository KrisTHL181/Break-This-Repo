package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustParticleBase<T extends ScalableParticleOptionsBase> extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected DustParticleBase(
      ClientLevel p_172094_,
      double p_172095_,
      double p_172096_,
      double p_172097_,
      double p_172098_,
      double p_172099_,
      double p_172100_,
      T p_335358_,
      SpriteSet p_172102_
   ) {
      super(p_172094_, p_172095_, p_172096_, p_172097_, p_172098_, p_172099_, p_172100_, p_172102_.first());
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = p_172102_;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.quadSize = this.quadSize * (0.75F * p_335358_.getScale());
      int i = (int)(8.0 / (this.random.nextDouble() * 0.8 + 0.2));
      this.lifetime = (int)Math.max(i * p_335358_.getScale(), 1.0F);
      this.setSpriteFromAge(p_172102_);
   }

   protected float randomizeColor(float p_172105_, float p_172106_) {
      return (this.random.nextFloat() * 0.2F + 0.8F) * p_172105_ * p_172106_;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public float getQuadSize(float p_172109_) {
      return this.quadSize * Mth.clamp((this.age + p_172109_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }
}
