package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SculkShriekerBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
   private static final int WARNING_SOUND_RADIUS = 10;
   private static final int WARDEN_SPAWN_ATTEMPTS = 20;
   private static final int WARDEN_SPAWN_RANGE_XZ = 5;
   private static final int WARDEN_SPAWN_RANGE_Y = 6;
   private static final int DARKNESS_RADIUS = 40;
   private static final int SHRIEKING_TICKS = 90;
   private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = Util.make(new Int2ObjectOpenHashMap(), map -> {
      map.put(1, SoundEvents.WARDEN_NEARBY_CLOSE);
      map.put(2, SoundEvents.WARDEN_NEARBY_CLOSER);
      map.put(3, SoundEvents.WARDEN_NEARBY_CLOSEST);
      map.put(4, SoundEvents.WARDEN_LISTENING_ANGRY);
   });
   private static final int DEFAULT_WARNING_LEVEL = 0;
   private int warningLevel = 0;
   private final VibrationSystem.User vibrationUser = new SculkShriekerBlockEntity.VibrationUser();
   private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
   private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

   public SculkShriekerBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.SCULK_SHRIEKER, worldPosition, blockState);
   }

   @Override
   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   @Override
   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.warningLevel = input.getIntOr("warning_level", 0);
      this.vibrationData = input.<VibrationSystem.Data>read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("warning_level", this.warningLevel);
      output.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
   }

   public static @Nullable ServerPlayer tryGetPlayer(final @Nullable Entity sourceEntity) {
      if (sourceEntity instanceof ServerPlayer player) {
         return player;
      } else if (sourceEntity != null && sourceEntity.getControllingPassenger() instanceof ServerPlayer player) {
         return player;
      } else if (sourceEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
         return player;
      } else {
         return sourceEntity instanceof ItemEntity item && item.getOwner() instanceof ServerPlayer player ? player : null;
      }
   }

   public void tryShriek(final ServerLevel level, final @Nullable ServerPlayer player) {
      if (player != null) {
         BlockState state = this.getBlockState();
         if (!state.getValue(SculkShriekerBlock.SHRIEKING)) {
            this.warningLevel = 0;
            if (!this.canRespond(level) || this.tryToWarn(level, player)) {
               this.shriek(level, player);
            }
         }
      }
   }

   private boolean tryToWarn(final ServerLevel level, final ServerPlayer player) {
      OptionalInt maybeWarningLevel = WardenSpawnTracker.tryWarn(level, this.getBlockPos(), player);
      maybeWarningLevel.ifPresent(warningLevel -> this.warningLevel = warningLevel);
      return maybeWarningLevel.isPresent();
   }

   private void shriek(final ServerLevel level, final @Nullable Entity sourceEntity) {
      BlockPos pos = this.getBlockPos();
      BlockState state = this.getBlockState();
      level.setBlock(pos, state.setValue(SculkShriekerBlock.SHRIEKING, true), 2);
      level.scheduleTick(pos, state.getBlock(), 90);
      level.levelEvent(3007, pos, 0);
      level.gameEvent(GameEvent.SHRIEK, pos, GameEvent.Context.of(sourceEntity));
   }

   private boolean canRespond(final ServerLevel level) {
      return this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON)
         && level.getDifficulty() != Difficulty.PEACEFUL
         && level.getGameRules().get(GameRules.SPAWN_WARDENS);
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
      if (state.getValue(SculkShriekerBlock.SHRIEKING) && this.level instanceof ServerLevel serverLevel) {
         this.tryRespond(serverLevel);
      }
   }

   public void tryRespond(final ServerLevel level) {
      if (this.canRespond(level) && this.warningLevel > 0) {
         if (!this.trySummonWarden(level)) {
            this.playWardenReplySound(level);
         }

         Warden.applyDarknessAround(level, Vec3.atCenterOf(this.getBlockPos()), null, 40);
      }
   }

   private void playWardenReplySound(final Level level) {
      SoundEvent sound = (SoundEvent)SOUND_BY_LEVEL.get(this.warningLevel);
      if (sound != null) {
         BlockPos pos = this.getBlockPos();
         RandomSource random = level.getRandom();
         int x = pos.getX() + Mth.randomBetweenInclusive(random, -10, 10);
         int y = pos.getY() + Mth.randomBetweenInclusive(random, -10, 10);
         int z = pos.getZ() + Mth.randomBetweenInclusive(random, -10, 10);
         level.playSound(null, x, y, z, sound, SoundSource.HOSTILE, 5.0F, 1.0F);
      }
   }

   private boolean trySummonWarden(final ServerLevel level) {
      return this.warningLevel < 4
         ? false
         : SpawnUtil.trySpawnMob(
               EntityTypes.WARDEN, EntitySpawnReason.TRIGGERED, level, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false
            )
            .isPresent();
   }

   public VibrationSystem.Listener getListener() {
      return this.vibrationListener;
   }

   private class VibrationUser implements VibrationSystem.User {
      private static final int LISTENER_RADIUS = 8;
      private final PositionSource positionSource = new BlockPositionSource(SculkShriekerBlockEntity.this.worldPosition);

      public VibrationUser() {
      }

      @Override
      public int getListenerRadius() {
         return 8;
      }

      @Override
      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      @Override
      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.SHRIEKER_CAN_LISTEN;
      }

      @Override
      public boolean canReceiveVibration(final ServerLevel level, final BlockPos pos, final Holder<GameEvent> event, final GameEvent.Context context) {
         return !SculkShriekerBlockEntity.this.getBlockState().getValue(SculkShriekerBlock.SHRIEKING)
            && SculkShriekerBlockEntity.tryGetPlayer(context.sourceEntity()) != null;
      }

      @Override
      public void onReceiveVibration(
         final ServerLevel level,
         final BlockPos pos,
         final Holder<GameEvent> event,
         final @Nullable Entity sourceEntity,
         final @Nullable Entity projectileOwner,
         final float receivingDistance
      ) {
         SculkShriekerBlockEntity.this.tryShriek(level, SculkShriekerBlockEntity.tryGetPlayer(projectileOwner != null ? projectileOwner : sourceEntity));
      }

      @Override
      public void onDataChanged() {
         SculkShriekerBlockEntity.this.setChanged();
      }

      @Override
      public boolean requiresAdjacentChunksToBeTicking() {
         return true;
      }
   }
}
