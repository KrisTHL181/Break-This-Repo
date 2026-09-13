package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class AmethystClusterBlock extends AmethystBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<AmethystClusterBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422034_ -> p_422034_.group(
            Codec.FLOAT.fieldOf("height").forGetter(p_313043_ -> p_313043_.height),
            Codec.FLOAT.fieldOf("width").forGetter(p_390893_ -> p_390893_.width),
            propertiesCodec()
         )
         .apply(p_422034_, AmethystClusterBlock::new)
   );
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
   private final float height;
   private final float width;
   private final Map<Direction, VoxelShape> shapes;

   @Override
   public MapCodec<AmethystClusterBlock> codec() {
      return CODEC;
   }

   public AmethystClusterBlock(float p_313148_, float p_309607_, BlockBehaviour.Properties p_152017_) {
      super(p_152017_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(FACING, Direction.UP));
      this.shapes = Shapes.rotateAll(Block.boxZ(p_309607_, 16.0F - p_313148_, 16.0));
      this.height = p_313148_;
      this.width = p_309607_;
   }

   @Override
   protected VoxelShape getShape(BlockState p_152021_, BlockGetter p_152022_, BlockPos p_152023_, CollisionContext p_152024_) {
      return this.shapes.get(p_152021_.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(BlockState p_152026_, LevelReader p_152027_, BlockPos p_152028_) {
      Direction direction = p_152026_.getValue(FACING);
      BlockPos blockpos = p_152028_.relative(direction.getOpposite());
      return p_152027_.getBlockState(blockpos).isFaceSturdy(p_152027_, blockpos, direction);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_152036_,
      LevelReader p_364765_,
      ScheduledTickAccess p_364906_,
      BlockPos p_152040_,
      Direction p_152037_,
      BlockPos p_152041_,
      BlockState p_152038_,
      RandomSource p_368729_
   ) {
      if (p_152036_.getValue(WATERLOGGED)) {
         p_364906_.scheduleTick(p_152040_, Fluids.WATER, Fluids.WATER.getTickDelay(p_364765_));
      }

      return p_152037_ == p_152036_.getValue(FACING).getOpposite() && !p_152036_.canSurvive(p_364765_, p_152040_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_152036_, p_364765_, p_364906_, p_152040_, p_152037_, p_152041_, p_152038_, p_368729_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_152019_) {
      LevelAccessor levelaccessor = p_152019_.getLevel();
      BlockPos blockpos = p_152019_.getClickedPos();
      return this.defaultBlockState()
         .setValue(WATERLOGGED, levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER)
         .setValue(FACING, p_152019_.getClickedFace());
   }

   @Override
   protected BlockState rotate(BlockState p_152033_, Rotation p_152034_) {
      return p_152033_.setValue(FACING, p_152034_.rotate(p_152033_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_152030_, Mirror p_152031_) {
      return p_152030_.rotate(p_152031_.getRotation(p_152030_.getValue(FACING)));
   }

   @Override
   protected FluidState getFluidState(BlockState p_152045_) {
      return p_152045_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152045_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152043_) {
      p_152043_.add(WATERLOGGED, FACING);
   }
}
