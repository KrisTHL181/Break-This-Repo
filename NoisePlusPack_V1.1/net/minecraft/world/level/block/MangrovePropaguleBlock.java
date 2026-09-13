package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MangrovePropaguleBlock extends SaplingBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<MangrovePropaguleBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422118_ -> p_422118_.group(TreeGrower.CODEC.fieldOf("tree").forGetter(p_310695_ -> p_310695_.treeGrower), propertiesCodec())
         .apply(p_422118_, MangrovePropaguleBlock::new)
   );
   public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
   public static final int MAX_AGE = 4;
   private static final int[] SHAPE_MIN_Y = new int[]{13, 10, 7, 3, 0};
   private static final VoxelShape[] SHAPE_PER_AGE = Block.boxes(4, p_390941_ -> Block.column(2.0, SHAPE_MIN_Y[p_390941_], 16.0));
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

   @Override
   public MapCodec<MangrovePropaguleBlock> codec() {
      return CODEC;
   }

   public MangrovePropaguleBlock(TreeGrower p_312632_, BlockBehaviour.Properties p_221449_) {
      super(p_312632_, p_221449_);
      this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0).setValue(AGE, 0).setValue(WATERLOGGED, false).setValue(HANGING, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_221484_) {
      p_221484_.add(STAGE).add(AGE).add(WATERLOGGED).add(HANGING);
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_221496_, BlockGetter p_221497_, BlockPos p_221498_) {
      return super.mayPlaceOn(p_221496_, p_221497_, p_221498_) || p_221496_.is(Blocks.CLAY);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_221456_) {
      FluidState fluidstate = p_221456_.getLevel().getFluidState(p_221456_.getClickedPos());
      boolean flag = fluidstate.getType() == Fluids.WATER;
      return super.getStateForPlacement(p_221456_).setValue(WATERLOGGED, flag).setValue(AGE, 4);
   }

   @Override
   protected VoxelShape getShape(BlockState p_221468_, BlockGetter p_221469_, BlockPos p_221470_, CollisionContext p_221471_) {
      int i = p_221468_.getValue(HANGING) ? p_221468_.getValue(AGE) : 4;
      return SHAPE_PER_AGE[i].move(p_221468_.getOffset(p_221470_));
   }

   @Override
   protected boolean canSurvive(BlockState p_221473_, LevelReader p_221474_, BlockPos p_221475_) {
      return isHanging(p_221473_) ? p_221474_.getBlockState(p_221475_.above()).is(Blocks.MANGROVE_LEAVES) : super.canSurvive(p_221473_, p_221474_, p_221475_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_221477_,
      LevelReader p_365009_,
      ScheduledTickAccess p_369801_,
      BlockPos p_221481_,
      Direction p_221478_,
      BlockPos p_221482_,
      BlockState p_221479_,
      RandomSource p_367883_
   ) {
      if (p_221477_.getValue(WATERLOGGED)) {
         p_369801_.scheduleTick(p_221481_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365009_));
      }

      return p_221478_ == Direction.UP && !p_221477_.canSurvive(p_365009_, p_221481_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_221477_, p_365009_, p_369801_, p_221481_, p_221478_, p_221482_, p_221479_, p_367883_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_221494_) {
      return p_221494_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_221494_);
   }

   @Override
   protected void randomTick(BlockState p_221488_, ServerLevel p_221489_, BlockPos p_221490_, RandomSource p_221491_) {
      if (!isHanging(p_221488_)) {
         if (p_221491_.nextInt(7) == 0) {
            this.advanceTree(p_221489_, p_221490_, p_221488_, p_221491_);
         }
      } else if (!isFullyGrown(p_221488_)) {
         p_221489_.setBlock(p_221490_, p_221488_.cycle(AGE), 2);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256541_, BlockPos p_221459_, BlockState p_221460_) {
      return !isHanging(p_221460_) || !isFullyGrown(p_221460_);
   }

   @Override
   public boolean isBonemealSuccess(Level p_221463_, RandomSource p_221464_, BlockPos p_221465_, BlockState p_221466_) {
      return isHanging(p_221466_) ? !isFullyGrown(p_221466_) : super.isBonemealSuccess(p_221463_, p_221464_, p_221465_, p_221466_);
   }

   @Override
   public void performBonemeal(ServerLevel p_221451_, RandomSource p_221452_, BlockPos p_221453_, BlockState p_221454_) {
      if (isHanging(p_221454_) && !isFullyGrown(p_221454_)) {
         p_221451_.setBlock(p_221453_, p_221454_.cycle(AGE), 2);
      } else {
         super.performBonemeal(p_221451_, p_221452_, p_221453_, p_221454_);
      }
   }

   private static boolean isHanging(BlockState p_221500_) {
      return p_221500_.getValue(HANGING);
   }

   private static boolean isFullyGrown(BlockState p_221502_) {
      return p_221502_.getValue(AGE) == 4;
   }

   public static BlockState createNewHangingPropagule() {
      return createNewHangingPropagule(0);
   }

   public static BlockState createNewHangingPropagule(int p_221486_) {
      return Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(HANGING, true).setValue(AGE, p_221486_);
   }
}
