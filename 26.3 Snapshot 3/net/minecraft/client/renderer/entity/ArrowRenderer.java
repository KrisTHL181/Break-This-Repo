package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

public abstract class ArrowRenderer<T extends AbstractArrow, S extends ArrowRenderState> extends EntityRenderer<T, S> {
   private final ArrowModel model;

   public ArrowRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
   }

   public void submit(final S state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
      submitNodeCollector.submitModel(
         this.model, state, poseStack, this.getTextureLocation(state), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor
      );
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected abstract Identifier getTextureLocation(S state);

   public void extractRenderState(final T entity, final S state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.xRot = entity.getXRot(partialTicks);
      state.yRot = entity.getYRot(partialTicks);
      state.shake = entity.shakeTime - partialTicks;
   }
}
