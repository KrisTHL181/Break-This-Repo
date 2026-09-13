package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;

public class SnowGolemHeadLayer extends RenderLayer<SnowGolemRenderState, SnowGolemModel> {
   public SnowGolemHeadLayer(final RenderLayerParent<SnowGolemRenderState, SnowGolemModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final SnowGolemRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (!state.headBlock.isEmpty()) {
         if (!state.isInvisible || state.appearsGlowing()) {
            poseStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(poseStack);
            float s = 0.625F;
            poseStack.translate(0.0F, -0.34375F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(0.625F, -0.625F, -0.625F);
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            if (state.appearsGlowing() && state.isInvisible) {
               state.headBlock.submitOnlyOutline(poseStack, submitNodeCollector, lightCoords, overlayCoords, state.outlineColor);
            } else {
               state.headBlock.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, state.outlineColor);
            }

            poseStack.popPose();
         }
      }
   }
}
