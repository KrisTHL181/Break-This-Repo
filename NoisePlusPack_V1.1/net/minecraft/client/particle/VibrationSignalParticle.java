package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class VibrationSignalParticle extends SingleQuadParticle {
   private final PositionSource target;
   private float rot;
   private float rotO;
   private float pitch;
   private float pitchO;

   VibrationSignalParticle(
      ClientLevel p_234105_, double p_234106_, double p_234107_, double p_234108_, PositionSource p_234109_, int p_234110_, TextureAtlasSprite p_429603_
   ) {
      super(p_234105_, p_234106_, p_234107_, p_234108_, 0.0, 0.0, 0.0, p_429603_);
      this.quadSize = 0.3F;
      this.target = p_234109_;
      this.lifetime = p_234110_;
      Optional<Vec3> optional = p_234109_.getPosition(p_234105_);
      if (optional.isPresent()) {
         Vec3 vec3 = optional.get();
         double d0 = p_234106_ - vec3.x();
         double d1 = p_234107_ - vec3.y();
         double d2 = p_234108_ - vec3.z();
         this.rotO = this.rot = (float)Mth.atan2(d0, d2);
         this.pitchO = this.pitch = (float)Mth.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2));
      }
   }

   @Override
   public void extract(QuadParticleRenderState p_431093_, Camera p_172476_, float p_172477_) {
      float f = Mth.sin((this.age + p_172477_ - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
      float f1 = Mth.lerp(p_172477_, this.rotO, this.rot);
      float f2 = Mth.lerp(p_172477_, this.pitchO, this.pitch) + (float) (Math.PI / 2);
      Quaternionf quaternionf = new Quaternionf();
      quaternionf.rotationY(f1).rotateX(-f2).rotateY(f);
      this.extractRotatedQuad(p_431093_, p_172476_, quaternionf, p_172477_);
      quaternionf.rotationY((float) -Math.PI + f1).rotateX(f2).rotateY(f);
      this.extractRotatedQuad(p_431093_, p_172476_, quaternionf, p_172477_);
   }

   @Override
   public int getLightColor(float p_172469_) {
      return 240;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         Optional<Vec3> optional = this.target.getPosition(this.level);
         if (optional.isEmpty()) {
            this.remove();
         } else {
            int i = this.lifetime - this.age;
            double d0 = 1.0 / i;
            Vec3 vec3 = optional.get();
            this.x = Mth.lerp(d0, this.x, vec3.x());
            this.y = Mth.lerp(d0, this.y, vec3.y());
            this.z = Mth.lerp(d0, this.z, vec3.z());
            double d1 = this.x - vec3.x();
            double d2 = this.y - vec3.y();
            double d3 = this.z - vec3.z();
            this.rotO = this.rot;
            this.rot = (float)Mth.atan2(d1, d3);
            this.pitchO = this.pitch;
            this.pitch = (float)Mth.atan2(d2, Math.sqrt(d1 * d1 + d3 * d3));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<VibrationParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_172490_) {
         this.sprite = p_172490_;
      }

      public Particle createParticle(
         VibrationParticleOption p_427696_,
         ClientLevel p_172493_,
         double p_172494_,
         double p_172495_,
         double p_172496_,
         double p_172497_,
         double p_172498_,
         double p_172499_,
         RandomSource p_424684_
      ) {
         VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(
            p_172493_, p_172494_, p_172495_, p_172496_, p_427696_.getDestination(), p_427696_.getArrivalInTicks(), this.sprite.get(p_424684_)
         );
         vibrationsignalparticle.setAlpha(1.0F);
         return vibrationsignalparticle;
      }
   }
}
