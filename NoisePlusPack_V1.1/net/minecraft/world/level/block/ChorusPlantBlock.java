package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
   public static final MapCodec<ChorusPlantBlock> CODEC = simpleCodec(ChorusPlantBlock::new);

   @Override
   public MapCodec<ChorusPlantBlock> codec() {
      return CODEC;
   }

   protected ChorusPlantBlock(BlockBehaviour.Properties p_51707_) {
      super(10.0F, p_51707_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51709_) {
      return getStateWithConnections(p_51709_.getLevel(), p_51709_.getClickedPos(), this.defaultBlockState());
   }

   public static BlockState getStateWithConnections(BlockGetter p_51711_, BlockPos p_51712_, BlockState p_312378_) {
      BlockState blockstate = p_51711_.getBlockState(p_51712_.below());
      BlockState blockstate1 = p_51711_.getBlockState(p_51712_.above());
      BlockState blockstate2 = p_51711_.getBlockState(p_51712_.north());
      BlockState blockstate3 = p_51711_.getBlockState(p_51712_.east());
      BlockState blockstate4 = p_51711_.getBlockState(p_51712_.south());
      BlockState blockstate5 = p_51711_.getBlockState(p_51712_.west());
      Block block = p_312378_.getBlock();
      return p_312378_.trySetValue(DOWN, blockstate.is(block) || blockstate.is(Blocks.CHORUS_FLOWER) || blockstate.is(Blocks.END_STONE))
         .trySetValue(UP, blockstate1.is(block) || blockstate1.is(Blocks.CHORUS_FLOWER))
         .trySetValue(NORTH, blockstate2.is(block) || blockstate2.is(Blocks.CHORUS_FLOWER))
         .trySetValue(EAST, blockstate3.is(block) || blockstate3.is(Blocks.CHORUS_FLOWER))
         .trySetValue(SOUTH, blockstate4.is(block) || blockstate4.is(Blocks.CHORUS_FLOWER))
         .trySetValue(WEST, blockstate5.is(block) || blockstate5.is(Blocks.CHORUS_FLOWER));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51728_,
      LevelReader p_369826_,
      ScheduledTickAccess p_364837_,
      BlockPos p_51732_,
      Direction p_51729_,
      BlockPos p_51733_,
      BlockState p_51730_,
      RandomSource p_368636_
   ) {
      if (!p_51728_.canSurvive(p_369826_, p_51732_)) {
         p_364837_.scheduleTick(p_51732_, this, 1);
         return super.updateShape(p_51728_, p_369826_, p_364837_, p_51732_, p_51729_, p_51733_, p_51730_, p_368636_);
      } else {
         boolean flag = p_51730_.is(this) || p_51730_.is(Blocks.CHORUS_FLOWER) || p_51729_ == Direction.DOWN && p_51730_.is(Blocks.END_STONE);
         return p_51728_.setValue(PROPERTY_BY_DIRECTION.get(p_51729_), flag);
      }
   }

   @Override
   protected void tick(BlockState p_220985_, ServerLevel p_220986_, BlockPos p_220987_, RandomSource p_220988_) {
      if (!p_220985_.canSurvive(p_220986_, p_220987_)) {
         p_220986_.destroyBlock(p_220987_, true);
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_51724_, LevelReader p_51725_, BlockPos p_51726_) {
      BlockState blockstate = p_51725_.getBlockState(p_51726_.below());
      boolean flag = !p_51725_.getBlockState(p_51726_.above()).isAir() && !blockstate.isAir();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_51726_.relative(direction);
         BlockState blockstate1 = p_51725_.getBlockState(blockpos);
         if (blockstate1.is(this)) {
            if (flag) {
               return false;
            }

            BlockState blockstate2 = p_51725_.getBlockState(blockpos.below());
            if (blockstate2.is(this) || blockstate2.is(Blocks.END_STONE)) {
               return true;
            }
         }
      }

      return blockstate.is(this) || blockstate.is(Blocks.END_STONE);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51735_) {
      p_51735_.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
   }

   @Override
   protected boolean isPathfindable(BlockState p_51719_, PathComputationType p_51722_) {
      return false;
   }
}
