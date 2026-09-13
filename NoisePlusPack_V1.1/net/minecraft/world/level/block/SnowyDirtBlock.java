package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SnowyDirtBlock extends Block {
   public static final MapCodec<SnowyDirtBlock> CODEC = simpleCodec(SnowyDirtBlock::new);
   public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

   @Override
   protected MapCodec<? extends SnowyDirtBlock> codec() {
      return CODEC;
   }

   protected SnowyDirtBlock(BlockBehaviour.Properties p_56640_) {
      super(p_56640_);
      this.registerDefaultState(this.stateDefinition.any().setValue(SNOWY, false));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_56644_,
      LevelReader p_368855_,
      ScheduledTickAccess p_365393_,
      BlockPos p_56648_,
      Direction p_56645_,
      BlockPos p_56649_,
      BlockState p_56646_,
      RandomSource p_368728_
   ) {
      return p_56645_ == Direction.UP
         ? p_56644_.setValue(SNOWY, isSnowySetting(p_56646_))
         : super.updateShape(p_56644_, p_368855_, p_365393_, p_56648_, p_56645_, p_56649_, p_56646_, p_368728_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56642_) {
      BlockState blockstate = p_56642_.getLevel().getBlockState(p_56642_.getClickedPos().above());
      return this.defaultBlockState().setValue(SNOWY, isSnowySetting(blockstate));
   }

   protected static boolean isSnowySetting(BlockState p_154649_) {
      return p_154649_.is(BlockTags.SNOW);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56651_) {
      p_56651_.add(SNOWY);
   }
}
