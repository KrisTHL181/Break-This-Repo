package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
   private final Vector3f fromColor;
   private final Vector3f toColor;

   protected DustColorTransitionParticle(
      ClientLevel p_172053_,
      double p_172054_,
      double p_172055_,
      double p_172056_,
      double p_172057_,
      double p_172058_,
      double p_172059_,
      DustColorTransitionOptions p_172060_,
      SpriteSet p_172061_
   ) {
      super(p_172053_, p_172054_, p_172055_, p_172056_, p_172057_, p_172058_, p_172059_, p_172060_, p_172061_);
      float f = this.random.nextFloat() * 0.4F + 0.6F;
      this.fromColor = this.randomizeColor(p_172060_.getFromColor(), f);
      this.toColor = this.randomizeColor(p_172060_.getToColor(), f);
   }

   private Vector3f randomizeColor(Vector3f p_254318_, float p_254472_) {
      return new Vector3f(
         this.randomizeColor(p_254318_.x(), p_254472_), this.randomizeColor(p_254318_.y(), p_254472_), this.randomizeColor(p_254318_.z(), p_254472_)
      );
   }

   private void lerpColors(float p_172070_) {
      float f = (this.age + p_172070_) / (this.lifetime + 1.0F);
      Vector3f vector3f = new Vector3f(this.fromColor).lerp(this.toColor, f);
      this.rCol = vector3f.x();
      this.gCol = vector3f.y();
      this.bCol = vector3f.z();
   }

   @Override
   public void extract(QuadParticleRenderState p_425707_, Camera p_430312_, float p_426043_) {
      this.lerpColors(p_426043_);
      super.extract(p_425707_, p_430312_, p_426043_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_172073_) {
         this.sprites = p_172073_;
      }

      public Particle createParticle(
         DustColorTransitionOptions p_172075_,
         ClientLevel p_172076_,
         double p_172077_,
         double p_172078_,
         double p_172079_,
         double p_172080_,
         double p_172081_,
         double p_172082_,
         RandomSource p_431002_
      ) {
         return new DustColorTransitionParticle(p_172076_, p_172077_, p_172078_, p_172079_, p_172080_, p_172081_, p_172082_, p_172075_, this.sprites);
      }
   }
}
