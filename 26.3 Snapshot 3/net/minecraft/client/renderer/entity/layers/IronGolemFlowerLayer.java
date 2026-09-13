package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class IronGolemFlowerLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {
   public IronGolemFlowerLayer(final RenderLayerParent<IronGolemRenderState, IronGolemModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final IronGolemRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (!state.flowerBlock.isEmpty()) {
         poseStack.pushPose();
         ModelPart arm = this.getParentModel().getFlowerHoldingArm();
         arm.translateAndRotate(poseStack);
         poseStack.translate(-1.1875F, 1.0625F, -0.9375F);
         poseStack.translate(0.5F, 0.5F, 0.5F);
         float s = 0.5F;
         poseStack.scale(0.5F, 0.5F, 0.5F);
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.translate(-0.5F, -0.5F, -0.5F);
         state.flowerBlock.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }
}
