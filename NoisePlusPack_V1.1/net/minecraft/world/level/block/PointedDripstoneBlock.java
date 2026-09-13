package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
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
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PointedDripstoneBlock extends Block implements Fallable, SimpleWaterloggedBlock {
   public static final MapCodec<PointedDripstoneBlock> CODEC = simpleCodec(PointedDripstoneBlock::new);
   public static final EnumProperty<Direction> TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
   public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
   private static final int DELAY_BEFORE_FALLING = 2;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
   private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
   private static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
   private static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
   private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_DRIPSTONE = 0.6;
   private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
   private static final int STALACTITE_MAX_DAMAGE = 40;
   private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
   private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.5F;
   private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
   private static final float AVERAGE_DAYS_PER_GROWTH = 5.0F;
   private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778F;
   private static final int MAX_GROWTH_LENGTH = 7;
   private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
   private static final VoxelShape SHAPE_TIP_MERGE = Block.column(6.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_TIP_UP = Block.column(6.0, 0.0, 11.0);
   private static final VoxelShape SHAPE_TIP_DOWN = Block.column(6.0, 5.0, 16.0);
   private static final VoxelShape SHAPE_FRUSTUM = Block.column(8.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_MIDDLE = Block.column(10.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_BASE = Block.column(12.0, 0.0, 16.0);
   private static final double STALACTITE_DRIP_START_PIXEL = SHAPE_TIP_DOWN.min(Direction.Axis.Y);
   private static final float MAX_HORIZONTAL_OFFSET = (float)SHAPE_BASE.min(Direction.Axis.X);
   private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.column(4.0, 0.0, 16.0);

   @Override
   public MapCodec<PointedDripstoneBlock> codec() {
      return CODEC;
   }

   public PointedDripstoneBlock(BlockBehaviour.Properties p_154025_) {
      super(p_154025_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(TIP_DIRECTION, Direction.UP).setValue(THICKNESS, DripstoneThickness.TIP).setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_154157_) {
      p_154157_.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
   }

   @Override
   protected boolean canSurvive(BlockState p_154137_, LevelReader p_154138_, BlockPos p_154139_) {
      return isValidPointedDripstonePlacement(p_154138_, p_154139_, p_154137_.getValue(TIP_DIRECTION));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_154147_,
      LevelReader p_366971_,
      ScheduledTickAccess p_370151_,
      BlockPos p_154151_,
      Direction p_154148_,
      BlockPos p_154152_,
      BlockState p_154149_,
      RandomSource p_362128_
   ) {
      if (p_154147_.getValue(WATERLOGGED)) {
         p_370151_.scheduleTick(p_154151_, Fluids.WATER, Fluids.WATER.getTickDelay(p_366971_));
      }

      if (p_154148_ != Direction.UP && p_154148_ != Direction.DOWN) {
         return p_154147_;
      }

      Direction direction = p_154147_.getValue(TIP_DIRECTION);
      if (direction == Direction.DOWN && p_370151_.getBlockTicks().hasScheduledTick(p_154151_, this)) {
         return p_154147_;
      }

      if (p_154148_ == direction.getOpposite() && !this.canSurvive(p_154147_, p_366971_, p_154151_)) {
         if (direction == Direction.DOWN) {
            p_370151_.scheduleTick(p_154151_, this, 2);
         } else {
            p_370151_.scheduleTick(p_154151_, this, 1);
         }

         return p_154147_;
      } else {
         boolean flag = p_154147_.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
         DripstoneThickness dripstonethickness = calculateDripstoneThickness(p_366971_, p_154151_, direction, flag);
         return p_154147_.setValue(THICKNESS, dripstonethickness);
      }
   }

   @Override
   protected void onProjectileHit(Level p_154042_, BlockState p_154043_, BlockHitResult p_154044_, Projectile p_154045_) {
      if (!p_154042_.isClientSide()) {
         BlockPos blockpos = p_154044_.getBlockPos();
         if (p_154042_ instanceof ServerLevel serverlevel
            && p_154045_.mayInteract(serverlevel, blockpos)
            && p_154045_.mayBreak(serverlevel)
            && p_154045_ instanceof ThrownTrident
            && p_154045_.getDeltaMovement().length() > 0.6) {
            p_154042_.destroyBlock(blockpos, true);
         }
      }
   }

   @Override
   public void fallOn(Level p_154047_, BlockState p_154048_, BlockPos p_154049_, Entity p_154050_, double p_392896_) {
      if (p_154048_.getValue(TIP_DIRECTION) == Direction.UP && p_154048_.getValue(THICKNESS) == DripstoneThickness.TIP) {
         p_154050_.causeFallDamage(p_392896_ + 2.5, 2.0F, p_154047_.damageSources().stalagmite());
      } else {
         super.fallOn(p_154047_, p_154048_, p_154049_, p_154050_, p_392896_);
      }
   }

   @Override
   public void animateTick(BlockState p_221870_, Level p_221871_, BlockPos p_221872_, RandomSource p_221873_) {
      if (canDrip(p_221870_)) {
         float f = p_221873_.nextFloat();
         if (!(f > 0.12F)) {
            getFluidAboveStalactite(p_221871_, p_221872_, p_221870_)
               .filter(p_221848_ -> f < 0.02F || canFillCauldron(p_221848_.fluid))
               .ifPresent(p_449903_ -> spawnDripParticle(p_221871_, p_221872_, p_221870_, p_449903_.fluid, p_449903_.pos));
         }
      }
   }

   @Override
   protected void tick(BlockState p_221865_, ServerLevel p_221866_, BlockPos p_221867_, RandomSource p_221868_) {
      if (isStalagmite(p_221865_) && !this.canSurvive(p_221865_, p_221866_, p_221867_)) {
         p_221866_.destroyBlock(p_221867_, true);
      } else {
         spawnFallingStalactite(p_221865_, p_221866_, p_221867_);
      }
   }

   @Override
   protected void randomTick(BlockState p_221883_, ServerLevel p_221884_, BlockPos p_221885_, RandomSource p_221886_) {
      maybeTransferFluid(p_221883_, p_221884_, p_221885_, p_221886_.nextFloat());
      if (p_221886_.nextFloat() < 0.011377778F && isStalactiteStartPos(p_221883_, p_221884_, p_221885_)) {
         growStalactiteOrStalagmiteIfPossible(p_221883_, p_221884_, p_221885_, p_221886_);
      }
   }

   @VisibleForTesting
   public static void maybeTransferFluid(BlockState p_221860_, ServerLevel p_221861_, BlockPos p_221862_, float p_221863_) {
      if ((!(p_221863_ > 0.17578125F) || !(p_221863_ > 0.05859375F)) && isStalactiteStartPos(p_221860_, p_221861_, p_221862_)) {
         Optional<PointedDripstoneBlock.FluidInfo> optional = getFluidAboveStalactite(p_221861_, p_221862_, p_221860_);
         if (!optional.isEmpty()) {
            Fluid fluid = optional.get().fluid;
            float f;
            if (fluid == Fluids.WATER) {
               f = 0.17578125F;
            } else {
               if (fluid != Fluids.LAVA) {
                  return;
               }

               f = 0.05859375F;
            }

            if (!(p_221863_ >= f)) {
               BlockPos blockpos = findTip(p_221860_, p_221861_, p_221862_, 11, false);
               if (blockpos != null) {
                  if (optional.get().sourceState.is(Blocks.MUD) && fluid == Fluids.WATER) {
                     BlockState blockstate1 = Blocks.CLAY.defaultBlockState();
                     p_221861_.setBlockAndUpdate(optional.get().pos, blockstate1);
                     Block.pushEntitiesUp(optional.get().sourceState, blockstate1, p_221861_, optional.get().pos);
                     p_221861_.gameEvent(GameEvent.BLOCK_CHANGE, optional.get().pos, GameEvent.Context.of(blockstate1));
                     p_221861_.levelEvent(1504, blockpos, 0);
                  } else {
                     BlockPos blockpos1 = findFillableCauldronBelowStalactiteTip(p_221861_, blockpos, fluid);
                     if (blockpos1 != null) {
                        p_221861_.levelEvent(1504, blockpos, 0);
                        int i = blockpos.getY() - blockpos1.getY();
                        int j = 50 + i;
                        BlockState blockstate = p_221861_.getBlockState(blockpos1);
                        p_221861_.scheduleTick(blockpos1, blockstate.getBlock(), j);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_154040_) {
      LevelAccessor levelaccessor = p_154040_.getLevel();
      BlockPos blockpos = p_154040_.getClickedPos();
      Direction direction = p_154040_.getNearestLookingVerticalDirection().getOpposite();
      Direction direction1 = calculateTipDirection(levelaccessor, blockpos, direction);
      if (direction1 == null) {
         return null;
      }

      boolean flag = !p_154040_.isSecondaryUseActive();
      DripstoneThickness dripstonethickness = calculateDripstoneThickness(levelaccessor, blockpos, direction1, flag);
      return this.defaultBlockState()
         .setValue(TIP_DIRECTION, direction1)
         .setValue(THICKNESS, dripstonethickness)
         .setValue(WATERLOGGED, levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER);
   }

   @Override
   protected FluidState getFluidState(BlockState p_154235_) {
      return p_154235_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_154235_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_154117_, BlockGetter p_154118_, BlockPos p_154119_, CollisionContext p_154120_) {
      VoxelShape voxelshape = switch ((DripstoneThickness)p_154117_.getValue(THICKNESS)) {
         case TIP_MERGE -> SHAPE_TIP_MERGE;
         case TIP -> p_154117_.getValue(TIP_DIRECTION) == Direction.DOWN ? SHAPE_TIP_DOWN : SHAPE_TIP_UP;
         case FRUSTUM -> SHAPE_FRUSTUM;
         case MIDDLE -> SHAPE_MIDDLE;
         case BASE -> SHAPE_BASE;
      };
      return voxelshape.move(p_154117_.getOffset(p_154119_));
   }

   @Override
   protected boolean isCollisionShapeFullBlock(BlockState p_181235_, BlockGetter p_181236_, BlockPos p_181237_) {
      return false;
   }

   @Override
   protected float getMaxHorizontalOffset() {
      return MAX_HORIZONTAL_OFFSET;
   }

   @Override
   public void onBrokenAfterFall(Level p_154059_, BlockPos p_154060_, FallingBlockEntity p_154061_) {
      if (!p_154061_.isSilent()) {
         p_154059_.levelEvent(1045, p_154060_, 0);
      }
   }

   @Override
   public DamageSource getFallDamageSource(Entity p_254432_) {
      return p_254432_.damageSources().fallingStalactite(p_254432_);
   }

   private static void spawnFallingStalactite(BlockState p_154098_, ServerLevel p_154099_, BlockPos p_154100_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_154100_.mutable();
      BlockState blockstate = p_154098_;

      while (isStalactite(blockstate)) {
         FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(p_154099_, blockpos$mutableblockpos, blockstate);
         if (isTip(blockstate, true)) {
            int i = Math.max(1 + p_154100_.getY() - blockpos$mutableblockpos.getY(), 6);
            float f = 1.0F * i;
            fallingblockentity.setHurtsEntities(f, 40);
            break;
         }

         blockpos$mutableblockpos.move(Direction.DOWN);
         blockstate = p_154099_.getBlockState(blockpos$mutableblockpos);
      }
   }

   @VisibleForTesting
   public static void growStalactiteOrStalagmiteIfPossible(BlockState p_221888_, ServerLevel p_221889_, BlockPos p_221890_, RandomSource p_221891_) {
      BlockState blockstate = p_221889_.getBlockState(p_221890_.above(1));
      BlockState blockstate1 = p_221889_.getBlockState(p_221890_.above(2));
      if (canGrow(blockstate, blockstate1)) {
         BlockPos blockpos = findTip(p_221888_, p_221889_, p_221890_, 7, false);
         if (blockpos != null) {
            BlockState blockstate2 = p_221889_.getBlockState(blockpos);
            if (canDrip(blockstate2) && canTipGrow(blockstate2, p_221889_, blockpos)) {
               if (p_221891_.nextBoolean()) {
                  grow(p_221889_, blockpos, Direction.DOWN);
               } else {
                  growStalagmiteBelow(p_221889_, blockpos);
               }
            }
         }
      }
   }

   private static void growStalagmiteBelow(ServerLevel p_154033_, BlockPos p_154034_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_154034_.mutable();

      for (int i = 0; i < 10; i++) {
         blockpos$mutableblockpos.move(Direction.DOWN);
         BlockState blockstate = p_154033_.getBlockState(blockpos$mutableblockpos);
         if (!blockstate.getFluidState().isEmpty()) {
            return;
         }

         if (isUnmergedTipWithDirection(blockstate, Direction.UP) && canTipGrow(blockstate, p_154033_, blockpos$mutableblockpos)) {
            grow(p_154033_, blockpos$mutableblockpos, Direction.UP);
            return;
         }

         if (isValidPointedDripstonePlacement(p_154033_, blockpos$mutableblockpos, Direction.UP) && !p_154033_.isWaterAt(blockpos$mutableblockpos.below())) {
            grow(p_154033_, blockpos$mutableblockpos.below(), Direction.UP);
            return;
         }

         if (!canDripThrough(p_154033_, blockpos$mutableblockpos, blockstate)) {
            return;
         }
      }
   }

   private static void grow(ServerLevel p_154036_, BlockPos p_154037_, Direction p_154038_) {
      BlockPos blockpos = p_154037_.relative(p_154038_);
      BlockState blockstate = p_154036_.getBlockState(blockpos);
      if (isUnmergedTipWithDirection(blockstate, p_154038_.getOpposite())) {
         createMergedTips(blockstate, p_154036_, blockpos);
      } else if (blockstate.isAir() || blockstate.is(Blocks.WATER)) {
         createDripstone(p_154036_, blockpos, p_154038_, DripstoneThickness.TIP);
      }
   }

   private static void createDripstone(LevelAccessor p_154088_, BlockPos p_154089_, Direction p_154090_, DripstoneThickness p_154091_) {
      BlockState blockstate = Blocks.POINTED_DRIPSTONE
         .defaultBlockState()
         .setValue(TIP_DIRECTION, p_154090_)
         .setValue(THICKNESS, p_154091_)
         .setValue(WATERLOGGED, p_154088_.getFluidState(p_154089_).getType() == Fluids.WATER);
      p_154088_.setBlock(p_154089_, blockstate, 3);
   }

   private static void createMergedTips(BlockState p_154231_, LevelAccessor p_154232_, BlockPos p_154233_) {
      BlockPos blockpos;
      BlockPos blockpos1;
      if (p_154231_.getValue(TIP_DIRECTION) == Direction.UP) {
         blockpos1 = p_154233_;
         blockpos = p_154233_.above();
      } else {
         blockpos = p_154233_;
         blockpos1 = p_154233_.below();
      }

      createDripstone(p_154232_, blockpos, Direction.DOWN, DripstoneThickness.TIP_MERGE);
      createDripstone(p_154232_, blockpos1, Direction.UP, DripstoneThickness.TIP_MERGE);
   }

   public static void spawnDripParticle(Level p_154063_, BlockPos p_154064_, BlockState p_154065_) {
      getFluidAboveStalactite(p_154063_, p_154064_, p_154065_)
         .ifPresent(p_449899_ -> spawnDripParticle(p_154063_, p_154064_, p_154065_, p_449899_.fluid, p_449899_.pos));
   }

   private static void spawnDripParticle(Level p_154072_, BlockPos p_154073_, BlockState p_154074_, Fluid p_154075_, BlockPos p_456775_) {
      Vec3 vec3 = p_154074_.getOffset(p_154073_);
      double d0 = 0.0625;
      double d1 = p_154073_.getX() + 0.5 + vec3.x;
      double d2 = p_154073_.getY() + STALACTITE_DRIP_START_PIXEL - 0.0625;
      double d3 = p_154073_.getZ() + 0.5 + vec3.z;
      ParticleOptions particleoptions = getDripParticle(p_154072_, p_154075_, p_456775_);
      p_154072_.addParticle(particleoptions, d1, d2, d3, 0.0, 0.0, 0.0);
   }

   private static @Nullable BlockPos findTip(BlockState p_154131_, LevelAccessor p_154132_, BlockPos p_154133_, int p_154134_, boolean p_154135_) {
      if (isTip(p_154131_, p_154135_)) {
         return p_154133_;
      }

      Direction direction = p_154131_.getValue(TIP_DIRECTION);
      BiPredicate<BlockPos, BlockState> bipredicate = (p_360445_, p_360446_) -> p_360446_.is(Blocks.POINTED_DRIPSTONE)
         && p_360446_.getValue(TIP_DIRECTION) == direction;
      return findBlockVertical(p_154132_, p_154133_, direction.getAxisDirection(), bipredicate, p_154168_ -> isTip(p_154168_, p_154135_), p_154134_)
         .orElse(null);
   }

   private static @Nullable Direction calculateTipDirection(LevelReader p_154191_, BlockPos p_154192_, Direction p_154193_) {
      Direction direction;
      if (isValidPointedDripstonePlacement(p_154191_, p_154192_, p_154193_)) {
         direction = p_154193_;
      } else {
         if (!isValidPointedDripstonePlacement(p_154191_, p_154192_, p_154193_.getOpposite())) {
            return null;
         }

         direction = p_154193_.getOpposite();
      }

      return direction;
   }

   private static DripstoneThickness calculateDripstoneThickness(LevelReader p_154093_, BlockPos p_154094_, Direction p_154095_, boolean p_154096_) {
      Direction direction = p_154095_.getOpposite();
      BlockState blockstate = p_154093_.getBlockState(p_154094_.relative(p_154095_));
      if (!isPointedDripstoneWithDirection(blockstate, direction)) {
         if (!isPointedDripstoneWithDirection(blockstate, p_154095_)) {
            return DripstoneThickness.TIP;
         } else {
            DripstoneThickness dripstonethickness = blockstate.getValue(THICKNESS);
            if (dripstonethickness != DripstoneThickness.TIP && dripstonethickness != DripstoneThickness.TIP_MERGE) {
               BlockState blockstate1 = p_154093_.getBlockState(p_154094_.relative(direction));
               return !isPointedDripstoneWithDirection(blockstate1, p_154095_) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            } else {
               return DripstoneThickness.FRUSTUM;
            }
         }
      } else {
         return !p_154096_ && blockstate.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
      }
   }

   public static boolean canDrip(BlockState p_154239_) {
      return isStalactite(p_154239_) && p_154239_.getValue(THICKNESS) == DripstoneThickness.TIP && !p_154239_.getValue(WATERLOGGED);
   }

   private static boolean canTipGrow(BlockState p_154195_, ServerLevel p_154196_, BlockPos p_154197_) {
      Direction direction = p_154195_.getValue(TIP_DIRECTION);
      BlockPos blockpos = p_154197_.relative(direction);
      BlockState blockstate = p_154196_.getBlockState(blockpos);
      if (!blockstate.getFluidState().isEmpty()) {
         return false;
      } else {
         return blockstate.isAir() ? true : isUnmergedTipWithDirection(blockstate, direction.getOpposite());
      }
   }

   private static Optional<BlockPos> findRootBlock(Level p_154067_, BlockPos p_154068_, BlockState p_154069_, int p_154070_) {
      Direction direction = p_154069_.getValue(TIP_DIRECTION);
      BiPredicate<BlockPos, BlockState> bipredicate = (p_360442_, p_360443_) -> p_360443_.is(Blocks.POINTED_DRIPSTONE)
         && p_360443_.getValue(TIP_DIRECTION) == direction;
      return findBlockVertical(
         p_154067_, p_154068_, direction.getOpposite().getAxisDirection(), bipredicate, p_154245_ -> !p_154245_.is(Blocks.POINTED_DRIPSTONE), p_154070_
      );
   }

   private static boolean isValidPointedDripstonePlacement(LevelReader p_154222_, BlockPos p_154223_, Direction p_154224_) {
      BlockPos blockpos = p_154223_.relative(p_154224_.getOpposite());
      BlockState blockstate = p_154222_.getBlockState(blockpos);
      return blockstate.isFaceSturdy(p_154222_, blockpos, p_154224_) || isPointedDripstoneWithDirection(blockstate, p_154224_);
   }

   private static boolean isTip(BlockState p_154154_, boolean p_154155_) {
      if (!p_154154_.is(Blocks.POINTED_DRIPSTONE)) {
         return false;
      }

      DripstoneThickness dripstonethickness = p_154154_.getValue(THICKNESS);
      return dripstonethickness == DripstoneThickness.TIP || p_154155_ && dripstonethickness == DripstoneThickness.TIP_MERGE;
   }

   private static boolean isUnmergedTipWithDirection(BlockState p_154144_, Direction p_154145_) {
      return isTip(p_154144_, false) && p_154144_.getValue(TIP_DIRECTION) == p_154145_;
   }

   private static boolean isStalactite(BlockState p_154241_) {
      return isPointedDripstoneWithDirection(p_154241_, Direction.DOWN);
   }

   private static boolean isStalagmite(BlockState p_154243_) {
      return isPointedDripstoneWithDirection(p_154243_, Direction.UP);
   }

   private static boolean isStalactiteStartPos(BlockState p_154204_, LevelReader p_154205_, BlockPos p_154206_) {
      return isStalactite(p_154204_) && !p_154205_.getBlockState(p_154206_.above()).is(Blocks.POINTED_DRIPSTONE);
   }

   @Override
   protected boolean isPathfindable(BlockState p_154112_, PathComputationType p_154115_) {
      return false;
   }

   private static boolean isPointedDripstoneWithDirection(BlockState p_154208_, Direction p_154209_) {
      return p_154208_.is(Blocks.POINTED_DRIPSTONE) && p_154208_.getValue(TIP_DIRECTION) == p_154209_;
   }

   private static @Nullable BlockPos findFillableCauldronBelowStalactiteTip(Level p_154077_, BlockPos p_154078_, Fluid p_154079_) {
      Predicate<BlockState> predicate = p_154162_ -> p_154162_.getBlock() instanceof AbstractCauldronBlock
         && ((AbstractCauldronBlock)p_154162_.getBlock()).canReceiveStalactiteDrip(p_154079_);
      BiPredicate<BlockPos, BlockState> bipredicate = (p_202034_, p_202035_) -> canDripThrough(p_154077_, p_202034_, p_202035_);
      return findBlockVertical(p_154077_, p_154078_, Direction.DOWN.getAxisDirection(), bipredicate, predicate, 11).orElse(null);
   }

   public static @Nullable BlockPos findStalactiteTipAboveCauldron(Level p_154056_, BlockPos p_154057_) {
      BiPredicate<BlockPos, BlockState> bipredicate = (p_202030_, p_202031_) -> canDripThrough(p_154056_, p_202030_, p_202031_);
      return findBlockVertical(p_154056_, p_154057_, Direction.UP.getAxisDirection(), bipredicate, PointedDripstoneBlock::canDrip, 11).orElse(null);
   }

   public static Fluid getCauldronFillFluidType(ServerLevel p_221850_, BlockPos p_221851_) {
      return getFluidAboveStalactite(p_221850_, p_221851_, p_221850_.getBlockState(p_221851_))
         .map(p_221858_ -> p_221858_.fluid)
         .filter(PointedDripstoneBlock::canFillCauldron)
         .orElse(Fluids.EMPTY);
   }

   private static Optional<PointedDripstoneBlock.FluidInfo> getFluidAboveStalactite(Level p_154182_, BlockPos p_154183_, BlockState p_154184_) {
      return !isStalactite(p_154184_) ? Optional.empty() : findRootBlock(p_154182_, p_154183_, p_154184_, 11).map(p_449895_ -> {
         BlockPos blockpos = p_449895_.above();
         BlockState blockstate = p_154182_.getBlockState(blockpos);
         Fluid fluid;
         if (blockstate.is(Blocks.MUD) && !p_154182_.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, blockpos)) {
            fluid = Fluids.WATER;
         } else {
            fluid = p_154182_.getFluidState(blockpos).getType();
         }

         return new PointedDripstoneBlock.FluidInfo(blockpos, fluid, blockstate);
      });
   }

   private static boolean canFillCauldron(Fluid p_154159_) {
      return p_154159_ == Fluids.LAVA || p_154159_ == Fluids.WATER;
   }

   private static boolean canGrow(BlockState p_154141_, BlockState p_154142_) {
      return p_154141_.is(Blocks.DRIPSTONE_BLOCK) && p_154142_.is(Blocks.WATER) && p_154142_.getFluidState().isSource();
   }

   private static ParticleOptions getDripParticle(Level p_456779_, Fluid p_459241_, BlockPos p_452600_) {
      if (p_459241_.isSame(Fluids.EMPTY)) {
         return p_456779_.environmentAttributes().getValue(EnvironmentAttributes.DEFAULT_DRIPSTONE_PARTICLE, p_452600_);
      } else {
         return p_459241_.is(FluidTags.LAVA) ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
      }
   }

   private static Optional<BlockPos> findBlockVertical(
      LevelAccessor p_202007_,
      BlockPos p_202008_,
      Direction.AxisDirection p_202009_,
      BiPredicate<BlockPos, BlockState> p_202010_,
      Predicate<BlockState> p_202011_,
      int p_202012_
   ) {
      Direction direction = Direction.get(p_202009_, Direction.Axis.Y);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_202008_.mutable();

      for (int i = 1; i < p_202012_; i++) {
         blockpos$mutableblockpos.move(direction);
         BlockState blockstate = p_202007_.getBlockState(blockpos$mutableblockpos);
         if (p_202011_.test(blockstate)) {
            return Optional.of(blockpos$mutableblockpos.immutable());
         }

         if (p_202007_.isOutsideBuildHeight(blockpos$mutableblockpos.getY()) || !p_202010_.test(blockpos$mutableblockpos, blockstate)) {
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static boolean canDripThrough(BlockGetter p_202018_, BlockPos p_202019_, BlockState p_202020_) {
      if (p_202020_.isAir()) {
         return true;
      }

      if (p_202020_.isSolidRender()) {
         return false;
      }

      if (!p_202020_.getFluidState().isEmpty()) {
         return false;
      }

      VoxelShape voxelshape = p_202020_.getCollisionShape(p_202018_, p_202019_);
      return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, voxelshape, BooleanOp.AND);
   }

   record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
   }
}
