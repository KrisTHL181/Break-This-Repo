package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public class ObserverBlock extends DirectionalBlock {
   public static final MapCodec<ObserverBlock> CODEC = simpleCodec(ObserverBlock::new);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   @Override
   public MapCodec<ObserverBlock> codec() {
      return CODEC;
   }

   public ObserverBlock(BlockBehaviour.Properties p_55085_) {
      super(p_55085_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POWERED, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55125_) {
      p_55125_.add(FACING, POWERED);
   }

   @Override
   protected BlockState rotate(BlockState p_55115_, Rotation p_55116_) {
      return p_55115_.setValue(FACING, p_55116_.rotate(p_55115_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_55112_, Mirror p_55113_) {
      return p_55112_.rotate(p_55113_.getRotation(p_55112_.getValue(FACING)));
   }

   @Override
   protected void tick(BlockState p_221840_, ServerLevel p_221841_, BlockPos p_221842_, RandomSource p_221843_) {
      if (p_221840_.getValue(POWERED)) {
         p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, false), 2);
      } else {
         p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, true), 2);
         p_221841_.scheduleTick(p_221842_, this, 2);
      }

      this.updateNeighborsInFront(p_221841_, p_221842_, p_221840_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_55118_,
      LevelReader p_362436_,
      ScheduledTickAccess p_366728_,
      BlockPos p_55122_,
      Direction p_55119_,
      BlockPos p_55123_,
      BlockState p_55120_,
      RandomSource p_368192_
   ) {
      if (p_55118_.getValue(FACING) == p_55119_ && !p_55118_.getValue(POWERED)) {
         this.startSignal(p_362436_, p_366728_, p_55122_);
      }

      return super.updateShape(p_55118_, p_362436_, p_366728_, p_55122_, p_55119_, p_55123_, p_55120_, p_368192_);
   }

   private void startSignal(LevelReader p_369671_, ScheduledTickAccess p_365436_, BlockPos p_55094_) {
      if (!p_369671_.isClientSide() && !p_365436_.getBlockTicks().hasScheduledTick(p_55094_, this)) {
         p_365436_.scheduleTick(p_55094_, this, 2);
      }
   }

   protected void updateNeighborsInFront(Level p_55089_, BlockPos p_55090_, BlockState p_55091_) {
      Direction direction = p_55091_.getValue(FACING);
      BlockPos blockpos = p_55090_.relative(direction.getOpposite());
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(p_55089_, direction.getOpposite(), null);
      p_55089_.neighborChanged(blockpos, this, orientation);
      p_55089_.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
   }

   @Override
   protected boolean isSignalSource(BlockState p_55138_) {
      return true;
   }

   @Override
   protected int getDirectSignal(BlockState p_55127_, BlockGetter p_55128_, BlockPos p_55129_, Direction p_55130_) {
      return p_55127_.getSignal(p_55128_, p_55129_, p_55130_);
   }

   @Override
   protected int getSignal(BlockState p_55101_, BlockGetter p_55102_, BlockPos p_55103_, Direction p_55104_) {
      return p_55101_.getValue(POWERED) && p_55101_.getValue(FACING) == p_55104_ ? 15 : 0;
   }

   @Override
   protected void onPlace(BlockState p_55132_, Level p_55133_, BlockPos p_55134_, BlockState p_55135_, boolean p_55136_) {
      if (!p_55132_.is(p_55135_.getBlock())
         && !p_55133_.isClientSide()
         && p_55132_.getValue(POWERED)
         && !p_55133_.getBlockTicks().hasScheduledTick(p_55134_, this)) {
         BlockState blockstate = p_55132_.setValue(POWERED, false);
         p_55133_.setBlock(p_55134_, blockstate, 18);
         this.updateNeighborsInFront(p_55133_, p_55134_, blockstate);
      }
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_394748_, ServerLevel p_398030_, BlockPos p_392130_, boolean p_394233_) {
      if (p_394748_.getValue(POWERED) && p_398030_.getBlockTicks().hasScheduledTick(p_392130_, this)) {
         this.updateNeighborsInFront(p_398030_, p_392130_, p_394748_.setValue(POWERED, false));
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_55087_) {
      return this.defaultBlockState().setValue(FACING, p_55087_.getNearestLookingDirection().getOpposite().getOpposite());
   }
}
