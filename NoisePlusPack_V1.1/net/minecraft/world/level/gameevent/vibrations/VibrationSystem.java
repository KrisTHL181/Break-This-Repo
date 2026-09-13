package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.advancements.CriteriaTriggers;
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
   ToIntFunction<ResourceKey<GameEvent>> VIBRATION_FREQUENCY_FOR_EVENT = Util.make(new Reference2IntOpenHashMap(), p_330465_ -> {
      p_330465_.defaultReturnValue(0);
      p_330465_.put(GameEvent.STEP.key(), 1);
      p_330465_.put(GameEvent.SWIM.key(), 1);
      p_330465_.put(GameEvent.FLAP.key(), 1);
      p_330465_.put(GameEvent.PROJECTILE_LAND.key(), 2);
      p_330465_.put(GameEvent.HIT_GROUND.key(), 2);
      p_330465_.put(GameEvent.SPLASH.key(), 2);
      p_330465_.put(GameEvent.ITEM_INTERACT_FINISH.key(), 3);
      p_330465_.put(GameEvent.PROJECTILE_SHOOT.key(), 3);
      p_330465_.put(GameEvent.INSTRUMENT_PLAY.key(), 3);
      p_330465_.put(GameEvent.ENTITY_ACTION.key(), 4);
      p_330465_.put(GameEvent.ELYTRA_GLIDE.key(), 4);
      p_330465_.put(GameEvent.UNEQUIP.key(), 4);
      p_330465_.put(GameEvent.ENTITY_DISMOUNT.key(), 5);
      p_330465_.put(GameEvent.EQUIP.key(), 5);
      p_330465_.put(GameEvent.ENTITY_INTERACT.key(), 6);
      p_330465_.put(GameEvent.SHEAR.key(), 6);
      p_330465_.put(GameEvent.ENTITY_MOUNT.key(), 6);
      p_330465_.put(GameEvent.ENTITY_DAMAGE.key(), 7);
      p_330465_.put(GameEvent.DRINK.key(), 8);
      p_330465_.put(GameEvent.EAT.key(), 8);
      p_330465_.put(GameEvent.CONTAINER_CLOSE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_CLOSE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_DEACTIVATE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_DETACH.key(), 9);
      p_330465_.put(GameEvent.CONTAINER_OPEN.key(), 10);
      p_330465_.put(GameEvent.BLOCK_OPEN.key(), 10);
      p_330465_.put(GameEvent.BLOCK_ACTIVATE.key(), 10);
      p_330465_.put(GameEvent.BLOCK_ATTACH.key(), 10);
      p_330465_.put(GameEvent.PRIME_FUSE.key(), 10);
      p_330465_.put(GameEvent.NOTE_BLOCK_PLAY.key(), 10);
      p_330465_.put(GameEvent.BLOCK_CHANGE.key(), 11);
      p_330465_.put(GameEvent.BLOCK_DESTROY.key(), 12);
      p_330465_.put(GameEvent.FLUID_PICKUP.key(), 12);
      p_330465_.put(GameEvent.BLOCK_PLACE.key(), 13);
      p_330465_.put(GameEvent.FLUID_PLACE.key(), 13);
      p_330465_.put(GameEvent.ENTITY_PLACE.key(), 14);
      p_330465_.put(GameEvent.LIGHTNING_STRIKE.key(), 14);
      p_330465_.put(GameEvent.TELEPORT.key(), 14);
      p_330465_.put(GameEvent.ENTITY_DIE.key(), 15);
      p_330465_.put(GameEvent.EXPLODE.key(), 15);

      for (int i = 1; i <= 15; i++) {
         p_330465_.put(getResonanceEventByFrequency(i), i);
      }
   });

   VibrationSystem.Data getVibrationData();

   VibrationSystem.User getVibrationUser();

   static int getGameEventFrequency(Holder<GameEvent> p_330756_) {
      return p_330756_.unwrapKey().map(VibrationSystem::getGameEventFrequency).orElse(0);
   }

   static int getGameEventFrequency(ResourceKey<GameEvent> p_335067_) {
      return VIBRATION_FREQUENCY_FOR_EVENT.applyAsInt(p_335067_);
   }

   static ResourceKey<GameEvent> getResonanceEventByFrequency(int p_282105_) {
      return RESONANCE_EVENTS.get(p_282105_ - 1);
   }

   static int getRedstoneStrengthForDistance(float p_282483_, int p_282722_) {
      double d0 = 15.0 / p_282722_;
      return Math.max(1, 15 - Mth.floor(d0 * p_282483_));
   }

   final class Data {
      public static Codec<VibrationSystem.Data> CODEC = RecordCodecBuilder.create(
         p_327444_ -> p_327444_.group(
               VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(p_281665_ -> Optional.ofNullable(p_281665_.currentVibration)),
               VibrationSelector.CODEC.fieldOf("selector").forGetter(VibrationSystem.Data::getSelectionStrategy),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter(VibrationSystem.Data::getTravelTimeInTicks)
            )
            .apply(p_327444_, (p_281934_, p_282381_, p_282931_) -> new VibrationSystem.Data((VibrationInfo)p_281934_.orElse(null), p_282381_, p_282931_, true))
      );
      public static final String NBT_TAG_KEY = "listener";
      @Nullable VibrationInfo currentVibration;
      private int travelTimeInTicks;
      final VibrationSelector selectionStrategy;
      private boolean reloadVibrationParticle;

      private Data(@Nullable VibrationInfo p_281967_, VibrationSelector p_283036_, int p_283607_, boolean p_282438_) {
         this.currentVibration = p_281967_;
         this.travelTimeInTicks = p_283607_;
         this.selectionStrategy = p_283036_;
         this.reloadVibrationParticle = p_282438_;
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

      public void setCurrentVibration(@Nullable VibrationInfo p_282049_) {
         this.currentVibration = p_282049_;
      }

      public int getTravelTimeInTicks() {
         return this.travelTimeInTicks;
      }

      public void setTravelTimeInTicks(int p_282973_) {
         this.travelTimeInTicks = p_282973_;
      }

      public void decrementTravelTime() {
         this.travelTimeInTicks = Math.max(0, this.travelTimeInTicks - 1);
      }

      public boolean shouldReloadVibrationParticle() {
         return this.reloadVibrationParticle;
      }

      public void setReloadVibrationParticle(boolean p_281702_) {
         this.reloadVibrationParticle = p_281702_;
      }
   }

   class Listener implements GameEventListener {
      private final VibrationSystem system;

      public Listener(VibrationSystem p_281843_) {
         this.system = p_281843_;
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
      public boolean handleGameEvent(ServerLevel p_282254_, Holder<GameEvent> p_335813_, GameEvent.Context p_283664_, Vec3 p_282426_) {
         VibrationSystem.Data vibrationsystem$data = this.system.getVibrationData();
         VibrationSystem.User vibrationsystem$user = this.system.getVibrationUser();
         if (vibrationsystem$data.getCurrentVibration() != null) {
            return false;
         }

         if (!vibrationsystem$user.isValidVibration(p_335813_, p_283664_)) {
            return false;
         }

         Optional<Vec3> optional = vibrationsystem$user.getPositionSource().getPosition(p_282254_);
         if (optional.isEmpty()) {
            return false;
         }

         Vec3 vec3 = optional.get();
         if (!vibrationsystem$user.canReceiveVibration(p_282254_, BlockPos.containing(p_282426_), p_335813_, p_283664_)) {
            return false;
         }

         if (isOccluded(p_282254_, p_282426_, vec3)) {
            return false;
         }

         this.scheduleVibration(p_282254_, vibrationsystem$data, p_335813_, p_283664_, p_282426_, vec3);
         return true;
      }

      public void forceScheduleVibration(ServerLevel p_282808_, Holder<GameEvent> p_332796_, GameEvent.Context p_281652_, Vec3 p_281530_) {
         this.system
            .getVibrationUser()
            .getPositionSource()
            .getPosition(p_282808_)
            .ifPresent(p_327449_ -> this.scheduleVibration(p_282808_, this.system.getVibrationData(), p_332796_, p_281652_, p_281530_, p_327449_));
      }

      private void scheduleVibration(
         ServerLevel p_282037_, VibrationSystem.Data p_283229_, Holder<GameEvent> p_329298_, GameEvent.Context p_283344_, Vec3 p_281758_, Vec3 p_282990_
      ) {
         p_283229_.selectionStrategy
            .addCandidate(new VibrationInfo(p_329298_, (float)p_281758_.distanceTo(p_282990_), p_281758_, p_283344_.sourceEntity()), p_282037_.getGameTime());
      }

      public static float distanceBetweenInBlocks(BlockPos p_282413_, BlockPos p_281960_) {
         return (float)Math.sqrt(p_282413_.distSqr(p_281960_));
      }

      private static boolean isOccluded(Level p_283225_, Vec3 p_283328_, Vec3 p_283163_) {
         Vec3 vec3 = new Vec3(Mth.floor(p_283328_.x) + 0.5, Mth.floor(p_283328_.y) + 0.5, Mth.floor(p_283328_.z) + 0.5);
         Vec3 vec31 = new Vec3(Mth.floor(p_283163_.x) + 0.5, Mth.floor(p_283163_.y) + 0.5, Mth.floor(p_283163_.z) + 0.5);

         for (Direction direction : Direction.values()) {
            Vec3 vec32 = vec3.relative(direction, 1.0E-5F);
            if (p_283225_.isBlockInLine(new ClipBlockStateContext(vec32, vec31, p_283608_ -> p_283608_.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
               != HitResult.Type.BLOCK) {
               return false;
            }
         }

         return true;
      }
   }

   interface Ticker {
      static void tick(Level p_281704_, VibrationSystem.Data p_282633_, VibrationSystem.User p_281564_) {
         if (p_281704_ instanceof ServerLevel serverlevel) {
            if (p_282633_.currentVibration == null) {
               trySelectAndScheduleVibration(serverlevel, p_282633_, p_281564_);
            }

            if (p_282633_.currentVibration != null) {
               boolean flag = p_282633_.getTravelTimeInTicks() > 0;
               tryReloadVibrationParticle(serverlevel, p_282633_, p_281564_);
               p_282633_.decrementTravelTime();
               if (p_282633_.getTravelTimeInTicks() <= 0) {
                  flag = receiveVibration(serverlevel, p_282633_, p_281564_, p_282633_.currentVibration);
               }

               if (flag) {
                  p_281564_.onDataChanged();
               }
            }
         }
      }

      private static void trySelectAndScheduleVibration(ServerLevel p_282775_, VibrationSystem.Data p_282792_, VibrationSystem.User p_281845_) {
         p_282792_.getSelectionStrategy()
            .chosenCandidate(p_282775_.getGameTime())
            .ifPresent(
               p_282059_ -> {
                  p_282792_.setCurrentVibration(p_282059_);
                  Vec3 vec3 = p_282059_.pos();
                  p_282792_.setTravelTimeInTicks(p_281845_.calculateTravelTimeInTicks(p_282059_.distance()));
                  p_282775_.sendParticles(
                     new VibrationParticleOption(p_281845_.getPositionSource(), p_282792_.getTravelTimeInTicks()),
                     vec3.x,
                     vec3.y,
                     vec3.z,
                     1,
                     0.0,
                     0.0,
                     0.0,
                     0.0
                  );
                  p_281845_.onDataChanged();
                  p_282792_.getSelectionStrategy().startOver();
               }
            );
      }

      private static void tryReloadVibrationParticle(ServerLevel p_282010_, VibrationSystem.Data p_282354_, VibrationSystem.User p_282958_) {
         if (p_282354_.shouldReloadVibrationParticle()) {
            if (p_282354_.currentVibration == null) {
               p_282354_.setReloadVibrationParticle(false);
            } else {
               Vec3 vec3 = p_282354_.currentVibration.pos();
               PositionSource positionsource = p_282958_.getPositionSource();
               Vec3 vec31 = positionsource.getPosition(p_282010_).orElse(vec3);
               int i = p_282354_.getTravelTimeInTicks();
               int j = p_282958_.calculateTravelTimeInTicks(p_282354_.currentVibration.distance());
               double d0 = 1.0 - (double)i / j;
               double d1 = Mth.lerp(d0, vec3.x, vec31.x);
               double d2 = Mth.lerp(d0, vec3.y, vec31.y);
               double d3 = Mth.lerp(d0, vec3.z, vec31.z);
               boolean flag = p_282010_.sendParticles(new VibrationParticleOption(positionsource, i), d1, d2, d3, 1, 0.0, 0.0, 0.0, 0.0) > 0;
               if (flag) {
                  p_282354_.setReloadVibrationParticle(false);
               }
            }
         }
      }

      private static boolean receiveVibration(ServerLevel p_282967_, VibrationSystem.Data p_283447_, VibrationSystem.User p_282301_, VibrationInfo p_281498_) {
         BlockPos blockpos = BlockPos.containing(p_281498_.pos());
         BlockPos blockpos1 = p_282301_.getPositionSource().getPosition(p_282967_).map(BlockPos::containing).orElse(blockpos);
         if (p_282301_.requiresAdjacentChunksToBeTicking() && !areAdjacentChunksTicking(p_282967_, blockpos1)) {
            return false;
         }

         p_282301_.onReceiveVibration(
            p_282967_,
            blockpos,
            p_281498_.gameEvent(),
            p_281498_.getEntity(p_282967_).orElse(null),
            p_281498_.getProjectileOwner(p_282967_).orElse(null),
            VibrationSystem.Listener.distanceBetweenInBlocks(blockpos, blockpos1)
         );
         p_283447_.setCurrentVibration(null);
         return true;
      }

      private static boolean areAdjacentChunksTicking(Level p_282735_, BlockPos p_281722_) {
         ChunkPos chunkpos = new ChunkPos(p_281722_);

         for (int i = chunkpos.x - 1; i <= chunkpos.x + 1; i++) {
            for (int j = chunkpos.z - 1; j <= chunkpos.z + 1; j++) {
               if (!p_282735_.shouldTickBlocksAt(ChunkPos.asLong(i, j)) || p_282735_.getChunkSource().getChunkNow(i, j) == null) {
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

      boolean canReceiveVibration(ServerLevel var1, BlockPos var2, Holder<GameEvent> var3, GameEvent.Context var4);

      void onReceiveVibration(ServerLevel var1, BlockPos var2, Holder<GameEvent> var3, @Nullable Entity var4, @Nullable Entity var5, float var6);

      default TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.VIBRATIONS;
      }

      default boolean canTriggerAvoidVibration() {
         return false;
      }

      default boolean requiresAdjacentChunksToBeTicking() {
         return false;
      }

      default int calculateTravelTimeInTicks(float p_281658_) {
         return Mth.floor(p_281658_);
      }

      default boolean isValidVibration(Holder<GameEvent> p_335159_, GameEvent.Context p_283373_) {
         if (!p_335159_.is(this.getListenableEvents())) {
            return false;
         }

         Entity entity = p_283373_.sourceEntity();
         if (entity != null) {
            if (entity.isSpectator()) {
               return false;
            }

            if (entity.isSteppingCarefully() && p_335159_.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
               if (this.canTriggerAvoidVibration() && entity instanceof ServerPlayer serverplayer) {
                  CriteriaTriggers.AVOID_VIBRATION.trigger(serverplayer);
               }

               return false;
            }

            if (entity.dampensVibrations()) {
               return false;
            }
         }

         return p_283373_.affectedState() != null ? !p_283373_.affectedState().is(BlockTags.DAMPENS_VIBRATIONS) : true;
      }

      default void onDataChanged() {
      }
   }
}
