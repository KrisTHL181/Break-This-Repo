package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.guardian.GuardianModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
   private static final Identifier GUARDIAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian.png");
   private static final Identifier GUARDIAN_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(EntityRendererProvider.Context p_174159_) {
      this(p_174159_, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(EntityRendererProvider.Context p_174161_, float p_174162_, ModelLayerLocation p_174163_) {
      super(p_174161_, new GuardianModel(p_174161_.bakeLayer(p_174163_)), p_174162_);
   }

   public boolean shouldRender(Guardian p_114836_, Frustum p_114837_, double p_114838_, double p_114839_, double p_114840_) {
      if (super.shouldRender(p_114836_, p_114837_, p_114838_, p_114839_, p_114840_)) {
         return true;
      }

      if (p_114836_.hasActiveAttackTarget()) {
         LivingEntity livingentity = p_114836_.getActiveAttackTarget();
         if (livingentity != null) {
            Vec3 vec3 = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, 1.0F);
            Vec3 vec31 = this.getPosition(p_114836_, p_114836_.getEyeHeight(), 1.0F);
            return p_114837_.isVisible(new AABB(vec31.x, vec31.y, vec31.z, vec3.x, vec3.y, vec3.z));
         }
      }

      return false;
   }

   private Vec3 getPosition(LivingEntity p_114803_, double p_114804_, float p_114805_) {
      double d0 = Mth.lerp(p_114805_, p_114803_.xOld, p_114803_.getX());
      double d1 = Mth.lerp(p_114805_, p_114803_.yOld, p_114803_.getY()) + p_114804_;
      double d2 = Mth.lerp(p_114805_, p_114803_.zOld, p_114803_.getZ());
      return new Vec3(d0, d1, d2);
   }

   public void submit(GuardianRenderState p_424517_, PoseStack p_426620_, SubmitNodeCollector p_430505_, CameraRenderState p_424293_) {
      super.submit(p_424517_, p_426620_, p_430505_, p_424293_);
      Vec3 vec3 = p_424517_.attackTargetPosition;
      if (vec3 != null) {
         float f = p_424517_.attackTime * 0.5F % 1.0F;
         p_426620_.pushPose();
         p_426620_.translate(0.0F, p_424517_.eyeHeight, 0.0F);
         renderBeam(p_426620_, p_430505_, vec3.subtract(p_424517_.eyePosition), p_424517_.attackTime, p_424517_.attackScale, f);
         p_426620_.popPose();
      }
   }

   private static void renderBeam(PoseStack p_362984_, SubmitNodeCollector p_426389_, Vec3 p_364612_, float p_368702_, float p_364900_, float p_363883_) {
      float f = (float)(p_364612_.length() + 1.0);
      p_364612_ = p_364612_.normalize();
      float f1 = (float)Math.acos(p_364612_.y);
      float f2 = (float) (Math.PI / 2) - (float)Math.atan2(p_364612_.z, p_364612_.x);
      p_362984_.mulPose(Axis.YP.rotationDegrees(f2 * (180.0F / (float)Math.PI)));
      p_362984_.mulPose(Axis.XP.rotationDegrees(f1 * (180.0F / (float)Math.PI)));
      float f3 = p_368702_ * 0.05F * -1.5F;
      float f4 = p_364900_ * p_364900_;
      int i = 64 + (int)(f4 * 191.0F);
      int j = 32 + (int)(f4 * 191.0F);
      int k = 128 - (int)(f4 * 64.0F);
      float f5 = 0.2F;
      float f6 = 0.282F;
      float f7 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float f8 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float f9 = Mth.cos(f3 + (float) (Math.PI / 4)) * 0.282F;
      float f10 = Mth.sin(f3 + (float) (Math.PI / 4)) * 0.282F;
      float f11 = Mth.cos(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float f12 = Mth.sin(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float f13 = Mth.cos(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float f14 = Mth.sin(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float f15 = Mth.cos(f3 + (float) Math.PI) * 0.2F;
      float f16 = Mth.sin(f3 + (float) Math.PI) * 0.2F;
      float f17 = Mth.cos(f3 + 0.0F) * 0.2F;
      float f18 = Mth.sin(f3 + 0.0F) * 0.2F;
      float f19 = Mth.cos(f3 + (float) (Math.PI / 2)) * 0.2F;
      float f20 = Mth.sin(f3 + (float) (Math.PI / 2)) * 0.2F;
      float f21 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float f22 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float f23 = 0.0F;
      float f24 = 0.4999F;
      float f25 = -1.0F + p_363883_;
      float f26 = f25 + f * 2.5F;
      p_426389_.submitCustomGeometry(p_362984_, BEAM_RENDER_TYPE, (p_430440_, p_424942_) -> {
         vertex(p_424942_, p_430440_, f15, f, f16, i, j, k, 0.4999F, f26);
         vertex(p_424942_, p_430440_, f15, 0.0F, f16, i, j, k, 0.4999F, f25);
         vertex(p_424942_, p_430440_, f17, 0.0F, f18, i, j, k, 0.0F, f25);
         vertex(p_424942_, p_430440_, f17, f, f18, i, j, k, 0.0F, f26);
         vertex(p_424942_, p_430440_, f19, f, f20, i, j, k, 0.4999F, f26);
         vertex(p_424942_, p_430440_, f19, 0.0F, f20, i, j, k, 0.4999F, f25);
         vertex(p_424942_, p_430440_, f21, 0.0F, f22, i, j, k, 0.0F, f25);
         vertex(p_424942_, p_430440_, f21, f, f22, i, j, k, 0.0F, f26);
         float f27 = Mth.floor(p_368702_) % 2 == 0 ? 0.5F : 0.0F;
         vertex(p_424942_, p_430440_, f7, f, f8, i, j, k, 0.5F, f27 + 0.5F);
         vertex(p_424942_, p_430440_, f9, f, f10, i, j, k, 1.0F, f27 + 0.5F);
         vertex(p_424942_, p_430440_, f13, f, f14, i, j, k, 1.0F, f27);
         vertex(p_424942_, p_430440_, f11, f, f12, i, j, k, 0.5F, f27);
      });
   }

   private static void vertex(
      VertexConsumer p_253637_,
      PoseStack.Pose p_334069_,
      float p_253994_,
      float p_254492_,
      float p_254474_,
      int p_254080_,
      int p_253655_,
      int p_254133_,
      float p_254233_,
      float p_253939_
   ) {
      p_253637_.addVertex(p_334069_, p_253994_, p_254492_, p_254474_)
         .setColor(p_254080_, p_253655_, p_254133_, 255)
         .setUv(p_254233_, p_253939_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(p_334069_, 0.0F, 1.0F, 0.0F);
   }

   public Identifier getTextureLocation(GuardianRenderState p_456710_) {
      return GUARDIAN_LOCATION;
   }

   public GuardianRenderState createRenderState() {
      return new GuardianRenderState();
   }

   public void extractRenderState(Guardian p_365802_, GuardianRenderState p_365304_, float p_367592_) {
      super.extractRenderState(p_365802_, p_365304_, p_367592_);
      p_365304_.spikesAnimation = p_365802_.getSpikesAnimation(p_367592_);
      p_365304_.tailAnimation = p_365802_.getTailAnimation(p_367592_);
      p_365304_.eyePosition = p_365802_.getEyePosition(p_367592_);
      Entity entity = getEntityToLookAt(p_365802_);
      if (entity != null) {
         p_365304_.lookDirection = p_365802_.getViewVector(p_367592_);
         p_365304_.lookAtPosition = entity.getEyePosition(p_367592_);
      } else {
         p_365304_.lookDirection = null;
         p_365304_.lookAtPosition = null;
      }

      LivingEntity livingentity = p_365802_.getActiveAttackTarget();
      if (livingentity != null) {
         p_365304_.attackScale = p_365802_.getAttackAnimationScale(p_367592_);
         p_365304_.attackTime = p_365802_.getClientSideAttackTime() + p_367592_;
         p_365304_.attackTargetPosition = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, p_367592_);
      } else {
         p_365304_.attackTargetPosition = null;
      }
   }

   private static @Nullable Entity getEntityToLookAt(Guardian p_369397_) {
      Entity entity = Minecraft.getInstance().getCameraEntity();
      return p_369397_.hasActiveAttackTarget() ? p_369397_.getActiveAttackTarget() : entity;
   }
}
