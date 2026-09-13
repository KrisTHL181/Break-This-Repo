package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
   public PistonHeadRenderState createRenderState() {
      return new PistonHeadRenderState();
   }

   public void extractRenderState(
      PistonMovingBlockEntity p_427395_,
      PistonHeadRenderState p_423556_,
      float p_427126_,
      Vec3 p_424260_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_430028_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_427395_, p_423556_, p_427126_, p_424260_, p_430028_);
      p_423556_.xOffset = p_427395_.getXOff(p_427126_);
      p_423556_.yOffset = p_427395_.getYOff(p_427126_);
      p_423556_.zOffset = p_427395_.getZOff(p_427126_);
      p_423556_.block = null;
      p_423556_.base = null;
      BlockState blockstate = p_427395_.getMovedState();
      Level level = p_427395_.getLevel();
      if (level != null && !blockstate.isAir()) {
         BlockPos blockpos = p_427395_.getBlockPos().relative(p_427395_.getMovementDirection().getOpposite());
         Holder<Biome> holder = level.getBiome(blockpos);
         if (blockstate.is(Blocks.PISTON_HEAD) && p_427395_.getProgress(p_427126_) <= 4.0F) {
            blockstate = blockstate.setValue(PistonHeadBlock.SHORT, p_427395_.getProgress(p_427126_) <= 0.5F);
            p_423556_.block = createMovingBlock(blockpos, blockstate, holder, level);
         } else if (p_427395_.isSourcePiston() && !p_427395_.isExtending()) {
            PistonType pistontype = blockstate.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState blockstate1 = Blocks.PISTON_HEAD
               .defaultBlockState()
               .setValue(PistonHeadBlock.TYPE, pistontype)
               .setValue(PistonHeadBlock.FACING, blockstate.getValue(PistonBaseBlock.FACING));
            blockstate1 = blockstate1.setValue(PistonHeadBlock.SHORT, p_427395_.getProgress(p_427126_) >= 0.5F);
            p_423556_.block = createMovingBlock(blockpos, blockstate1, holder, level);
            BlockPos blockpos1 = blockpos.relative(p_427395_.getMovementDirection());
            blockstate = blockstate.setValue(PistonBaseBlock.EXTENDED, true);
            p_423556_.base = createMovingBlock(blockpos1, blockstate, holder, level);
         } else {
            p_423556_.block = createMovingBlock(blockpos, blockstate, holder, level);
         }
      }
   }

   public void submit(PistonHeadRenderState p_427310_, PoseStack p_424877_, SubmitNodeCollector p_423615_, CameraRenderState p_424899_) {
      if (p_427310_.block != null) {
         p_424877_.pushPose();
         p_424877_.translate(p_427310_.xOffset, p_427310_.yOffset, p_427310_.zOffset);
         p_423615_.submitMovingBlock(p_424877_, p_427310_.block);
         p_424877_.popPose();
         if (p_427310_.base != null) {
            p_423615_.submitMovingBlock(p_424877_, p_427310_.base);
         }
      }
   }

   private static MovingBlockRenderState createMovingBlock(BlockPos p_424991_, BlockState p_427265_, Holder<Biome> p_423702_, Level p_426655_) {
      MovingBlockRenderState movingblockrenderstate = new MovingBlockRenderState();
      movingblockrenderstate.randomSeedPos = p_424991_;
      movingblockrenderstate.blockPos = p_424991_;
      movingblockrenderstate.blockState = p_427265_;
      movingblockrenderstate.biome = p_423702_;
      movingblockrenderstate.level = p_426655_;
      return movingblockrenderstate;
   }

   @Override
   public int getViewDistance() {
      return 68;
   }
}
