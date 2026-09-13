package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseTorchBlock extends Block {
   private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 10.0);

   protected BaseTorchBlock(BlockBehaviour.Properties p_310835_) {
      super(p_310835_);
   }

   @Override
   protected abstract MapCodec<? extends BaseTorchBlock> codec();

   @Override
   protected VoxelShape getShape(BlockState p_310927_, BlockGetter p_313227_, BlockPos p_311676_, CollisionContext p_310238_) {
      return SHAPE;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_311008_,
      LevelReader p_368861_,
      ScheduledTickAccess p_362825_,
      BlockPos p_313113_,
      Direction p_310059_,
      BlockPos p_312310_,
      BlockState p_312601_,
      RandomSource p_367825_
   ) {
      return p_310059_ == Direction.DOWN && !this.canSurvive(p_311008_, p_368861_, p_313113_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_311008_, p_368861_, p_362825_, p_313113_, p_310059_, p_312310_, p_312601_, p_367825_);
   }

   @Override
   protected boolean canSurvive(BlockState p_309766_, LevelReader p_313035_, BlockPos p_311995_) {
      return canSupportCenter(p_313035_, p_311995_.below(), Direction.UP);
   }
}
