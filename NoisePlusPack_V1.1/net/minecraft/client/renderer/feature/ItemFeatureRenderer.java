package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public void render(SubmitNodeCollection p_429535_, MultiBufferSource.BufferSource p_428443_, OutlineBufferSource p_423238_) {
      for (SubmitNodeStorage.ItemSubmit submitnodestorage$itemsubmit : p_429535_.getItemSubmits()) {
         this.poseStack.pushPose();
         this.poseStack.last().set(submitnodestorage$itemsubmit.pose());
         ItemRenderer.renderItem(
            submitnodestorage$itemsubmit.displayContext(),
            this.poseStack,
            p_428443_,
            submitnodestorage$itemsubmit.lightCoords(),
            submitnodestorage$itemsubmit.overlayCoords(),
            submitnodestorage$itemsubmit.tintLayers(),
            submitnodestorage$itemsubmit.quads(),
            submitnodestorage$itemsubmit.renderType(),
            submitnodestorage$itemsubmit.foilType()
         );
         if (submitnodestorage$itemsubmit.outlineColor() != 0) {
            p_423238_.setColor(submitnodestorage$itemsubmit.outlineColor());
            ItemRenderer.renderItem(
               submitnodestorage$itemsubmit.displayContext(),
               this.poseStack,
               p_423238_,
               submitnodestorage$itemsubmit.lightCoords(),
               submitnodestorage$itemsubmit.overlayCoords(),
               submitnodestorage$itemsubmit.tintLayers(),
               submitnodestorage$itemsubmit.quads(),
               submitnodestorage$itemsubmit.renderType(),
               ItemStackRenderState.FoilType.NONE
            );
         }

         this.poseStack.popPose();
      }
   }
}
