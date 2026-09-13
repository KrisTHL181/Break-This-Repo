package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.joml.Quaternionf;

public abstract class AbstractBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {
   protected final Identifier texture;

   public AbstractBoatRenderer(final EntityRendererProvider.Context context, final Identifier texture) {
      super(context);
      this.texture = texture;
      this.shadowRadius = 0.8F;
   }

   public void submit(final BoatRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.375F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
      float hurt = state.hurtTime;
      if (hurt > 0.0F) {
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
      }

      if (!state.isUnderWater && !Mth.equal(state.bubbleAngle, 0.0F)) {
         poseStack.mulPose(new Quaternionf().setAngleAxis(state.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
      }

      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      submitNodeCollector.submitModel(this.model(), state, poseStack, this.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      this.submitTypeAdditions(state, poseStack, submitNodeCollector, state.lightCoords);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected void submitTypeAdditions(
      final BoatRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords
   ) {
   }

   protected abstract EntityModel<BoatRenderState> model();

   public BoatRenderState createRenderState() {
      return new BoatRenderState();
   }

   public void extractRenderState(final AbstractBoat entity, final BoatRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.yRot = entity.getYRot(partialTicks);
      state.hurtTime = entity.getHurtTime() - partialTicks;
      state.hurtDir = entity.getHurtDir();
      state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
      state.bubbleAngle = entity.getBubbleAngle(partialTicks);
      state.isUnderWater = entity.isUnderWater();
      state.rowingTimeLeft = entity.getRowingTime(0, partialTicks);
      state.rowingTimeRight = entity.getRowingTime(1, partialTicks);
   }
}
