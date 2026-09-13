package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class StandingSignBlock extends SignBlock {
   public static final MapCodec<StandingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422128_ -> p_422128_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec()).apply(p_422128_, StandingSignBlock::new)
   );
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

   @Override
   public MapCodec<StandingSignBlock> codec() {
      return CODEC;
   }

   public StandingSignBlock(WoodType p_56991_, BlockBehaviour.Properties p_56990_) {
      super(p_56991_, p_56990_.sound(p_56991_.soundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean canSurvive(BlockState p_56995_, LevelReader p_56996_, BlockPos p_56997_) {
      return p_56996_.getBlockState(p_56997_.below()).isSolid();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56993_) {
      FluidState fluidstate = p_56993_.getLevel().getFluidState(p_56993_.getClickedPos());
      return this.defaultBlockState()
         .setValue(ROTATION, RotationSegment.convertToSegment(p_56993_.getRotation() + 180.0F))
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_57005_,
      LevelReader p_369213_,
      ScheduledTickAccess p_368266_,
      BlockPos p_57009_,
      Direction p_57006_,
      BlockPos p_57010_,
      BlockState p_57007_,
      RandomSource p_361208_
   ) {
      return p_57006_ == Direction.DOWN && !this.canSurvive(p_57005_, p_369213_, p_57009_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_57005_, p_369213_, p_368266_, p_57009_, p_57006_, p_57010_, p_57007_, p_361208_);
   }

   @Override
   public float getYRotationDegrees(BlockState p_277795_) {
      return RotationSegment.convertToDegrees(p_277795_.getValue(ROTATION));
   }

   @Override
   protected BlockState rotate(BlockState p_57002_, Rotation p_57003_) {
      return p_57002_.setValue(ROTATION, p_57003_.rotate(p_57002_.getValue(ROTATION), 16));
   }

   @Override
   protected BlockState mirror(BlockState p_56999_, Mirror p_57000_) {
      return p_56999_.setValue(ROTATION, p_57000_.mirror(p_56999_.getValue(ROTATION), 16));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57012_) {
      p_57012_.add(ROTATION, WATERLOGGED);
   }
}
