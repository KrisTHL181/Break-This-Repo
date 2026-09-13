package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BaseCoralWallFanBlock extends BaseCoralFanBlock {
   public static final MapCodec<BaseCoralWallFanBlock> CODEC = simpleCodec(BaseCoralWallFanBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 8.0, 5.0, 16.0));

   @Override
   public MapCodec<? extends BaseCoralWallFanBlock> codec() {
      return CODEC;
   }

   protected BaseCoralWallFanBlock(BlockBehaviour.Properties p_49196_) {
      super(p_49196_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, true));
   }

   @Override
   protected VoxelShape getShape(BlockState p_49219_, BlockGetter p_49220_, BlockPos p_49221_, CollisionContext p_49222_) {
      return SHAPES.get(p_49219_.getValue(FACING));
   }

   @Override
   protected BlockState rotate(BlockState p_49207_, Rotation p_49208_) {
      return p_49207_.setValue(FACING, p_49208_.rotate(p_49207_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_49204_, Mirror p_49205_) {
      return p_49204_.rotate(p_49205_.getRotation(p_49204_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49217_) {
      p_49217_.add(FACING, WATERLOGGED);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_49210_,
      LevelReader p_362875_,
      ScheduledTickAccess p_370057_,
      BlockPos p_49214_,
      Direction p_49211_,
      BlockPos p_49215_,
      BlockState p_49212_,
      RandomSource p_365418_
   ) {
      if (p_49210_.getValue(WATERLOGGED)) {
         p_370057_.scheduleTick(p_49214_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362875_));
      }

      return p_49211_.getOpposite() == p_49210_.getValue(FACING) && !p_49210_.canSurvive(p_362875_, p_49214_) ? Blocks.AIR.defaultBlockState() : p_49210_;
   }

   @Override
   protected boolean canSurvive(BlockState p_49200_, LevelReader p_49201_, BlockPos p_49202_) {
      Direction direction = p_49200_.getValue(FACING);
      BlockPos blockpos = p_49202_.relative(direction.getOpposite());
      BlockState blockstate = p_49201_.getBlockState(blockpos);
      return blockstate.isFaceSturdy(p_49201_, blockpos, direction);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_49198_) {
      BlockState blockstate = super.getStateForPlacement(p_49198_);
      LevelReader levelreader = p_49198_.getLevel();
      BlockPos blockpos = p_49198_.getClickedPos();
      Direction[] adirection = p_49198_.getNearestLookingDirections();

      for (Direction direction : adirection) {
         if (direction.getAxis().isHorizontal()) {
            blockstate = blockstate.setValue(FACING, direction.getOpposite());
            if (blockstate.canSurvive(levelreader, blockpos)) {
               return blockstate;
            }
         }
      }

      return null;
   }
}
