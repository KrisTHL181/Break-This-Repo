package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);

   @Override
   public MapCodec<? extends IronBarsBlock> codec() {
      return CODEC;
   }

   protected IronBarsBlock(BlockBehaviour.Properties p_54198_) {
      super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, p_54198_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54200_) {
      BlockGetter blockgetter = p_54200_.getLevel();
      BlockPos blockpos = p_54200_.getClickedPos();
      FluidState fluidstate = p_54200_.getLevel().getFluidState(p_54200_.getClickedPos());
      BlockPos blockpos1 = blockpos.north();
      BlockPos blockpos2 = blockpos.south();
      BlockPos blockpos3 = blockpos.west();
      BlockPos blockpos4 = blockpos.east();
      BlockState blockstate = blockgetter.getBlockState(blockpos1);
      BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
      BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
      BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
      return this.defaultBlockState()
         .setValue(NORTH, this.attachsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH)))
         .setValue(SOUTH, this.attachsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.NORTH)))
         .setValue(WEST, this.attachsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.EAST)))
         .setValue(EAST, this.attachsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.WEST)))
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_54211_,
      LevelReader p_367146_,
      ScheduledTickAccess p_367530_,
      BlockPos p_54215_,
      Direction p_54212_,
      BlockPos p_54216_,
      BlockState p_54213_,
      RandomSource p_369110_
   ) {
      if (p_54211_.getValue(WATERLOGGED)) {
         p_367530_.scheduleTick(p_54215_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367146_));
      }

      return p_54212_.getAxis().isHorizontal()
         ? p_54211_.setValue(PROPERTY_BY_DIRECTION.get(p_54212_), this.attachsTo(p_54213_, p_54213_.isFaceSturdy(p_367146_, p_54216_, p_54212_.getOpposite())))
         : super.updateShape(p_54211_, p_367146_, p_367530_, p_54215_, p_54212_, p_54216_, p_54213_, p_369110_);
   }

   @Override
   protected VoxelShape getVisualShape(BlockState p_54202_, BlockGetter p_54203_, BlockPos p_54204_, CollisionContext p_54205_) {
      return Shapes.empty();
   }

   @Override
   protected boolean skipRendering(BlockState p_54207_, BlockState p_54208_, Direction p_54209_) {
      if (p_54208_.is(this)
         || p_54208_.is(BlockTags.BARS) && p_54207_.is(BlockTags.BARS) && p_54208_.hasProperty(PROPERTY_BY_DIRECTION.get(p_54209_.getOpposite()))) {
         if (!p_54209_.getAxis().isHorizontal()) {
            return true;
         }

         if (p_54207_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_)) && p_54208_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(p_54207_, p_54208_, p_54209_);
   }

   public final boolean attachsTo(BlockState p_54218_, boolean p_54219_) {
      return !isExceptionForConnection(p_54218_) && p_54219_ || p_54218_.getBlock() instanceof IronBarsBlock || p_54218_.is(BlockTags.WALLS);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54221_) {
      p_54221_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
