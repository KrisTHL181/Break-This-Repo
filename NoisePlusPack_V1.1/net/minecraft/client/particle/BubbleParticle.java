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
public class BubbleParticle extends SingleQuadParticle {
   BubbleParticle(
      ClientLevel p_105773_,
      double p_105774_,
      double p_105775_,
      double p_105776_,
      double p_105777_,
      double p_105778_,
      double p_105779_,
      TextureAtlasSprite p_428509_
   ) {
      super(p_105773_, p_105774_, p_105775_, p_105776_, p_428509_);
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.xd = p_105777_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.yd = p_105778_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.zd = p_105779_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.yd += 0.002;
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.85F;
         this.yd *= 0.85F;
         this.zd *= 0.85F;
         if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.remove();
         }
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_105793_) {
         this.sprite = p_105793_;
      }

      public Particle createParticle(
         SimpleParticleType p_429804_,
         ClientLevel p_105796_,
         double p_105797_,
         double p_105798_,
         double p_105799_,
         double p_105800_,
         double p_105801_,
         double p_105802_,
         RandomSource p_431259_
      ) {
         return new BubbleParticle(p_105796_, p_105797_, p_105798_, p_105799_, p_105800_, p_105801_, p_105802_, this.sprite.get(p_431259_));
      }
   }
}
