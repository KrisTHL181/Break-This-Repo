package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class PandaHoldsItemLayer extends RenderLayer<PandaRenderState, PandaModel> {
   public PandaHoldsItemLayer(final RenderLayerParent<PandaRenderState, PandaModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final PandaRenderState state,
      final float yRot,
      final float xRot
   ) {
      ItemStackRenderState item = state.heldItem;
      if (!item.isEmpty() && state.isSitting && !state.isScared) {
         float z = -0.6F;
         float y = 1.4F;
         if (state.isEating) {
            z -= 0.2F * Mth.sin(state.ageInTicks * 0.6F) + 0.2F;
            y -= 0.09F * Mth.sin(state.ageInTicks * 0.6F);
         }

         poseStack.pushPose();
         poseStack.translate(0.1F, y, z);
         item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }
}
