package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class LeavesBlock extends Block implements SimpleWaterloggedBlock {
   public static final int DECAY_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
   public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected final float leafParticleChance;
   private static final int TICK_DELAY = 1;
   private static boolean cutoutLeaves = true;

   @Override
   public abstract MapCodec<? extends LeavesBlock> codec();

   public LeavesBlock(float p_395099_, BlockBehaviour.Properties p_54422_) {
      super(p_54422_);
      this.leafParticleChance = p_395099_;
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(PERSISTENT, false).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean skipRendering(BlockState p_451532_, BlockState p_456220_, Direction p_460006_) {
      return !cutoutLeaves && p_456220_.getBlock() instanceof LeavesBlock ? true : super.skipRendering(p_451532_, p_456220_, p_460006_);
   }

   public static void setCutoutLeaves(boolean p_459255_) {
      cutoutLeaves = p_459255_;
   }

   @Override
   protected VoxelShape getBlockSupportShape(BlockState p_54456_, BlockGetter p_54457_, BlockPos p_54458_) {
      return Shapes.empty();
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_54449_) {
      return p_54449_.getValue(DISTANCE) == 7 && !p_54449_.getValue(PERSISTENT);
   }

   @Override
   protected void randomTick(BlockState p_221379_, ServerLevel p_221380_, BlockPos p_221381_, RandomSource p_221382_) {
      if (this.decaying(p_221379_)) {
         dropResources(p_221379_, p_221380_, p_221381_);
         p_221380_.removeBlock(p_221381_, false);
      }
   }

   protected boolean decaying(BlockState p_221386_) {
      return !p_221386_.getValue(PERSISTENT) && p_221386_.getValue(DISTANCE) == 7;
   }

   @Override
   protected void tick(BlockState p_221369_, ServerLevel p_221370_, BlockPos p_221371_, RandomSource p_221372_) {
      p_221370_.setBlock(p_221371_, updateDistance(p_221369_, p_221370_, p_221371_), 3);
   }

   @Override
   protected int getLightBlock(BlockState p_54460_) {
      return 1;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_54440_,
      LevelReader p_369206_,
      ScheduledTickAccess p_362574_,
      BlockPos p_54444_,
      Direction p_54441_,
      BlockPos p_54445_,
      BlockState p_54442_,
      RandomSource p_363861_
   ) {
      if (p_54440_.getValue(WATERLOGGED)) {
         p_362574_.scheduleTick(p_54444_, Fluids.WATER, Fluids.WATER.getTickDelay(p_369206_));
      }

      int i = getDistanceAt(p_54442_) + 1;
      if (i != 1 || p_54440_.getValue(DISTANCE) != i) {
         p_362574_.scheduleTick(p_54444_, this, 1);
      }

      return p_54440_;
   }

   private static BlockState updateDistance(BlockState p_54436_, LevelAccessor p_54437_, BlockPos p_54438_) {
      int i = 7;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.values()) {
         blockpos$mutableblockpos.setWithOffset(p_54438_, direction);
         i = Math.min(i, getDistanceAt(p_54437_.getBlockState(blockpos$mutableblockpos)) + 1);
         if (i == 1) {
            break;
         }
      }

      return p_54436_.setValue(DISTANCE, i);
   }

   private static int getDistanceAt(BlockState p_54464_) {
      return getOptionalDistanceAt(p_54464_).orElse(7);
   }

   public static OptionalInt getOptionalDistanceAt(BlockState p_277868_) {
      if (p_277868_.is(BlockTags.LOGS)) {
         return OptionalInt.of(0);
      } else {
         return p_277868_.hasProperty(DISTANCE) ? OptionalInt.of(p_277868_.getValue(DISTANCE)) : OptionalInt.empty();
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_221384_) {
      return p_221384_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_221384_);
   }

   @Override
   public void animateTick(BlockState p_221374_, Level p_221375_, BlockPos p_221376_, RandomSource p_221377_) {
      super.animateTick(p_221374_, p_221375_, p_221376_, p_221377_);
      BlockPos blockpos = p_221376_.below();
      BlockState blockstate = p_221375_.getBlockState(blockpos);
      makeDrippingWaterParticles(p_221375_, p_221376_, p_221377_, blockstate, blockpos);
      this.makeFallingLeavesParticles(p_221375_, p_221376_, p_221377_, blockstate, blockpos);
   }

   private static void makeDrippingWaterParticles(Level p_395380_, BlockPos p_398019_, RandomSource p_393134_, BlockState p_394825_, BlockPos p_393458_) {
      if (p_395380_.isRainingAt(p_398019_.above())
         && p_393134_.nextInt(15) == 1
         && (!p_394825_.canOcclude() || !p_394825_.isFaceSturdy(p_395380_, p_393458_, Direction.UP))) {
         ParticleUtils.spawnParticleBelow(p_395380_, p_398019_, p_393134_, ParticleTypes.DRIPPING_WATER);
      }
   }

   private void makeFallingLeavesParticles(Level p_392380_, BlockPos p_391974_, RandomSource p_396413_, BlockState p_391693_, BlockPos p_397349_) {
      if (!(p_396413_.nextFloat() >= this.leafParticleChance) && !isFaceFull(p_391693_.getCollisionShape(p_392380_, p_397349_), Direction.UP)) {
         this.spawnFallingLeavesParticle(p_392380_, p_391974_, p_396413_);
      }
   }

   protected abstract void spawnFallingLeavesParticle(Level var1, BlockPos var2, RandomSource var3);

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54447_) {
      p_54447_.add(DISTANCE, PERSISTENT, WATERLOGGED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54424_) {
      FluidState fluidstate = p_54424_.getLevel().getFluidState(p_54424_.getClickedPos());
      BlockState blockstate = this.defaultBlockState().setValue(PERSISTENT, true).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
      return updateDistance(blockstate, p_54424_.getLevel(), p_54424_.getClickedPos());
   }
}
