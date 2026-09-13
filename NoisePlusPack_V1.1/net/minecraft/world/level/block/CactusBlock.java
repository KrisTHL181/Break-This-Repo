package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusBlock extends Block {
   public static final MapCodec<CactusBlock> CODEC = simpleCodec(CactusBlock::new);
   public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
   public static final int MAX_AGE = 15;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_COLLISION = Block.column(14.0, 0.0, 15.0);
   private static final int MAX_CACTUS_GROWING_HEIGHT = 3;
   private static final int ATTEMPT_GROW_CACTUS_FLOWER_AGE = 8;
   private static final double ATTEMPT_GROW_CACTUS_FLOWER_SMALL_CACTUS_CHANCE = 0.1;
   private static final double ATTEMPT_GROW_CACTUS_FLOWER_TALL_CACTUS_CHANCE = 0.25;

   @Override
   public MapCodec<CactusBlock> codec() {
      return CODEC;
   }

   protected CactusBlock(BlockBehaviour.Properties p_51136_) {
      super(p_51136_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   protected void tick(BlockState p_220908_, ServerLevel p_220909_, BlockPos p_220910_, RandomSource p_220911_) {
      if (!p_220908_.canSurvive(p_220909_, p_220910_)) {
         p_220909_.destroyBlock(p_220910_, true);
      }
   }

   @Override
   protected void randomTick(BlockState p_220913_, ServerLevel p_220914_, BlockPos p_220915_, RandomSource p_220916_) {
      BlockPos blockpos = p_220915_.above();
      if (p_220914_.isEmptyBlock(blockpos)) {
         int i = 1;
         int j = p_220913_.getValue(AGE);

         while (p_220914_.getBlockState(p_220915_.below(i)).is(this)) {
            if (++i == 3 && j == 15) {
               return;
            }
         }

         if (j == 8 && this.canSurvive(this.defaultBlockState(), p_220914_, p_220915_.above())) {
            double d0 = i >= 3 ? 0.25 : 0.1;
            if (p_220916_.nextDouble() <= d0) {
               p_220914_.setBlockAndUpdate(blockpos, Blocks.CACTUS_FLOWER.defaultBlockState());
            }
         } else if (j == 15 && i < 3) {
            p_220914_.setBlockAndUpdate(blockpos, this.defaultBlockState());
            BlockState blockstate = p_220913_.setValue(AGE, 0);
            p_220914_.setBlock(p_220915_, blockstate, 260);
            p_220914_.neighborChanged(blockstate, blockpos, this, null, false);
         }

         if (j < 15) {
            p_220914_.setBlock(p_220915_, p_220913_.setValue(AGE, j + 1), 260);
         }
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_51176_, BlockGetter p_51177_, BlockPos p_51178_, CollisionContext p_51179_) {
      return SHAPE_COLLISION;
   }

   @Override
   protected VoxelShape getShape(BlockState p_51171_, BlockGetter p_51172_, BlockPos p_51173_, CollisionContext p_51174_) {
      return SHAPE;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51157_,
      LevelReader p_368068_,
      ScheduledTickAccess p_362750_,
      BlockPos p_51161_,
      Direction p_51158_,
      BlockPos p_51162_,
      BlockState p_51159_,
      RandomSource p_362850_
   ) {
      if (!p_51157_.canSurvive(p_368068_, p_51161_)) {
         p_362750_.scheduleTick(p_51161_, this, 1);
      }

      return super.updateShape(p_51157_, p_368068_, p_362750_, p_51161_, p_51158_, p_51162_, p_51159_, p_362850_);
   }

   @Override
   protected boolean canSurvive(BlockState p_51153_, LevelReader p_51154_, BlockPos p_51155_) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockState blockstate = p_51154_.getBlockState(p_51155_.relative(direction));
         if (blockstate.isSolid() || p_51154_.getFluidState(p_51155_.relative(direction)).is(FluidTags.LAVA)) {
            return false;
         }
      }

      BlockState blockstate1 = p_51154_.getBlockState(p_51155_.below());
      return (blockstate1.is(Blocks.CACTUS) || blockstate1.is(BlockTags.SAND)) && !p_51154_.getBlockState(p_51155_.above()).liquid();
   }

   @Override
   protected void entityInside(BlockState p_51148_, Level p_51149_, BlockPos p_51150_, Entity p_51151_, InsideBlockEffectApplier p_393274_, boolean p_432042_) {
      p_51151_.hurt(p_51149_.damageSources().cactus(), 1.0F);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51164_) {
      p_51164_.add(AGE);
   }

   @Override
   protected boolean isPathfindable(BlockState p_51143_, PathComputationType p_51146_) {
      return false;
   }
}
