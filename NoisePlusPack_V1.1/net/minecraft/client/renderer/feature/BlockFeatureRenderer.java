package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public void render(SubmitNodeCollection p_426810_, MultiBufferSource.BufferSource p_425259_, BlockRenderDispatcher p_429476_, OutlineBufferSource p_424699_) {
      for (SubmitNodeStorage.MovingBlockSubmit submitnodestorage$movingblocksubmit : p_426810_.getMovingBlockSubmits()) {
         MovingBlockRenderState movingblockrenderstate = submitnodestorage$movingblocksubmit.movingBlockRenderState();
         BlockState blockstate = movingblockrenderstate.blockState;
         List<BlockModelPart> list = p_429476_.getBlockModel(blockstate)
            .collectParts(RandomSource.create(blockstate.getSeed(movingblockrenderstate.randomSeedPos)));
         PoseStack posestack = new PoseStack();
         posestack.mulPose(submitnodestorage$movingblocksubmit.pose());
         p_429476_.getModelRenderer()
            .tesselateBlock(
               movingblockrenderstate,
               list,
               blockstate,
               movingblockrenderstate.blockPos,
               posestack,
               p_425259_.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockstate)),
               false,
               OverlayTexture.NO_OVERLAY
            );
      }

      for (SubmitNodeStorage.BlockSubmit submitnodestorage$blocksubmit : p_426810_.getBlockSubmits()) {
         this.poseStack.pushPose();
         this.poseStack.last().set(submitnodestorage$blocksubmit.pose());
         p_429476_.renderSingleBlock(
            submitnodestorage$blocksubmit.state(),
            this.poseStack,
            p_425259_,
            submitnodestorage$blocksubmit.lightCoords(),
            submitnodestorage$blocksubmit.overlayCoords()
         );
         if (submitnodestorage$blocksubmit.outlineColor() != 0) {
            p_424699_.setColor(submitnodestorage$blocksubmit.outlineColor());
            p_429476_.renderSingleBlock(
               submitnodestorage$blocksubmit.state(),
               this.poseStack,
               p_424699_,
               submitnodestorage$blocksubmit.lightCoords(),
               submitnodestorage$blocksubmit.overlayCoords()
            );
         }

         this.poseStack.popPose();
      }

      for (SubmitNodeStorage.BlockModelSubmit submitnodestorage$blockmodelsubmit : p_426810_.getBlockModelSubmits()) {
         ModelBlockRenderer.renderModel(
            submitnodestorage$blockmodelsubmit.pose(),
            p_425259_.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
            submitnodestorage$blockmodelsubmit.model(),
            submitnodestorage$blockmodelsubmit.r(),
            submitnodestorage$blockmodelsubmit.g(),
            submitnodestorage$blockmodelsubmit.b(),
            submitnodestorage$blockmodelsubmit.lightCoords(),
            submitnodestorage$blockmodelsubmit.overlayCoords()
         );
         if (submitnodestorage$blockmodelsubmit.outlineColor() != 0) {
            p_424699_.setColor(submitnodestorage$blockmodelsubmit.outlineColor());
            ModelBlockRenderer.renderModel(
               submitnodestorage$blockmodelsubmit.pose(),
               p_424699_.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
               submitnodestorage$blockmodelsubmit.model(),
               submitnodestorage$blockmodelsubmit.r(),
               submitnodestorage$blockmodelsubmit.g(),
               submitnodestorage$blockmodelsubmit.b(),
               submitnodestorage$blockmodelsubmit.lightCoords(),
               submitnodestorage$blockmodelsubmit.overlayCoords()
            );
         }
      }
   }
}
