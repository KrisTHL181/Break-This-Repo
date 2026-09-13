package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class FoxHeldItemLayer extends RenderLayer<FoxRenderState, FoxModel> {
   public FoxHeldItemLayer(final RenderLayerParent<FoxRenderState, FoxModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final FoxRenderState state,
      final float yRot,
      final float xRot
   ) {
      ItemStackRenderState item = state.heldItem;
      if (!item.isEmpty()) {
         boolean sleeping = state.isSleeping;
         boolean isBaby = state.isBaby;
         poseStack.pushPose();
         poseStack.translate(this.getParentModel().head.x / 16.0F, this.getParentModel().head.y / 16.0F, this.getParentModel().head.z / 16.0F);
         if (isBaby) {
            float hs = 0.75F;
            poseStack.scale(0.75F, 0.75F, 0.75F);
         }

         poseStack.mulPose(Axis.ZP.rotation(state.headRollAngle));
         poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
         poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
         if (state.isBaby) {
            if (sleeping) {
               poseStack.translate(0.4F, 0.26F, 0.15F);
            } else {
               poseStack.translate(0.06F, 0.26F, -0.5F);
            }
         } else if (sleeping) {
            poseStack.translate(0.46F, 0.26F, 0.22F);
         } else {
            poseStack.translate(0.06F, 0.27F, -0.5F);
         }

         poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         if (sleeping) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
         }

         item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }
}
