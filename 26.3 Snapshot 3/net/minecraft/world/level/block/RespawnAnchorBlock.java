package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RespawnAnchorBlock extends Block {
   public static final int MIN_CHARGES = 0;
   public static final int MAX_CHARGES = 4;
   public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
   private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(
      new Vec3i(0, 0, -1),
      new Vec3i(-1, 0, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(-1, 0, -1),
      new Vec3i(1, 0, -1),
      new Vec3i(-1, 0, 1),
      new Vec3i(1, 0, 1)
   );
   private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new Builder()
      .addAll(RESPAWN_HORIZONTAL_OFFSETS)
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
      .add(new Vec3i(0, 1, 0))
      .build();

   public RespawnAnchorBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
   }

   @Override
   protected InteractionResult useItemOn(
      final ItemStack itemStack,
      final BlockState state,
      final Level level,
      final BlockPos pos,
      final Player player,
      final InteractionHand hand,
      final BlockHitResult hitResult
   ) {
      if (isRespawnFuel(itemStack) && canBeCharged(state)) {
         charge(player, level, pos, state);
         itemStack.consume(1, player);
         return InteractionResult.SUCCESS;
      } else {
         return hand == InteractionHand.MAIN_HAND && isRespawnFuel(player.getItemInHand(InteractionHand.OFF_HAND)) && canBeCharged(state)
            ? InteractionResult.PASS
            : InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (state.getValue(CHARGE) == 0) {
         return InteractionResult.PASS;
      }

      if (level instanceof ServerLevel serverLevel) {
         if (!canSetSpawn(serverLevel, pos)) {
            this.explode(state, serverLevel, pos);
            return InteractionResult.SUCCESS_SERVER;
         }

         if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayer.RespawnConfig respawnConfig = serverPlayer.getRespawnConfig();
            ServerPlayer.RespawnConfig newRespawnConfig = new ServerPlayer.RespawnConfig(
               LevelData.RespawnData.of(serverLevel.dimension(), pos, 0.0F, 0.0F), false
            );
            if (respawnConfig == null || !respawnConfig.isSamePosition(newRespawnConfig)) {
               serverPlayer.setRespawnPosition(newRespawnConfig, true);
               serverLevel.playSound(
                  null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F
               );
               return InteractionResult.SUCCESS_SERVER;
            }
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   private static boolean isRespawnFuel(final ItemStack itemInHand) {
      return itemInHand.is(Items.GLOWSTONE);
   }

   private static boolean canBeCharged(final BlockState state) {
      return state.getValue(CHARGE) < 4;
   }

   private static boolean isWaterThatWouldFlow(final BlockPos pos, final Level level) {
      FluidState fluid = level.getFluidState(pos);
      if (!fluid.is(FluidTags.WATER)) {
         return false;
      }

      if (fluid.isSource()) {
         return true;
      }

      float amount = fluid.getAmount();
      if (amount < 2.0F) {
         return false;
      }

      FluidState fluidBelow = level.getFluidState(pos.below());
      return !fluidBelow.is(FluidTags.WATER);
   }

   private void explode(final BlockState state, final ServerLevel level, final BlockPos pos) {
      level.removeBlock(pos, false);
      boolean anyWaterNeighbors = Direction.Plane.HORIZONTAL.stream().map(pos::relative).anyMatch(neighborPos -> isWaterThatWouldFlow(neighborPos, level));
      final boolean inWater = anyWaterNeighbors || level.getFluidState(pos.above()).is(FluidTags.WATER);
      ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator() {
         @Override
         public Optional<Float> getBlockExplosionResistance(
            final Explosion explosion, final BlockGetter levelx, final BlockPos testPos, final BlockState block, final FluidState fluid
         ) {
            return testPos.equals(pos) && inWater
               ? Optional.of(Blocks.WATER.getExplosionResistance())
               : super.getBlockExplosionResistance(explosion, levelx, testPos, block, fluid);
         }
      };
      Vec3 boomPos = Vec3.atCenterOf(pos);
      level.explode(null, level.damageSources().badRespawnPointExplosion(boomPos), damageCalculator, boomPos, 5.0F, true, Level.ExplosionInteraction.BLOCK);
   }

   public static boolean canSetSpawn(final ServerLevel level, final BlockPos pos) {
      return level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, pos);
   }

   public static void charge(final @Nullable Entity sourceEntity, final Level level, final BlockPos pos, final BlockState state) {
      BlockState newState = state.setValue(CHARGE, state.getValue(CHARGE) + 1);
      level.setBlockAndUpdate(pos, newState);
      level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
      level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(CHARGE) != 0) {
         if (random.nextInt(100) == 0) {
            level.playLocalSound(pos, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         double x = pos.getX() + 0.5 + (0.5 - random.nextDouble());
         double y = pos.getY() + 1.0;
         double z = pos.getZ() + 0.5 + (0.5 - random.nextDouble());
         double ya = random.nextFloat() * 0.04;
         level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, ya, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(CHARGE);
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   public static int getScaledChargeLevel(final BlockState state, final int maximum) {
      return Mth.floor((state.getValue(CHARGE) - 0) / 4.0F * maximum);
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return getScaledChargeLevel(state, 15);
   }

   public static Optional<Vec3> findStandUpPosition(final EntityType<?> type, final CollisionGetter level, final BlockPos pos) {
      Optional<Vec3> safePosition = findStandUpPosition(type, level, pos, true);
      return safePosition.isPresent() ? safePosition : findStandUpPosition(type, level, pos, false);
   }

   private static Optional<Vec3> findStandUpPosition(final EntityType<?> type, final CollisionGetter level, final BlockPos pos, final boolean checkDangerous) {
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var5 = RESPAWN_OFFSETS.iterator();

      while (var5.hasNext()) {
         Vec3i offset = (Vec3i)var5.next();
         blockPos.set(pos).move(offset);
         Vec3 position = DismountHelper.findSafeDismountLocation(type, level, blockPos, checkDangerous);
         if (position != null) {
            return Optional.of(position);
         }
      }

      return Optional.empty();
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
