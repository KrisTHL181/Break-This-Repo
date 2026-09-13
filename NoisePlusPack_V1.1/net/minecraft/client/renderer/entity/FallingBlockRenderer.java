package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingBlockRenderer extends EntityRenderer<FallingBlockEntity, FallingBlockRenderState> {
   public FallingBlockRenderer(EntityRendererProvider.Context p_174112_) {
      super(p_174112_);
      this.shadowRadius = 0.5F;
   }

   public boolean shouldRender(FallingBlockEntity p_367111_, Frustum p_361639_, double p_368114_, double p_367640_, double p_370068_) {
      return !super.shouldRender(p_367111_, p_361639_, p_368114_, p_367640_, p_370068_)
         ? false
         : p_367111_.getBlockState() != p_367111_.level().getBlockState(p_367111_.blockPosition());
   }

   public void submit(FallingBlockRenderState p_423866_, PoseStack p_423331_, SubmitNodeCollector p_425379_, CameraRenderState p_424752_) {
      BlockState blockstate = p_423866_.movingBlockRenderState.blockState;
      if (blockstate.getRenderShape() == RenderShape.MODEL) {
         p_423331_.pushPose();
         p_423331_.translate(-0.5, 0.0, -0.5);
         p_425379_.submitMovingBlock(p_423331_, p_423866_.movingBlockRenderState);
         p_423331_.popPose();
         super.submit(p_423866_, p_423331_, p_425379_, p_424752_);
      }
   }

   public FallingBlockRenderState createRenderState() {
      return new FallingBlockRenderState();
   }

   public void extractRenderState(FallingBlockEntity p_364466_, FallingBlockRenderState p_362649_, float p_366753_) {
      super.extractRenderState(p_364466_, p_362649_, p_366753_);
      BlockPos blockpos = BlockPos.containing(p_364466_.getX(), p_364466_.getBoundingBox().maxY, p_364466_.getZ());
      p_362649_.movingBlockRenderState.randomSeedPos = p_364466_.getStartPos();
      p_362649_.movingBlockRenderState.blockPos = blockpos;
      p_362649_.movingBlockRenderState.blockState = p_364466_.getBlockState();
      p_362649_.movingBlockRenderState.biome = p_364466_.level().getBiome(blockpos);
      p_362649_.movingBlockRenderState.level = p_364466_.level();
   }
}
