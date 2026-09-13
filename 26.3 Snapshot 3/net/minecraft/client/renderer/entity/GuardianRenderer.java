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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
   private static final Identifier GUARDIAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian.png");
   private static final Identifier GUARDIAN_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutout(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(final EntityRendererProvider.Context context) {
      this(context, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(final EntityRendererProvider.Context context, final float shadow, final ModelLayerLocation modelId) {
      super(context, new GuardianModel(context.bakeLayer(modelId)), shadow);
   }

   public boolean shouldRender(final Guardian entity, final Frustum culler, final double camX, final double camY, final double camZ) {
      if (super.shouldRender(entity, culler, camX, camY, camZ)) {
         return true;
      }

      if (entity.hasActiveAttackTarget()) {
         LivingEntity lookAtEntity = entity.getActiveAttackTarget();
         if (lookAtEntity != null) {
            Vec3 targetPos = this.getPosition(lookAtEntity, lookAtEntity.getBbHeight() * 0.5, 1.0F);
            Vec3 startPos = this.getPosition(entity, entity.getEyeHeight(), 1.0F);
            return culler.isVisible(new AABB(startPos.x, startPos.y, startPos.z, targetPos.x, targetPos.y, targetPos.z));
         }
      }

      return false;
   }

   private Vec3 getPosition(final LivingEntity entity, final double yOffset, final float partialTicks) {
      double sx = Mth.lerp(partialTicks, entity.xOld, entity.getX());
      double sy = Mth.lerp(partialTicks, entity.yOld, entity.getY()) + yOffset;
      double sz = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
      return new Vec3(sx, sy, sz);
   }

   public void submit(final GuardianRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      super.submit(state, poseStack, submitNodeCollector, camera);
      Vec3 targetPosition = state.attackTargetPosition;
      if (targetPosition != null) {
         float texVOff = state.attackTime * 0.5F % 1.0F;
         poseStack.pushPose();
         poseStack.translate(0.0F, state.eyeHeight, 0.0F);
         renderBeam(poseStack, submitNodeCollector, targetPosition.subtract(state.eyePosition), state.attackTime, state.attackScale, texVOff);
         poseStack.popPose();
      }
   }

   private static void renderBeam(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      Vec3 beamVector,
      final float timeInTicks,
      final float scale,
      final float texVOff
   ) {
      float length = (float)(beamVector.length() + 1.0);
      beamVector = beamVector.normalize();
      float xRot = (float)Math.acos(beamVector.y);
      float yRot = (float) (Math.PI / 2) - (float)Math.atan2(beamVector.z, beamVector.x);
      poseStack.mulPose(Axis.YP.rotationDegrees(yRot * (180.0F / (float)Math.PI)));
      poseStack.mulPose(Axis.XP.rotationDegrees(xRot * (180.0F / (float)Math.PI)));
      float rot = timeInTicks * 0.05F * -1.5F;
      float colorScale = scale * scale;
      int red = 64 + (int)(colorScale * 191.0F);
      int green = 32 + (int)(colorScale * 191.0F);
      int blue = 128 - (int)(colorScale * 64.0F);
      float rr1 = 0.2F;
      float rr2 = 0.282F;
      float wnx = Mth.cos(rot + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float wnz = Mth.sin(rot + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float enx = Mth.cos(rot + (float) (Math.PI / 4)) * 0.282F;
      float enz = Mth.sin(rot + (float) (Math.PI / 4)) * 0.282F;
      float wsx = Mth.cos(rot + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float wsz = Mth.sin(rot + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float esx = Mth.cos(rot + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float esz = Mth.sin(rot + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float wx = Mth.cos(rot + (float) Math.PI) * 0.2F;
      float wz = Mth.sin(rot + (float) Math.PI) * 0.2F;
      float ex = Mth.cos(rot + 0.0F) * 0.2F;
      float ez = Mth.sin(rot + 0.0F) * 0.2F;
      float nx = Mth.cos(rot + (float) (Math.PI / 2)) * 0.2F;
      float nz = Mth.sin(rot + (float) (Math.PI / 2)) * 0.2F;
      float sx = Mth.cos(rot + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float sz = Mth.sin(rot + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float top = length;
      float minU = 0.0F;
      float maxU = 0.4999F;
      float minV = -1.0F + texVOff;
      float maxV = minV + length * 2.5F;
      submitNodeCollector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (pose, buffer) -> {
         vertex(buffer, pose, wx, top, wz, red, green, blue, 0.4999F, maxV);
         vertex(buffer, pose, wx, 0.0F, wz, red, green, blue, 0.4999F, minV);
         vertex(buffer, pose, ex, 0.0F, ez, red, green, blue, 0.0F, minV);
         vertex(buffer, pose, ex, top, ez, red, green, blue, 0.0F, maxV);
         vertex(buffer, pose, nx, top, nz, red, green, blue, 0.4999F, maxV);
         vertex(buffer, pose, nx, 0.0F, nz, red, green, blue, 0.4999F, minV);
         vertex(buffer, pose, sx, 0.0F, sz, red, green, blue, 0.0F, minV);
         vertex(buffer, pose, sx, top, sz, red, green, blue, 0.0F, maxV);
         float vBase = Mth.floor(timeInTicks) % 2 == 0 ? 0.5F : 0.0F;
         vertex(buffer, pose, wnx, top, wnz, red, green, blue, 0.5F, vBase + 0.5F);
         vertex(buffer, pose, enx, top, enz, red, green, blue, 1.0F, vBase + 0.5F);
         vertex(buffer, pose, esx, top, esz, red, green, blue, 1.0F, vBase);
         vertex(buffer, pose, wsx, top, wsz, red, green, blue, 0.5F, vBase);
      });
   }

   private static void vertex(
      final VertexConsumer builder,
      final PoseStack.Pose pose,
      final float x,
      final float y,
      final float z,
      final int red,
      final int green,
      final int blue,
      final float u,
      final float v
   ) {
      builder.addVertex(pose, x, y, z)
         .setColor(red, green, blue, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public Identifier getTextureLocation(final GuardianRenderState state) {
      return GUARDIAN_LOCATION;
   }

   public GuardianRenderState createRenderState() {
      return new GuardianRenderState();
   }

   public void extractRenderState(final Guardian entity, final GuardianRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.spikesAnimation = entity.getSpikesAnimation(partialTicks);
      state.tailAnimation = entity.getTailAnimation(partialTicks);
      state.eyePosition = entity.getEyePosition(partialTicks);
      Entity lookAtEntity = getEntityToLookAt(entity);
      if (lookAtEntity != null) {
         state.lookDirection = entity.getViewVector(partialTicks);
         state.lookAtPosition = lookAtEntity.getEyePosition(partialTicks);
      } else {
         state.lookDirection = null;
         state.lookAtPosition = null;
      }

      LivingEntity targetEntity = entity.getActiveAttackTarget();
      if (targetEntity != null) {
         state.attackScale = entity.getAttackAnimationScale(partialTicks);
         state.attackTime = entity.getClientSideAttackTime() + partialTicks;
         state.attackTargetPosition = this.getPosition(targetEntity, targetEntity.getBbHeight() * 0.5, partialTicks);
      } else {
         state.attackTargetPosition = null;
      }
   }

   private static @Nullable Entity getEntityToLookAt(final Guardian entity) {
      Entity lookAtEntity = Minecraft.getInstance().getCameraEntity();
      return entity.hasActiveAttackTarget() ? entity.getActiveAttackTarget() : lookAtEntity;
   }
}
