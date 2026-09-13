package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;

public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
   public MushroomCowMushroomLayer(final RenderLayerParent<MushroomCowRenderState, CowModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final MushroomCowRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (!state.isBaby && !state.mushroomModel.isEmpty()) {
         boolean appearsGlowingWithInvisibility = state.appearsGlowing() && state.isInvisible;
         if (!state.isInvisible || appearsGlowingWithInvisibility) {
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            poseStack.pushPose();
            poseStack.translate(0.2F, -0.35F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-48.0F));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(
               poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords
            );
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.2F, -0.35F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(42.0F));
            poseStack.translate(0.1F, 0.0F, -0.6F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-48.0F));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(
               poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords
            );
            poseStack.popPose();
            poseStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(poseStack);
            poseStack.translate(0.0F, -0.7F, -0.2F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-78.0F));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(
               poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords
            );
            poseStack.popPose();
         }
      }
   }

   private void submitMushroomBlock(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final boolean appearsGlowingWithInvisibility,
      final int outlineColor,
      final BlockModelRenderState mushroomModel,
      final int overlayCoords
   ) {
      if (appearsGlowingWithInvisibility) {
         mushroomModel.submitOnlyOutline(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      } else {
         mushroomModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      }
   }
}
