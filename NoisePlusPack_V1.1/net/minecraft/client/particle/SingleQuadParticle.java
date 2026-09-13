package net.minecraft.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class SingleQuadParticle extends Particle {
   protected float quadSize;
   protected float rCol = 1.0F;
   protected float gCol = 1.0F;
   protected float bCol = 1.0F;
   protected float alpha = 1.0F;
   protected float roll;
   protected float oRoll;
   protected TextureAtlasSprite sprite;

   protected SingleQuadParticle(ClientLevel p_107670_, double p_107671_, double p_107672_, double p_107673_, TextureAtlasSprite p_424099_) {
      super(p_107670_, p_107671_, p_107672_, p_107673_);
      this.sprite = p_424099_;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   protected SingleQuadParticle(
      ClientLevel p_107665_,
      double p_107666_,
      double p_107667_,
      double p_107668_,
      double p_424112_,
      double p_423135_,
      double p_430356_,
      TextureAtlasSprite p_422668_
   ) {
      super(p_107665_, p_107666_, p_107667_, p_107668_, p_424112_, p_423135_, p_430356_);
      this.sprite = p_422668_;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
   }

   public void extract(QuadParticleRenderState p_425034_, Camera p_422318_, float p_428246_) {
      Quaternionf quaternionf = new Quaternionf();
      this.getFacingCameraMode().setRotation(quaternionf, p_422318_, p_428246_);
      if (this.roll != 0.0F) {
         quaternionf.rotateZ(Mth.lerp(p_428246_, this.oRoll, this.roll));
      }

      this.extractRotatedQuad(p_425034_, p_422318_, quaternionf, p_428246_);
   }

   protected void extractRotatedQuad(QuadParticleRenderState p_426925_, Camera p_344083_, Quaternionf p_342719_, float p_343457_) {
      Vec3 vec3 = p_344083_.position();
      float f = (float)(Mth.lerp(p_343457_, this.xo, this.x) - vec3.x());
      float f1 = (float)(Mth.lerp(p_343457_, this.yo, this.y) - vec3.y());
      float f2 = (float)(Mth.lerp(p_343457_, this.zo, this.z) - vec3.z());
      this.extractRotatedQuad(p_426925_, p_342719_, f, f1, f2, p_343457_);
   }

   protected void extractRotatedQuad(
      QuadParticleRenderState p_428884_, Quaternionf p_428365_, float p_422699_, float p_425778_, float p_424046_, float p_429033_
   ) {
      p_428884_.add(
         this.getLayer(),
         p_422699_,
         p_425778_,
         p_424046_,
         p_428365_.x,
         p_428365_.y,
         p_428365_.z,
         p_428365_.w,
         this.getQuadSize(p_429033_),
         this.getU0(),
         this.getU1(),
         this.getV0(),
         this.getV1(),
         ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
         this.getLightColor(p_429033_)
      );
   }

   public float getQuadSize(float p_107681_) {
      return this.quadSize;
   }

   @Override
   public Particle scale(float p_107683_) {
      this.quadSize *= p_107683_;
      return super.scale(p_107683_);
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.SINGLE_QUADS;
   }

   public void setSpriteFromAge(SpriteSet p_423027_) {
      if (!this.removed) {
         this.setSprite(p_423027_.get(this.age, this.lifetime));
      }
   }

   protected void setSprite(TextureAtlasSprite p_428553_) {
      this.sprite = p_428553_;
   }

   protected float getU0() {
      return this.sprite.getU0();
   }

   protected float getU1() {
      return this.sprite.getU1();
   }

   protected float getV0() {
      return this.sprite.getV0();
   }

   protected float getV1() {
      return this.sprite.getV1();
   }

   protected abstract SingleQuadParticle.Layer getLayer();

   public void setColor(float p_430895_, float p_431632_, float p_430848_) {
      this.rCol = p_430895_;
      this.gCol = p_431632_;
      this.bCol = p_430848_;
   }

   protected void setAlpha(float p_422903_) {
      this.alpha = p_422903_;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName()
         + ", Pos ("
         + this.x
         + ","
         + this.y
         + ","
         + this.z
         + "), RGBA ("
         + this.rCol
         + ","
         + this.gCol
         + ","
         + this.bCol
         + ","
         + this.alpha
         + "), Age "
         + this.age;
   }

   @OnlyIn(Dist.CLIENT)
   public interface FacingCameraMode {
      SingleQuadParticle.FacingCameraMode LOOKAT_XYZ = (p_312026_, p_311956_, p_310043_) -> p_312026_.set(p_311956_.rotation());
      SingleQuadParticle.FacingCameraMode LOOKAT_Y = (p_310770_, p_309904_, p_311153_) -> p_310770_.set(
         0.0F, p_309904_.rotation().y, 0.0F, p_309904_.rotation().w
      );

      void setRotation(Quaternionf var1, Camera var2, float var3);
   }

   @OnlyIn(Dist.CLIENT)
   public record Layer(boolean translucent, Identifier textureAtlasLocation, RenderPipeline pipeline) {
      public static final SingleQuadParticle.Layer TERRAIN = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE
      );
      public static final SingleQuadParticle.Layer ITEMS = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_ITEMS, RenderPipelines.TRANSLUCENT_PARTICLE);
      public static final SingleQuadParticle.Layer OPAQUE = new SingleQuadParticle.Layer(
         false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE
      );
      public static final SingleQuadParticle.Layer TRANSLUCENT = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE
      );
   }
}
