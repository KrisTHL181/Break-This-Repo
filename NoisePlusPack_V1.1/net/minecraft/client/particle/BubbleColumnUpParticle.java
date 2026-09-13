package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BubbleColumnUpParticle extends SingleQuadParticle {
   BubbleColumnUpParticle(
      ClientLevel p_105733_,
      double p_105734_,
      double p_105735_,
      double p_105736_,
      double p_105737_,
      double p_105738_,
      double p_105739_,
      TextureAtlasSprite p_428951_
   ) {
      super(p_105733_, p_105734_, p_105735_, p_105736_, p_428951_);
      this.gravity = -0.125F;
      this.friction = 0.85F;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.xd = p_105737_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.yd = p_105738_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.zd = p_105739_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.lifetime = (int)(40.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
         this.remove();
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_105753_) {
         this.sprite = p_105753_;
      }

      public Particle createParticle(
         SimpleParticleType p_429766_,
         ClientLevel p_105756_,
         double p_105757_,
         double p_105758_,
         double p_105759_,
         double p_105760_,
         double p_105761_,
         double p_105762_,
         RandomSource p_430612_
      ) {
         return new BubbleColumnUpParticle(p_105756_, p_105757_, p_105758_, p_105759_, p_105760_, p_105761_, p_105762_, this.sprite.get(p_430612_));
      }
   }
}
