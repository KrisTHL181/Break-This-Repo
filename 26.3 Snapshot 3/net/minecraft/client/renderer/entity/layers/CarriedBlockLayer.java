package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class CarriedBlockLayer extends RenderLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {
   public CarriedBlockLayer(final RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final EndermanRenderState state,
      final float yRot,
      final float xRot
   ) {
      BlockModelRenderState carriedBlock = state.carriedBlock;
      if (!carriedBlock.isEmpty()) {
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.6875F, -0.75F);
         poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
         poseStack.translate(0.25F, 0.1875F, 0.25F);
         float s = 0.5F;
         poseStack.scale(-0.5F, -0.5F, 0.5F);
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         carriedBlock.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }
}
