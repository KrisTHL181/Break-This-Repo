package net.minecraft.world.level.block;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CreakingHeartBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
   public static final EnumProperty<CreakingHeartState> STATE = BlockStateProperties.CREAKING_HEART_STATE;
   public static final BooleanProperty NATURAL = BlockStateProperties.NATURAL;

   protected CreakingHeartBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(STATE, CreakingHeartState.UPROOTED).setValue(NATURAL, false));
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new CreakingHeartBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      if (level.isClientSide()) {
         return null;
      } else {
         return blockState.getValue(STATE) != CreakingHeartState.UPROOTED
            ? createTickerHelper(type, BlockEntityTypes.CREAKING_HEART, CreakingHeartBlockEntity::serverTick)
            : null;
      }
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos)) {
         if (state.getValue(STATE) != CreakingHeartState.UPROOTED) {
            if (random.nextInt(16) == 0 && isSurroundedByLogs(level, pos)) {
               level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.CREAKING_HEART_IDLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
         }
      }
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
      ticks.scheduleTick(pos, this, 1);
      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      BlockState newState = updateState(state, level, pos);
      if (newState != state) {
         level.setBlockAndUpdate(pos, newState);
      }
   }

   private static BlockState updateState(final BlockState state, final Level level, final BlockPos pos) {
      boolean hasLogs = hasRequiredLogs(state, level, pos);
      boolean disabled = state.getValue(STATE) == CreakingHeartState.UPROOTED;
      return hasLogs && disabled
         ? state.setValue(
            STATE, level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos) ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT
         )
         : state;
   }

   public static boolean hasRequiredLogs(final BlockState state, final LevelReader level, final BlockPos pos) {
      Direction.Axis axis = state.getValue(AXIS);

      for (Direction dir : axis.getDirections()) {
         BlockState neigbour = level.getBlockState(pos.relative(dir));
         if (!neigbour.is(BlockTags.PALE_OAK_LOGS) || neigbour.getValue(AXIS) != axis) {
            return false;
         }
      }

      return true;
   }

   private static boolean isSurroundedByLogs(final LevelAccessor level, final BlockPos pos) {
      for (Direction dir : Direction.values()) {
         BlockPos neighbourPos = pos.relative(dir);
         BlockState neighbourState = level.getBlockState(neighbourPos);
         if (!neighbourState.is(BlockTags.PALE_OAK_LOGS)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      return updateState(this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()), context.getLevel(), context.getClickedPos());
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return RotatedPillarBlock.rotatePillar(state, rotation);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AXIS, STATE, NATURAL);
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      Containers.updateNeighboursAfterDestroy(state, level, pos);
   }

   @Override
   protected void onExplosionHit(
      final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
   ) {
      if (level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity
         && explosion instanceof ServerExplosion serverExplosion
         && explosion.getBlockInteraction().shouldAffectBlocklikeEntities()) {
         creakingHeartBlockEntity.removeProtector(serverExplosion.getDamageSource());
         if (explosion.getIndirectSourceEntity() instanceof Player player && explosion.getBlockInteraction().shouldAffectBlocklikeEntities()) {
            this.tryAwardExperience(player, state, level, pos);
         }
      }

      super.onExplosionHit(state, level, pos, explosion, onHit);
   }

   @Override
   public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
      if (level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity) {
         creakingHeartBlockEntity.removeProtector(player.damageSources().playerAttack(player));
         this.tryAwardExperience(player, state, level, pos);
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   private void tryAwardExperience(final Player player, final BlockState state, final Level level, final BlockPos pos) {
      if (!player.preventsBlockDrops() && !player.isSpectator() && state.getValue(NATURAL) && level instanceof ServerLevel serverLevel) {
         this.popExperience(serverLevel, pos, level.getRandom().nextIntBetweenInclusive(20, 24));
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      if (state.getValue(STATE) == CreakingHeartState.UPROOTED) {
         return 0;
      } else {
         return level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity ? creakingHeartBlockEntity.getAnalogOutputSignal() : 0;
      }
   }
}
