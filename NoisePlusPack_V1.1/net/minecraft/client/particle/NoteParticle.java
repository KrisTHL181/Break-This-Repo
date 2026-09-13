package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoteParticle extends SingleQuadParticle {
   NoteParticle(ClientLevel p_107167_, double p_107168_, double p_107169_, double p_107170_, double p_107171_, TextureAtlasSprite p_422471_) {
      super(p_107167_, p_107168_, p_107169_, p_107170_, 0.0, 0.0, 0.0, p_422471_);
      this.friction = 0.66F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.xd *= 0.01F;
      this.yd *= 0.01F;
      this.zd *= 0.01F;
      this.yd += 0.2;
      this.rCol = Math.max(0.0F, Mth.sin(((float)p_107171_ + 0.0F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.gCol = Math.max(0.0F, Mth.sin(((float)p_107171_ + 0.33333334F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.bCol = Math.max(0.0F, Mth.sin(((float)p_107171_ + 0.6666667F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.quadSize *= 1.5F;
      this.lifetime = 6;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public float getQuadSize(float p_107182_) {
      return this.quadSize * Mth.clamp((this.age + p_107182_) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_107185_) {
         this.sprite = p_107185_;
      }

      public Particle createParticle(
         SimpleParticleType p_107196_,
         ClientLevel p_107197_,
         double p_107198_,
         double p_107199_,
         double p_107200_,
         double p_107201_,
         double p_107202_,
         double p_107203_,
         RandomSource p_423206_
      ) {
         return new NoteParticle(p_107197_, p_107198_, p_107199_, p_107200_, p_107201_, this.sprite.get(p_423206_));
      }
   }
}
