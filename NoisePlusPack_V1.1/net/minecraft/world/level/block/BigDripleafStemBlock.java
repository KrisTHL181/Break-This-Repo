package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigDripleafStemBlock extends HorizontalDirectionalBlock implements BonemealableBlock, SimpleWaterloggedBlock {
   public static final MapCodec<BigDripleafStemBlock> CODEC = simpleCodec(BigDripleafStemBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.column(6.0, 0.0, 16.0).move(0.0, 0.0, 0.25).optimize());

   @Override
   public MapCodec<BigDripleafStemBlock> codec() {
      return CODEC;
   }

   protected BigDripleafStemBlock(BlockBehaviour.Properties p_152329_) {
      super(p_152329_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
   }

   @Override
   protected VoxelShape getShape(BlockState p_152360_, BlockGetter p_152361_, BlockPos p_152362_, CollisionContext p_152363_) {
      return SHAPES.get(p_152360_.getValue(FACING));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152376_) {
      p_152376_.add(WATERLOGGED, FACING);
   }

   @Override
   protected FluidState getFluidState(BlockState p_152378_) {
      return p_152378_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152378_);
   }

   @Override
   protected boolean canSurvive(BlockState p_152365_, LevelReader p_152366_, BlockPos p_152367_) {
      BlockPos blockpos = p_152367_.below();
      BlockState blockstate = p_152366_.getBlockState(blockpos);
      BlockState blockstate1 = p_152366_.getBlockState(p_152367_.above());
      return (blockstate.is(this) || blockstate.is(BlockTags.BIG_DRIPLEAF_PLACEABLE)) && (blockstate1.is(this) || blockstate1.is(Blocks.BIG_DRIPLEAF));
   }

   protected static boolean place(LevelAccessor p_152350_, BlockPos p_152351_, FluidState p_152352_, Direction p_152353_) {
      BlockState blockstate = Blocks.BIG_DRIPLEAF_STEM
         .defaultBlockState()
         .setValue(WATERLOGGED, p_152352_.isSourceOfType(Fluids.WATER))
         .setValue(FACING, p_152353_);
      return p_152350_.setBlock(p_152351_, blockstate, 3);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_152369_,
      LevelReader p_370232_,
      ScheduledTickAccess p_369831_,
      BlockPos p_152373_,
      Direction p_152370_,
      BlockPos p_152374_,
      BlockState p_152371_,
      RandomSource p_369505_
   ) {
      if ((p_152370_ == Direction.DOWN || p_152370_ == Direction.UP) && !p_152369_.canSurvive(p_370232_, p_152373_)) {
         p_369831_.scheduleTick(p_152373_, this, 1);
      }

      if (p_152369_.getValue(WATERLOGGED)) {
         p_369831_.scheduleTick(p_152373_, Fluids.WATER, Fluids.WATER.getTickDelay(p_370232_));
      }

      return super.updateShape(p_152369_, p_370232_, p_369831_, p_152373_, p_152370_, p_152374_, p_152371_, p_369505_);
   }

   @Override
   protected void tick(BlockState p_220813_, ServerLevel p_220814_, BlockPos p_220815_, RandomSource p_220816_) {
      if (!p_220813_.canSurvive(p_220814_, p_220815_)) {
         p_220814_.destroyBlock(p_220815_, true);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255683_, BlockPos p_256358_, BlockState p_256408_) {
      Optional<BlockPos> optional = BlockUtil.getTopConnectedBlock(p_255683_, p_256358_, p_256408_.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if (optional.isEmpty()) {
         return false;
      }

      BlockPos blockpos = optional.get().above();
      BlockState blockstate = p_255683_.getBlockState(blockpos);
      return BigDripleafBlock.canPlaceAt(p_255683_, blockpos, blockstate);
   }

   @Override
   public boolean isBonemealSuccess(Level p_220808_, RandomSource p_220809_, BlockPos p_220810_, BlockState p_220811_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_220803_, RandomSource p_220804_, BlockPos p_220805_, BlockState p_220806_) {
      Optional<BlockPos> optional = BlockUtil.getTopConnectedBlock(p_220803_, p_220805_, p_220806_.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if (!optional.isEmpty()) {
         BlockPos blockpos = optional.get();
         BlockPos blockpos1 = blockpos.above();
         Direction direction = p_220806_.getValue(FACING);
         place(p_220803_, blockpos, p_220803_.getFluidState(blockpos), direction);
         BigDripleafBlock.place(p_220803_, blockpos1, p_220803_.getFluidState(blockpos1), direction);
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312051_, BlockPos p_152337_, BlockState p_152338_, boolean p_378221_) {
      return new ItemStack(Blocks.BIG_DRIPLEAF);
   }
}
