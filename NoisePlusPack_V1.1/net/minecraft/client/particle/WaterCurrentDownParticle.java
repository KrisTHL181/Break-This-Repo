package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterCurrentDownParticle extends SingleQuadParticle {
   private float angle;

   WaterCurrentDownParticle(ClientLevel p_108450_, double p_108451_, double p_108452_, double p_108453_, TextureAtlasSprite p_428028_) {
      super(p_108450_, p_108451_, p_108452_, p_108453_, p_428028_);
      this.lifetime = (int)(this.random.nextFloat() * 60.0F) + 30;
      this.hasPhysics = false;
      this.xd = 0.0;
      this.yd = -0.05;
      this.zd = 0.0;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.gravity = 0.002F;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = 0.6F;
         this.xd = this.xd + 0.6F * Mth.cos(this.angle);
         this.zd = this.zd + 0.6F * Mth.sin(this.angle);
         this.xd *= 0.07;
         this.zd *= 0.07;
         this.move(this.xd, this.yd, this.zd);
         if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER) || this.onGround) {
            this.remove();
         }

         this.angle += 0.08F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_108464_) {
         this.sprite = p_108464_;
      }

      public Particle createParticle(
         SimpleParticleType p_425201_,
         ClientLevel p_108467_,
         double p_108468_,
         double p_108469_,
         double p_108470_,
         double p_108471_,
         double p_108472_,
         double p_108473_,
         RandomSource p_427002_
      ) {
         return new WaterCurrentDownParticle(p_108467_, p_108468_, p_108469_, p_108470_, this.sprite.get(p_427002_));
      }
   }
}
