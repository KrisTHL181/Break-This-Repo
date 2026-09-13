package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
   public PistonHeadRenderState createRenderState() {
      return new PistonHeadRenderState();
   }

   public void extractRenderState(
      final PistonMovingBlockEntity blockEntity,
      final PistonHeadRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.xOffset = blockEntity.getXOff(partialTicks);
      state.yOffset = blockEntity.getYOff(partialTicks);
      state.zOffset = blockEntity.getZOff(partialTicks);
      state.block = null;
      state.base = null;
      BlockState blockState = blockEntity.getMovedState();
      if (blockEntity.getLevel() instanceof ClientLevel level && !blockState.isAir()) {
         BlockPos pos = blockEntity.getBlockPos().relative(blockEntity.getMovementDirection().getOpposite());
         Holder<Biome> biome = level.getBiome(pos);
         if (blockState.is(Blocks.PISTON_HEAD) && blockEntity.getProgress(partialTicks) <= 4.0F) {
            blockState = blockState.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTicks) <= 0.5F);
            state.block = createMovingBlock(pos, blockState, biome, level);
         } else if (blockEntity.isSourcePiston() && !blockEntity.isExtending()) {
            PistonType value = blockState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState pistonHeadState = Blocks.PISTON_HEAD
               .defaultBlockState()
               .setValue(PistonHeadBlock.TYPE, value)
               .setValue(PistonHeadBlock.FACING, blockState.getValue(PistonBaseBlock.FACING));
            pistonHeadState = pistonHeadState.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTicks) >= 0.5F);
            state.block = createMovingBlock(pos, pistonHeadState, biome, level);
            BlockPos basePos = pos.relative(blockEntity.getMovementDirection());
            blockState = blockState.setValue(PistonBaseBlock.EXTENDED, true);
            state.base = createMovingBlock(basePos, blockState, biome, level);
         } else {
            state.block = createMovingBlock(pos, blockState, biome, level);
         }
      }
   }

   public void submit(
      final PistonHeadRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      if (state.block != null) {
         poseStack.pushPose();
         poseStack.translate(state.xOffset, state.yOffset, state.zOffset);
         submitNodeCollector.submitMovingBlock(poseStack, state.block, 0);
         poseStack.popPose();
         if (state.base != null) {
            submitNodeCollector.submitMovingBlock(poseStack, state.base, 0);
         }
      }
   }

   private static MovingBlockRenderState createMovingBlock(final BlockPos pos, final BlockState blockState, final Holder<Biome> biome, final ClientLevel level) {
      MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
      movingBlockRenderState.randomSeedPos = pos;
      movingBlockRenderState.blockPos = pos;
      movingBlockRenderState.blockState = blockState;
      movingBlockRenderState.biome = biome;
      movingBlockRenderState.cardinalLighting = level.cardinalLighting();
      movingBlockRenderState.lightEngine = level.getLightEngine();
      return movingBlockRenderState;
   }

   @Override
   public int getViewDistance() {
      return 68;
   }
}
