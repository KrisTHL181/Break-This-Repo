package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface VibrationSystem {
   List<ResourceKey<GameEvent>> RESONANCE_EVENTS = List.of(
      GameEvent.RESONATE_1.key(),
      GameEvent.RESONATE_2.key(),
      GameEvent.RESONATE_3.key(),
      GameEvent.RESONATE_4.key(),
      GameEvent.RESONATE_5.key(),
      GameEvent.RESONATE_6.key(),
      GameEvent.RESONATE_7.key(),
      GameEvent.RESONATE_8.key(),
      GameEvent.RESONATE_9.key(),
      GameEvent.RESONATE_10.key(),
      GameEvent.RESONATE_11.key(),
      GameEvent.RESONATE_12.key(),
      GameEvent.RESONATE_13.key(),
      GameEvent.RESONATE_14.key(),
      GameEvent.RESONATE_15.key()
   );
   int NO_VIBRATION_FREQUENCY = 0;
   ToIntFunction<ResourceKey<GameEvent>> VIBRATION_FREQUENCY_FOR_EVENT = Util.make(new Reference2IntOpenHashMap(), map -> {
      map.defaultReturnValue(0);
      map.put(GameEvent.STEP.key(), 1);
      map.put(GameEvent.SWIM.key(), 1);
      map.put(GameEvent.FLAP.key(), 1);
      map.put(GameEvent.PROJECTILE_LAND.key(), 2);
      map.put(GameEvent.HIT_GROUND.key(), 2);
      map.put(GameEvent.SPLASH.key(), 2);
      map.put(GameEvent.BOUNCE.key(), 2);
      map.put(GameEvent.ITEM_INTERACT_FINISH.key(), 3);
      map.put(GameEvent.PROJECTILE_SHOOT.key(), 3);
      map.put(GameEvent.INSTRUMENT_PLAY.key(), 3);
      map.put(GameEvent.ENTITY_ACTION.key(), 4);
      map.put(GameEvent.ELYTRA_GLIDE.key(), 4);
      map.put(GameEvent.UNEQUIP.key(), 4);
      map.put(GameEvent.ENTITY_DISMOUNT.key(), 5);
      map.put(GameEvent.EQUIP.key(), 5);
      map.put(GameEvent.ENTITY_INTERACT.key(), 6);
      map.put(GameEvent.SHEAR.key(), 6);
      map.put(GameEvent.ENTITY_MOUNT.key(), 6);
      map.put(GameEvent.ENTITY_DAMAGE.key(), 7);
      map.put(GameEvent.DRINK.key(), 8);
      map.put(GameEvent.EAT.key(), 8);
      map.put(GameEvent.CONTAINER_CLOSE.key(), 9);
      map.put(GameEvent.BLOCK_CLOSE.key(), 9);
      map.put(GameEvent.BLOCK_DEACTIVATE.key(), 9);
      map.put(GameEvent.BLOCK_DETACH.key(), 9);
      map.put(GameEvent.CONTAINER_OPEN.key(), 10);
      map.put(GameEvent.BLOCK_OPEN.key(), 10);
      map.put(GameEvent.BLOCK_ACTIVATE.key(), 10);
      map.put(GameEvent.BLOCK_ATTACH.key(), 10);
      map.put(GameEvent.PRIME_FUSE.key(), 10);
      map.put(GameEvent.NOTE_BLOCK_PLAY.key(), 10);
      map.put(GameEvent.BLOCK_CHANGE.key(), 11);
      map.put(GameEvent.BLOCK_DESTROY.key(), 12);
      map.put(GameEvent.FLUID_PICKUP.key(), 12);
      map.put(GameEvent.BLOCK_PLACE.key(), 13);
      map.put(GameEvent.FLUID_PLACE.key(), 13);
      map.put(GameEvent.ENTITY_PLACE.key(), 14);
      map.put(GameEvent.LIGHTNING_STRIKE.key(), 14);
      map.put(GameEvent.TELEPORT.key(), 14);
      map.put(GameEvent.ENTITY_DIE.key(), 15);
      map.put(GameEvent.EXPLODE.key(), 15);

      for (int i = 1; i <= 15; i++) {
         map.put(getResonanceEventByFrequency(i), i);
      }
   });

   VibrationSystem.Data getVibrationData();

   VibrationSystem.User getVibrationUser();

   static int getGameEventFrequency(final Holder<GameEvent> event) {
      return event.unwrapKey().map(VibrationSystem::getGameEventFrequency).orElse(0);
   }

   static int getGameEventFrequency(final ResourceKey<GameEvent> event) {
      return VIBRATION_FREQUENCY_FOR_EVENT.applyAsInt(event);
   }

   static ResourceKey<GameEvent> getResonanceEventByFrequency(final int vibrationFrequency) {
      return RESONANCE_EVENTS.get(vibrationFrequency - 1);
   }

   static int getRedstoneStrengthForDistance(final float distance, final int listenerRadius) {
      double powerScale = 15.0 / listenerRadius;
      return Math.max(1, 15 - Mth.floor(powerScale * distance));
   }

   final class Data {
      public static final Codec<VibrationSystem.Data> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(o -> Optional.ofNullable(o.currentVibration)),
               VibrationSelector.CODEC.fieldOf("selector").forGetter(VibrationSystem.Data::getSelectionStrategy),
               ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "event_delay", 0).forGetter(VibrationSystem.Data::getTravelTimeInTicks)
            )
            .apply(
               i,
               (currentVibration, selectionStrategy, travelTimeInTicks) -> new VibrationSystem.Data(
                  (VibrationInfo)currentVibration.orElse(null), selectionStrategy, travelTimeInTicks, true
               )
            )
      );
      public static final String NBT_TAG_KEY = "listener";
      private @Nullable VibrationInfo currentVibration;
      private int travelTimeInTicks;
      private final VibrationSelector selectionStrategy;
      private boolean reloadVibrationParticle;

      private Data(
         final @Nullable VibrationInfo currentVibration,
         final VibrationSelector selectionStrategy,
         final int travelTimeInTicks,
         final boolean reloadVibrationParticle
      ) {
         this.currentVibration = currentVibration;
         this.travelTimeInTicks = travelTimeInTicks;
         this.selectionStrategy = selectionStrategy;
         this.reloadVibrationParticle = reloadVibrationParticle;
      }

      public Data() {
         this(null, new VibrationSelector(), 0, false);
      }

      public VibrationSelector getSelectionStrategy() {
         return this.selectionStrategy;
      }

      public @Nullable VibrationInfo getCurrentVibration() {
         return this.currentVibration;
      }

      public void setCurrentVibration(final @Nullable VibrationInfo currentVibration) {
         this.currentVibration = currentVibration;
      }

      public int getTravelTimeInTicks() {
         return this.travelTimeInTicks;
      }

      public void setTravelTimeInTicks(final int travelTimeInTicks) {
         this.travelTimeInTicks = travelTimeInTicks;
      }

      public void decrementTravelTime() {
         this.travelTimeInTicks = Math.max(0, this.travelTimeInTicks - 1);
      }

      public boolean shouldReloadVibrationParticle() {
         return this.reloadVibrationParticle;
      }

      public void setReloadVibrationParticle(final boolean reloadVibrationParticle) {
         this.reloadVibrationParticle = reloadVibrationParticle;
      }
   }

   class Listener implements GameEventListener {
      private final VibrationSystem system;

      public Listener(final VibrationSystem system) {
         this.system = system;
      }

      @Override
      public PositionSource getListenerSource() {
         return this.system.getVibrationUser().getPositionSource();
      }

      @Override
      public int getListenerRadius() {
         return this.system.getVibrationUser().getListenerRadius();
      }

      @Override
      public boolean handleGameEvent(final ServerLevel level, final Holder<GameEvent> event, final GameEvent.Context context, final Vec3 sourcePosition) {
         VibrationSystem.Data data = this.system.getVibrationData();
         VibrationSystem.User user = this.system.getVibrationUser();
         if (data.getCurrentVibration() != null) {
            return false;
         }

         if (!user.isValidVibration(event, context)) {
            return false;
         }

         Optional<Vec3> listenerSourcePos = user.getPositionSource().getPosition(level);
         if (listenerSourcePos.isEmpty()) {
            return false;
         }

         Vec3 destination = listenerSourcePos.get();
         if (!user.canReceiveVibration(level, BlockPos.containing(sourcePosition), event, context)) {
            return false;
         }

         if (isOccluded(level, sourcePosition, destination)) {
            return false;
         }

         this.scheduleVibration(level, data, event, context, sourcePosition, destination);
         return true;
      }

      public void forceScheduleVibration(final ServerLevel level, final Holder<GameEvent> event, final GameEvent.Context context, final Vec3 origin) {
         this.system
            .getVibrationUser()
            .getPositionSource()
            .getPosition(level)
            .ifPresent(p -> this.scheduleVibration(level, this.system.getVibrationData(), event, context, origin, p));
      }

      private void scheduleVibration(
         final ServerLevel level,
         final VibrationSystem.Data data,
         final Holder<GameEvent> event,
         final GameEvent.Context context,
         final Vec3 origin,
         final Vec3 dest
      ) {
         data.selectionStrategy.addCandidate(new VibrationInfo(event, (float)origin.distanceTo(dest), origin, context.sourceEntity()), level.getGameTime());
      }

      public static float distanceBetweenInBlocks(final BlockPos origin, final BlockPos dest) {
         return (float)Math.sqrt(origin.distSqr(dest));
      }

      private static boolean isOccluded(final Level level, final Vec3 origin, final Vec3 dest) {
         Vec3 from = new Vec3(Mth.floor(origin.x) + 0.5, Mth.floor(origin.y) + 0.5, Mth.floor(origin.z) + 0.5);
         Vec3 to = new Vec3(Mth.floor(dest.x) + 0.5, Mth.floor(dest.y) + 0.5, Mth.floor(dest.z) + 0.5);

         for (Direction direction : Direction.values()) {
            Vec3 nudgedSource = from.relative(direction, 1.0E-5F);
            if (level.isBlockInLine(new ClipBlockStateContext(nudgedSource, to, state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
               != HitResult.Type.BLOCK) {
               return false;
            }
         }

         return true;
      }
   }

   interface Ticker {
      static void tick(final Level level, final VibrationSystem.Data data, final VibrationSystem.User user) {
         if (level instanceof ServerLevel serverLevel) {
            if (data.currentVibration == null) {
               trySelectAndScheduleVibration(serverLevel, data, user);
            }

            if (data.currentVibration != null) {
               boolean hasChanged = data.getTravelTimeInTicks() > 0;
               tryReloadVibrationParticle(serverLevel, data, user);
               data.decrementTravelTime();
               if (data.getTravelTimeInTicks() <= 0) {
                  hasChanged = receiveVibration(serverLevel, data, user, data.currentVibration);
               }

               if (hasChanged) {
                  user.onDataChanged();
               }
            }
         }
      }

      private static void trySelectAndScheduleVibration(final ServerLevel serverLevel, final VibrationSystem.Data data, final VibrationSystem.User user) {
         data.getSelectionStrategy()
            .chosenCandidate(serverLevel.getGameTime())
            .ifPresent(
               context -> {
                  data.setCurrentVibration(context);
                  Vec3 origin = context.pos();
                  data.setTravelTimeInTicks(user.calculateTravelTimeInTicks(context.distance()));
                  serverLevel.sendParticles(
                     new VibrationParticleOption(user.getPositionSource(), data.getTravelTimeInTicks()), origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0
                  );
                  user.onDataChanged();
                  data.getSelectionStrategy().startOver();
               }
            );
      }

      private static void tryReloadVibrationParticle(final ServerLevel level, final VibrationSystem.Data data, final VibrationSystem.User user) {
         if (data.shouldReloadVibrationParticle()) {
            if (data.currentVibration == null) {
               data.setReloadVibrationParticle(false);
            } else {
               Vec3 origin = data.currentVibration.pos();
               PositionSource positionSource = user.getPositionSource();
               Vec3 destination = positionSource.getPosition(level).orElse(origin);
               int travelTimeInTicks = data.getTravelTimeInTicks();
               int initialTravelTime = user.calculateTravelTimeInTicks(data.currentVibration.distance());
               double alpha = 1.0 - (double)travelTimeInTicks / initialTravelTime;
               double newInitialX = Mth.lerp(alpha, origin.x, destination.x);
               double newInitialY = Mth.lerp(alpha, origin.y, destination.y);
               double newInitialZ = Mth.lerp(alpha, origin.z, destination.z);
               boolean particleWasSent = level.sendParticles(
                     new VibrationParticleOption(positionSource, travelTimeInTicks), newInitialX, newInitialY, newInitialZ, 1, 0.0, 0.0, 0.0, 0.0
                  )
                  > 0;
               if (particleWasSent) {
                  data.setReloadVibrationParticle(false);
               }
            }
         }
      }

      private static boolean receiveVibration(
         final ServerLevel serverLevel, final VibrationSystem.Data data, final VibrationSystem.User user, final VibrationInfo currentVibration
      ) {
         BlockPos origin = BlockPos.containing(currentVibration.pos());
         BlockPos destination = user.getPositionSource().getPosition(serverLevel).map(BlockPos::containing).orElse(origin);
         if (user.requiresAdjacentChunksToBeTicking() && !areAdjacentChunksTicking(serverLevel, destination)) {
            return false;
         }

         user.onReceiveVibration(
            serverLevel,
            origin,
            currentVibration.gameEvent(),
            currentVibration.getEntity(serverLevel).orElse(null),
            currentVibration.getProjectileOwner(serverLevel).orElse(null),
            VibrationSystem.Listener.distanceBetweenInBlocks(origin, destination)
         );
         data.setCurrentVibration(null);
         return true;
      }

      private static boolean areAdjacentChunksTicking(final Level level, final BlockPos listenerPos) {
         ChunkPos listenerChunkPos = ChunkPos.containing(listenerPos);

         for (int x = listenerChunkPos.x() - 1; x <= listenerChunkPos.x() + 1; x++) {
            for (int z = listenerChunkPos.z() - 1; z <= listenerChunkPos.z() + 1; z++) {
               if (!level.shouldTickBlocksAt(ChunkPos.pack(x, z)) || level.getChunkSource().getChunkNow(x, z) == null) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   interface User {
      int getListenerRadius();

      PositionSource getPositionSource();

      boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context);

      void onReceiveVibration(
         ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float receivingDistance
      );

      default TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.VIBRATIONS;
      }

      default boolean canTriggerAvoidVibration() {
         return false;
      }

      default boolean requiresAdjacentChunksToBeTicking() {
         return false;
      }

      default int calculateTravelTimeInTicks(final float distanceToDestination) {
         return Mth.floor(distanceToDestination);
      }

      default boolean isValidVibration(final Holder<GameEvent> event, final GameEvent.Context context) {
         if (!event.is(this.getListenableEvents())) {
            return false;
         }

         Entity sourceEntity = context.sourceEntity();
         if (sourceEntity != null) {
            if (sourceEntity.isSpectator()) {
               return false;
            }

            if (sourceEntity.isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
               if (this.canTriggerAvoidVibration() && sourceEntity instanceof ServerPlayer player) {
                  CriteriaTriggers.AVOID_VIBRATION.trigger(player);
               }

               return false;
            }

            if (sourceEntity.dampensVibrations()) {
               return false;
            }
         }

         return context.affectedState() != null ? !context.affectedState().is(BlockTags.DAMPENS_VIBRATIONS) : true;
      }

      default void onDataChanged() {
      }
   }
}
