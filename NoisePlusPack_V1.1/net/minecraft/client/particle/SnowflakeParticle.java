package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowflakeParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected SnowflakeParticle(
      ClientLevel p_172292_, double p_172293_, double p_172294_, double p_172295_, double p_172296_, double p_172297_, double p_172298_, SpriteSet p_172299_
   ) {
      super(p_172292_, p_172293_, p_172294_, p_172295_, p_172299_.first());
      this.gravity = 0.225F;
      this.friction = 1.0F;
      this.sprites = p_172299_;
      this.xd = p_172296_ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      this.yd = p_172297_ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      this.zd = p_172298_ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      this.quadSize = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 1.0F + 1.0F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 2;
      this.setSpriteFromAge(p_172299_);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      this.xd *= 0.95F;
      this.yd *= 0.9F;
      this.zd *= 0.95F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_172304_) {
         this.sprites = p_172304_;
      }

      public Particle createParticle(
         SimpleParticleType p_172315_,
         ClientLevel p_172316_,
         double p_172317_,
         double p_172318_,
         double p_172319_,
         double p_172320_,
         double p_172321_,
         double p_172322_,
         RandomSource p_429589_
      ) {
         SnowflakeParticle snowflakeparticle = new SnowflakeParticle(p_172316_, p_172317_, p_172318_, p_172319_, p_172320_, p_172321_, p_172322_, this.sprites);
         snowflakeparticle.setColor(0.923F, 0.964F, 0.999F);
         return snowflakeparticle;
      }
   }
}
