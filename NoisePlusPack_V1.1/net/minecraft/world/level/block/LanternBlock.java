package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LanternBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<LanternBlock> CODEC = simpleCodec(LanternBlock::new);
   public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE_STANDING = Shapes.or(Block.column(4.0, 7.0, 9.0), Block.column(6.0, 0.0, 7.0));
   private static final VoxelShape SHAPE_HANGING = SHAPE_STANDING.move(0.0, 0.0625, 0.0).optimize();

   @Override
   public MapCodec<? extends LanternBlock> codec() {
      return CODEC;
   }

   public LanternBlock(BlockBehaviour.Properties p_153465_) {
      super(p_153465_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HANGING, false).setValue(WATERLOGGED, false));
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_153467_) {
      FluidState fluidstate = p_153467_.getLevel().getFluidState(p_153467_.getClickedPos());

      for (Direction direction : p_153467_.getNearestLookingDirections()) {
         if (direction.getAxis() == Direction.Axis.Y) {
            BlockState blockstate = this.defaultBlockState().setValue(HANGING, direction == Direction.UP);
            if (blockstate.canSurvive(p_153467_.getLevel(), p_153467_.getClickedPos())) {
               return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
            }
         }
      }

      return null;
   }

   @Override
   protected VoxelShape getShape(BlockState p_153474_, BlockGetter p_153475_, BlockPos p_153476_, CollisionContext p_153477_) {
      return p_153474_.getValue(HANGING) ? SHAPE_HANGING : SHAPE_STANDING;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153490_) {
      p_153490_.add(HANGING, WATERLOGGED);
   }

   @Override
   protected boolean canSurvive(BlockState p_153479_, LevelReader p_153480_, BlockPos p_153481_) {
      Direction direction = getConnectedDirection(p_153479_).getOpposite();
      return Block.canSupportCenter(p_153480_, p_153481_.relative(direction), direction.getOpposite());
   }

   protected static Direction getConnectedDirection(BlockState p_153496_) {
      return p_153496_.getValue(HANGING) ? Direction.DOWN : Direction.UP;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_153483_,
      LevelReader p_362938_,
      ScheduledTickAccess p_369863_,
      BlockPos p_153487_,
      Direction p_153484_,
      BlockPos p_153488_,
      BlockState p_153485_,
      RandomSource p_369622_
   ) {
      if (p_153483_.getValue(WATERLOGGED)) {
         p_369863_.scheduleTick(p_153487_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362938_));
      }

      return getConnectedDirection(p_153483_).getOpposite() == p_153484_ && !p_153483_.canSurvive(p_362938_, p_153487_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_153483_, p_362938_, p_369863_, p_153487_, p_153484_, p_153488_, p_153485_, p_369622_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_153492_) {
      return p_153492_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153492_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_153469_, PathComputationType p_153472_) {
      return false;
   }
}
