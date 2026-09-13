package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class CrossedArmsItemLayer<S extends HoldingEntityRenderState, M extends EntityModel<S> & VillagerLikeModel<S>> extends RenderLayer<S, M> {
   public CrossedArmsItemLayer(final RenderLayerParent<S, M> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      ItemStackRenderState item = state.heldItem;
      if (!item.isEmpty()) {
         poseStack.pushPose();
         this.applyTranslation(state, poseStack);
         item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }

   protected void applyTranslation(final S state, final PoseStack poseStack) {
      this.getParentModel().translateToArms(state, poseStack);
      poseStack.mulPose(Axis.XP.rotation(0.75F));
      poseStack.scale(1.07F, 1.07F, 1.07F);
      poseStack.translate(0.0F, 0.13F, -0.34F);
      poseStack.mulPose(Axis.XP.rotation((float) Math.PI));
   }
}
