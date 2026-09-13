package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BambooStalkBlock extends Block implements BonemealableBlock {
   public static final MapCodec<BambooStalkBlock> CODEC = simpleCodec(BambooStalkBlock::new);
   private static final VoxelShape SHAPE_SMALL = Block.column(6.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_LARGE = Block.column(10.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_COLLISION = Block.column(3.0, 0.0, 16.0);
   public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
   public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
   public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
   public static final int MAX_HEIGHT = 16;
   public static final int STAGE_GROWING = 0;
   public static final int STAGE_DONE_GROWING = 1;
   public static final int AGE_THIN_BAMBOO = 0;
   public static final int AGE_THICK_BAMBOO = 1;

   @Override
   public MapCodec<BambooStalkBlock> codec() {
      return CODEC;
   }

   public BambooStalkBlock(BlockBehaviour.Properties p_261753_) {
      super(p_261753_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(LEAVES, BambooLeaves.NONE).setValue(STAGE, 0));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_261641_) {
      p_261641_.add(AGE, LEAVES, STAGE);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_261479_) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState p_261515_, BlockGetter p_261586_, BlockPos p_261526_, CollisionContext p_261930_) {
      VoxelShape voxelshape = p_261515_.getValue(LEAVES) == BambooLeaves.LARGE ? SHAPE_LARGE : SHAPE_SMALL;
      return voxelshape.move(p_261515_.getOffset(p_261526_));
   }

   @Override
   protected boolean isPathfindable(BlockState p_262166_, PathComputationType p_261513_) {
      return false;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_261560_, BlockGetter p_261965_, BlockPos p_261950_, CollisionContext p_261571_) {
      return SHAPE_COLLISION.move(p_261560_.getOffset(p_261950_));
   }

   @Override
   protected boolean isCollisionShapeFullBlock(BlockState p_262062_, BlockGetter p_261848_, BlockPos p_261466_) {
      return false;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_261764_) {
      FluidState fluidstate = p_261764_.getLevel().getFluidState(p_261764_.getClickedPos());
      if (!fluidstate.isEmpty()) {
         return null;
      }

      BlockState blockstate = p_261764_.getLevel().getBlockState(p_261764_.getClickedPos().below());
      if (blockstate.is(BlockTags.BAMBOO_PLANTABLE_ON)) {
         if (blockstate.is(Blocks.BAMBOO_SAPLING)) {
            return this.defaultBlockState().setValue(AGE, 0);
         } else if (blockstate.is(Blocks.BAMBOO)) {
            int i = blockstate.getValue(AGE) > 0 ? 1 : 0;
            return this.defaultBlockState().setValue(AGE, i);
         } else {
            BlockState blockstate1 = p_261764_.getLevel().getBlockState(p_261764_.getClickedPos().above());
            return blockstate1.is(Blocks.BAMBOO)
               ? this.defaultBlockState().setValue(AGE, blockstate1.getValue(AGE))
               : Blocks.BAMBOO_SAPLING.defaultBlockState();
         }
      } else {
         return null;
      }
   }

   @Override
   protected void tick(BlockState p_261612_, ServerLevel p_261527_, BlockPos p_261846_, RandomSource p_261638_) {
      if (!p_261612_.canSurvive(p_261527_, p_261846_)) {
         p_261527_.destroyBlock(p_261846_, true);
      }
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_262083_) {
      return p_262083_.getValue(STAGE) == 0;
   }

   @Override
   protected void randomTick(BlockState p_261931_, ServerLevel p_261751_, BlockPos p_261616_, RandomSource p_261766_) {
      if (p_261931_.getValue(STAGE) == 0
         && p_261766_.nextInt(3) == 0
         && p_261751_.isEmptyBlock(p_261616_.above())
         && p_261751_.getRawBrightness(p_261616_.above(), 0) >= 9) {
         int i = this.getHeightBelowUpToMax(p_261751_, p_261616_) + 1;
         if (i < 16) {
            this.growBamboo(p_261931_, p_261751_, p_261616_, p_261766_, i);
         }
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_261860_, LevelReader p_262154_, BlockPos p_261493_) {
      return p_262154_.getBlockState(p_261493_.below()).is(BlockTags.BAMBOO_PLANTABLE_ON);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_261476_,
      LevelReader p_362157_,
      ScheduledTickAccess p_366749_,
      BlockPos p_261876_,
      Direction p_261512_,
      BlockPos p_262140_,
      BlockState p_262167_,
      RandomSource p_364105_
   ) {
      if (!p_261476_.canSurvive(p_362157_, p_261876_)) {
         p_366749_.scheduleTick(p_261876_, this, 1);
      }

      return p_261512_ == Direction.UP && p_262167_.is(Blocks.BAMBOO) && p_262167_.getValue(AGE) > p_261476_.getValue(AGE)
         ? p_261476_.cycle(AGE)
         : super.updateShape(p_261476_, p_362157_, p_366749_, p_261876_, p_261512_, p_262140_, p_262167_, p_364105_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_262065_, BlockPos p_262033_, BlockState p_261700_) {
      int i = this.getHeightAboveUpToMax(p_262065_, p_262033_);
      int j = this.getHeightBelowUpToMax(p_262065_, p_262033_);
      return i + j + 1 < 16 && p_262065_.getBlockState(p_262033_.above(i)).getValue(STAGE) != 1;
   }

   @Override
   public boolean isBonemealSuccess(Level p_261870_, RandomSource p_261802_, BlockPos p_262123_, BlockState p_261972_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_261845_, RandomSource p_262034_, BlockPos p_261955_, BlockState p_261685_) {
      int i = this.getHeightAboveUpToMax(p_261845_, p_261955_);
      int j = this.getHeightBelowUpToMax(p_261845_, p_261955_);
      int k = i + j + 1;
      int l = 1 + p_262034_.nextInt(2);

      for (int i1 = 0; i1 < l; i1++) {
         BlockPos blockpos = p_261955_.above(i);
         BlockState blockstate = p_261845_.getBlockState(blockpos);
         if (k >= 16 || blockstate.getValue(STAGE) == 1 || !p_261845_.isEmptyBlock(blockpos.above())) {
            return;
         }

         this.growBamboo(blockstate, p_261845_, blockpos, p_262034_, k);
         i++;
         k++;
      }
   }

   protected void growBamboo(BlockState p_261855_, Level p_262076_, BlockPos p_262109_, RandomSource p_261633_, int p_261759_) {
      BlockState blockstate = p_262076_.getBlockState(p_262109_.below());
      BlockPos blockpos = p_262109_.below(2);
      BlockState blockstate1 = p_262076_.getBlockState(blockpos);
      BambooLeaves bambooleaves = BambooLeaves.NONE;
      if (p_261759_ >= 1) {
         if (!blockstate.is(Blocks.BAMBOO) || blockstate.getValue(LEAVES) == BambooLeaves.NONE) {
            bambooleaves = BambooLeaves.SMALL;
         } else if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
            bambooleaves = BambooLeaves.LARGE;
            if (blockstate1.is(Blocks.BAMBOO)) {
               p_262076_.setBlock(p_262109_.below(), blockstate.setValue(LEAVES, BambooLeaves.SMALL), 3);
               p_262076_.setBlock(blockpos, blockstate1.setValue(LEAVES, BambooLeaves.NONE), 3);
            }
         }
      }

      int i = p_261855_.getValue(AGE) != 1 && !blockstate1.is(Blocks.BAMBOO) ? 0 : 1;
      int j = (p_261759_ < 11 || !(p_261633_.nextFloat() < 0.25F)) && p_261759_ != 15 ? 0 : 1;
      p_262076_.setBlock(p_262109_.above(), this.defaultBlockState().setValue(AGE, i).setValue(LEAVES, bambooleaves).setValue(STAGE, j), 3);
   }

   protected int getHeightAboveUpToMax(BlockGetter p_261541_, BlockPos p_261593_) {
      int i = 0;

      while (i < 16 && p_261541_.getBlockState(p_261593_.above(i + 1)).is(Blocks.BAMBOO)) {
         i++;
      }

      return i;
   }

   protected int getHeightBelowUpToMax(BlockGetter p_261927_, BlockPos p_261481_) {
      int i = 0;

      while (i < 16 && p_261927_.getBlockState(p_261481_.below(i + 1)).is(Blocks.BAMBOO)) {
         i++;
      }

      return i;
   }
}
