package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerBedBlock extends VegetationBlock implements BonemealableBlock, SegmentableBlock {
   public static final MapCodec<FlowerBedBlock> CODEC = simpleCodec(FlowerBedBlock::new);
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final IntegerProperty AMOUNT = BlockStateProperties.FLOWER_AMOUNT;
   private final Function<BlockState, VoxelShape> shapes;

   @Override
   public MapCodec<FlowerBedBlock> codec() {
      return CODEC;
   }

   protected FlowerBedBlock(BlockBehaviour.Properties p_397218_) {
      super(p_397218_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AMOUNT, 1));
      this.shapes = this.makeShapes();
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      return this.getShapeForEachState(this.getShapeCalculator(FACING, AMOUNT));
   }

   @Override
   public BlockState rotate(BlockState p_393921_, Rotation p_392639_) {
      return p_393921_.setValue(FACING, p_392639_.rotate(p_393921_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_391796_, Mirror p_392417_) {
      return p_391796_.rotate(p_392417_.getRotation(p_391796_.getValue(FACING)));
   }

   @Override
   public boolean canBeReplaced(BlockState p_392482_, BlockPlaceContext p_397242_) {
      return this.canBeReplaced(p_392482_, p_397242_, AMOUNT) ? true : super.canBeReplaced(p_392482_, p_397242_);
   }

   @Override
   public VoxelShape getShape(BlockState p_392309_, BlockGetter p_396575_, BlockPos p_394826_, CollisionContext p_394289_) {
      return this.shapes.apply(p_392309_);
   }

   @Override
   public double getShapeHeight() {
      return 3.0;
   }

   @Override
   public IntegerProperty getSegmentAmountProperty() {
      return AMOUNT;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_395692_) {
      return this.getStateForPlacement(p_395692_, this, AMOUNT, FACING);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_397787_) {
      p_397787_.add(FACING, AMOUNT);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_395020_, BlockPos p_395501_, BlockState p_395088_) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level p_392861_, RandomSource p_396866_, BlockPos p_392975_, BlockState p_391909_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_393621_, RandomSource p_397965_, BlockPos p_393101_, BlockState p_394965_) {
      int i = p_394965_.getValue(AMOUNT);
      if (i < 4) {
         p_393621_.setBlock(p_393101_, p_394965_.setValue(AMOUNT, i + 1), 2);
      } else {
         popResource(p_393621_, p_393101_, new ItemStack(this));
      }
   }
}
