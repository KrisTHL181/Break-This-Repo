package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

public abstract class AbstractBedBlock extends HorizontalDirectionalBlock {
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

   public AbstractBedBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
   }

   public static @Nullable Direction getBedOrientation(final BlockGetter level, final BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      return blockState.getBlock() instanceof AbstractBedBlock ? blockState.getValue(FACING) : null;
   }

   protected abstract EnvironmentAttribute<BedRule> getBedEnvironmentAttribute();

   protected abstract InteractionResult destroyOnUse(final BlockState state, final Level level, BlockPos pos, final Player player);

   protected abstract void destroyOnLeave(final Level level, BlockPos pos);

   public BedRule getBedRule(final Level level, final BlockPos pos) {
      return level.environmentAttributes().getValue(this.getBedEnvironmentAttribute(), pos);
   }

   public Identifier getSleptInBedStatType() {
      return Stats.SLEEP_IN_BED;
   }

   public double getSleepHeight(final BlockState state, final Level level, final BlockPos pos) {
      return state.getShape(level, pos).max(Direction.Axis.Y);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, final Level level, BlockPos pos, final Player player, final BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS_SERVER;
      }

      if (state.getValue(PART) != BedPart.HEAD) {
         pos = pos.relative(state.getValue(FACING));
         state = level.getBlockState(pos);
         if (!state.is(this)) {
            return InteractionResult.CONSUME;
         }
      }

      BedRule bedRule = this.getBedRule(level, pos);
      if (bedRule.destroyOnUse()) {
         bedRule.errorMessage().ifPresent(player::sendOverlayMessage);
         return this.destroyOnUse(state, level, pos, player);
      }

      if (state.getValue(OCCUPIED)) {
         if (!this.kickVillagerOutOfBed(level, pos)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.bed.occupied"));
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         player.startSleepInBed(this, state, bedRule, pos).ifLeft(problem -> {
            if (problem.message() != null) {
               player.sendOverlayMessage(problem.message());
            }
         });
         return InteractionResult.SUCCESS_SERVER;
      }
   }

   public void onStopSleeping(final Level level, final BlockPos pos) {
      BedRule bedRule = this.getBedRule(level, pos);
      if (bedRule.destroyOnLeave()) {
         this.destroyOnLeave(level, pos);
      }
   }

   private boolean kickVillagerOutOfBed(final Level level, final BlockPos pos) {
      List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new AABB(pos), LivingEntity::isSleeping);
      if (villagers.isEmpty()) {
         return false;
      }

      villagers.get(0).stopSleeping();
      return true;
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      if (directionToNeighbour == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
         return neighbourState.is(this) && neighbourState.getValue(PART) != state.getValue(PART)
            ? state.setValue(OCCUPIED, neighbourState.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      } else {
         return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
      }
   }

   private static Direction getNeighbourDirection(final BedPart part, final Direction facing) {
      return part == BedPart.FOOT ? facing : facing.getOpposite();
   }

   @Override
   public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
      if (!level.isClientSide() && player.preventsBlockDrops()) {
         BedPart part = state.getValue(PART);
         if (part == BedPart.FOOT) {
            BlockPos headPos = pos.relative(getNeighbourDirection(part, state.getValue(FACING)));
            BlockState headState = level.getBlockState(headPos);
            if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
               level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 35);
               level.levelEvent(player, 2001, headPos, Block.getId(headState));
            }
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection();
      BlockPos pos = context.getClickedPos();
      BlockPos relative = pos.relative(facing);
      Level level = context.getLevel();
      return level.getBlockState(relative).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(relative)
         ? this.defaultBlockState().setValue(FACING, facing)
         : null;
   }

   public static Direction getConnectedDirection(final BlockState state) {
      Direction facing = state.getValue(FACING);
      return state.getValue(PART) == BedPart.HEAD ? facing.getOpposite() : facing;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(final BlockState state) {
      BedPart part = state.getValue(PART);
      return part == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(final BlockGetter level, final BlockPos pos) {
      return level.getBlockState(pos.below()).getBlock() instanceof AbstractBedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(
      final EntityType<?> type, final CollisionGetter level, final BlockPos pos, final Direction forward, final float yaw
   ) {
      Direction right = forward.getClockWise();
      Direction side = right.isFacingAngle(yaw) ? right.getOpposite() : right;
      if (isBunkBed(level, pos)) {
         return findBunkBedStandUpPosition(type, level, pos, forward, side);
      }

      int[][] offsets = bedStandUpOffsets(forward, side);
      Optional<Vec3> safePosition = findStandUpPositionAtOffset(type, level, pos, offsets, true);
      return safePosition.isPresent() ? safePosition : findStandUpPositionAtOffset(type, level, pos, offsets, false);
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(
      final EntityType<?> type, final CollisionGetter level, final BlockPos pos, final Direction forward, final Direction side
   ) {
      int[][] offsets = bedSurroundStandUpOffsets(forward, side);
      Optional<Vec3> safePosition = findStandUpPositionAtOffset(type, level, pos, offsets, true);
      if (safePosition.isPresent()) {
         return safePosition;
      }

      BlockPos below = pos.below();
      Optional<Vec3> belowSafePosition = findStandUpPositionAtOffset(type, level, below, offsets, true);
      if (belowSafePosition.isPresent()) {
         return belowSafePosition;
      }

      int[][] aboveOffsets = bedAboveStandUpOffsets(forward);
      Optional<Vec3> aboveSafePosition = findStandUpPositionAtOffset(type, level, pos, aboveOffsets, true);
      if (aboveSafePosition.isPresent()) {
         return aboveSafePosition;
      }

      Optional<Vec3> unsafePosition = findStandUpPositionAtOffset(type, level, pos, offsets, false);
      if (unsafePosition.isPresent()) {
         return unsafePosition;
      }

      Optional<Vec3> belowUnsafePosition = findStandUpPositionAtOffset(type, level, below, offsets, false);
      return belowUnsafePosition.isPresent() ? belowUnsafePosition : findStandUpPositionAtOffset(type, level, pos, aboveOffsets, false);
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(
      final EntityType<?> type, final CollisionGetter level, final BlockPos pos, final int[][] offsets, final boolean checkDangerous
   ) {
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

      for (int[] offset : offsets) {
         blockPos.set(pos.getX() + offset[0], pos.getY(), pos.getZ() + offset[1]);
         Vec3 position = DismountHelper.findSafeDismountLocation(type, level, blockPos, checkDangerous);
         if (position != null) {
            return Optional.of(position);
         }
      }

      return Optional.empty();
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, PART, OCCUPIED);
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      super.setPlacedBy(level, pos, state, by, itemStack);
      BlockPos otherPos = pos.relative(state.getValue(FACING));
      level.setBlockAndUpdate(otherPos, state.setValue(PART, BedPart.HEAD));
   }

   @Override
   protected long getSeed(final BlockState state, final BlockPos pos) {
      BlockPos sourcePos = pos.relative(state.getValue(FACING), state.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed(sourcePos.getX(), pos.getY(), sourcePos.getZ());
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   private static int[][] bedStandUpOffsets(final Direction forward, final Direction side) {
      return (int[][])ArrayUtils.addAll(bedSurroundStandUpOffsets(forward, side), bedAboveStandUpOffsets(forward));
   }

   private static int[][] bedSurroundStandUpOffsets(final Direction forward, final Direction side) {
      return new int[][]{
         {side.getStepX(), side.getStepZ()},
         {side.getStepX() - forward.getStepX(), side.getStepZ() - forward.getStepZ()},
         {side.getStepX() - forward.getStepX() * 2, side.getStepZ() - forward.getStepZ() * 2},
         {-forward.getStepX() * 2, -forward.getStepZ() * 2},
         {-side.getStepX() - forward.getStepX() * 2, -side.getStepZ() - forward.getStepZ() * 2},
         {-side.getStepX() - forward.getStepX(), -side.getStepZ() - forward.getStepZ()},
         {-side.getStepX(), -side.getStepZ()},
         {-side.getStepX() + forward.getStepX(), -side.getStepZ() + forward.getStepZ()},
         {forward.getStepX(), forward.getStepZ()},
         {side.getStepX() + forward.getStepX(), side.getStepZ() + forward.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(final Direction forward) {
      return new int[][]{{0, 0}, {-forward.getStepX(), -forward.getStepZ()}};
   }
}
