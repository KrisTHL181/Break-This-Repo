package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;

public class AcquirePoi {
   public static final int SCAN_RANGE = 48;

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> p_260007_,
      MemoryModuleType<GlobalPos> p_259129_,
      boolean p_259108_,
      Optional<Byte> p_260129_,
      BiPredicate<ServerLevel, BlockPos> p_376059_
   ) {
      return create(p_260007_, p_259129_, p_259129_, p_259108_, p_260129_, p_376059_);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> p_259994_, MemoryModuleType<GlobalPos> p_259167_, boolean p_259077_, Optional<Byte> p_259824_
   ) {
      return create(p_259994_, p_259167_, p_259167_, p_259077_, p_259824_, (p_374959_, p_374960_) -> true);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> p_377768_,
      MemoryModuleType<GlobalPos> p_378646_,
      MemoryModuleType<GlobalPos> p_375764_,
      boolean p_375441_,
      Optional<Byte> p_376147_,
      BiPredicate<ServerLevel, BlockPos> p_376572_
   ) {
      int i = 5;
      int j = 20;
      MutableLong mutablelong = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> long2objectmap = new Long2ObjectOpenHashMap();
      OneShot<PathfinderMob> oneshot = BehaviorBuilder.create(
         p_374955_ -> p_374955_.group(p_374955_.absent(p_375764_))
            .apply(
               p_374955_,
               p_374967_ -> (p_374945_, p_374946_, p_374947_) -> {
                  if (p_375441_ && p_374946_.isBaby()) {
                     return false;
                  }

                  if (mutablelong.longValue() == 0L) {
                     mutablelong.setValue(p_374945_.getGameTime() + p_374945_.random.nextInt(20));
                     return false;
                  }

                  if (p_374945_.getGameTime() < mutablelong.longValue()) {
                     return false;
                  }

                  mutablelong.setValue(p_374947_ + 20L + p_374945_.getRandom().nextInt(20));
                  PoiManager poimanager = p_374945_.getPoiManager();
                  long2objectmap.long2ObjectEntrySet().removeIf(p_22338_ -> !((AcquirePoi.JitteredLinearRetry)p_22338_.getValue()).isStillValid(p_374947_));
                  Predicate<BlockPos> predicate = p_258266_ -> {
                     AcquirePoi.JitteredLinearRetry acquirepoi$jitteredlinearretry = (AcquirePoi.JitteredLinearRetry)long2objectmap.get(p_258266_.asLong());
                     if (acquirepoi$jitteredlinearretry == null) {
                        return true;
                     }

                     if (!acquirepoi$jitteredlinearretry.shouldRetry(p_374947_)) {
                        return false;
                     }

                     acquirepoi$jitteredlinearretry.markAttempt(p_374947_);
                     return true;
                  };
                  Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllClosestFirstWithType(
                        p_377768_, predicate, p_374946_.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                     )
                     .limit(5L)
                     .filter(p_374958_ -> p_376572_.test(p_374945_, (BlockPos)p_374958_.getSecond()))
                     .collect(Collectors.toSet());
                  Path path = findPathToPois(p_374946_, set);
                  if (path != null && path.canReach()) {
                     BlockPos blockpos = path.getTarget();
                     poimanager.getType(blockpos).ifPresent(p_449453_ -> {
                        poimanager.take(p_377768_, (p_217108_, p_217109_) -> p_217109_.equals(blockpos), blockpos, 1);
                        p_374967_.set(GlobalPos.of(p_374945_.dimension(), blockpos));
                        p_376147_.ifPresent(p_147369_ -> p_374945_.broadcastEntityEvent(p_374946_, p_147369_));
                        long2objectmap.clear();
                        p_374945_.debugSynchronizers().updatePoi(blockpos);
                     });
                  } else {
                     for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                        long2objectmap.computeIfAbsent(
                           ((BlockPos)pair.getSecond()).asLong(), p_264881_ -> new AcquirePoi.JitteredLinearRetry(p_374945_.random, p_374947_)
                        );
                     }
                  }

                  return true;
               }
            )
      );
      return p_375764_ == p_378646_
         ? oneshot
         : BehaviorBuilder.create(p_258269_ -> p_258269_.group(p_258269_.absent(p_378646_)).apply(p_258269_, p_258302_ -> oneshot));
   }

   public static @Nullable Path findPathToPois(Mob p_217098_, Set<Pair<Holder<PoiType>, BlockPos>> p_217099_) {
      if (p_217099_.isEmpty()) {
         return null;
      }

      Set<BlockPos> set = new HashSet<>();
      int i = 1;

      for (Pair<Holder<PoiType>, BlockPos> pair : p_217099_) {
         i = Math.max(i, ((PoiType)((Holder)pair.getFirst()).value()).validRange());
         set.add((BlockPos)pair.getSecond());
      }

      return p_217098_.getNavigation().createPath(set, i);
   }

   static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      JitteredLinearRetry(RandomSource p_217111_, long p_217112_) {
         this.random = p_217111_;
         this.markAttempt(p_217112_);
      }

      public void markAttempt(long p_22381_) {
         this.previousAttemptTimestamp = p_22381_;
         int i = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(i, 400);
         this.nextScheduledAttemptTimestamp = p_22381_ + this.currentDelay;
      }

      public boolean isStillValid(long p_22383_) {
         return p_22383_ - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long p_22385_) {
         return p_22385_ >= this.nextScheduledAttemptTimestamp;
      }

      @Override
      public String toString() {
         return "RetryMarker{, previousAttemptAt="
            + this.previousAttemptTimestamp
            + ", nextScheduledAttemptAt="
            + this.nextScheduledAttemptTimestamp
            + ", currentDelay="
            + this.currentDelay
            + "}";
      }
   }
}
