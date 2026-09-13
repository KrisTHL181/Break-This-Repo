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
      final Predicate<Holder<PoiType>> poiType,
      final MemoryModuleType<GlobalPos> memoryToAcquire,
      final boolean onlyIfAdult,
      final Optional<Byte> onPoiAcquisitionEvent,
      final BiPredicate<ServerLevel, BlockPos> validPoi
   ) {
      return create(poiType, memoryToAcquire, memoryToAcquire, onlyIfAdult, onPoiAcquisitionEvent, validPoi);
   }

   public static BehaviorControl<PathfinderMob> create(
      final Predicate<Holder<PoiType>> poiType,
      final MemoryModuleType<GlobalPos> memoryToAcquire,
      final boolean onlyIfAdult,
      final Optional<Byte> onPoiAcquisitionEvent
   ) {
      return create(poiType, memoryToAcquire, memoryToAcquire, onlyIfAdult, onPoiAcquisitionEvent, (l, p) -> true);
   }

   public static BehaviorControl<PathfinderMob> create(
      final Predicate<Holder<PoiType>> poiType,
      final MemoryModuleType<GlobalPos> memoryToValidate,
      final MemoryModuleType<GlobalPos> memoryToAcquire,
      final boolean onlyIfAdult,
      final Optional<Byte> onPoiAcquisitionEvent,
      final BiPredicate<ServerLevel, BlockPos> validPoi
   ) {
      int batchSize = 5;
      int rate = 20;
      MutableLong nextScheduledStart = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> batchCache = new Long2ObjectOpenHashMap();
      OneShot<PathfinderMob> acquirePoi = BehaviorBuilder.create(
         i -> i.group(i.absent(memoryToAcquire))
            .apply(
               i,
               toAcquire -> (level, body, timestamp) -> {
                  if (onlyIfAdult && body.isBaby()) {
                     return false;
                  }

                  RandomSource random = level.getRandom();
                  if (nextScheduledStart.longValue() == 0L) {
                     nextScheduledStart.setValue(level.getGameTime() + random.nextInt(20));
                     return false;
                  }

                  if (level.getGameTime() < nextScheduledStart.longValue()) {
                     return false;
                  }

                  nextScheduledStart.setValue(timestamp + 20L + random.nextInt(20));
                  PoiManager poiManager = level.getPoiManager();
                  batchCache.long2ObjectEntrySet().removeIf(entry -> !((AcquirePoi.JitteredLinearRetry)entry.getValue()).isStillValid(timestamp));
                  Predicate<BlockPos> cacheTest = pos -> {
                     AcquirePoi.JitteredLinearRetry retryMarker = (AcquirePoi.JitteredLinearRetry)batchCache.get(pos.asLong());
                     if (retryMarker == null) {
                        return true;
                     }

                     if (!retryMarker.shouldRetry(timestamp)) {
                        return false;
                     }

                     retryMarker.markAttempt(timestamp);
                     return true;
                  };
                  Set<Pair<Holder<PoiType>, BlockPos>> poiPositions = poiManager.findAllClosestFirstWithType(
                        poiType, cacheTest, body.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                     )
                     .limit(5L)
                     .filter(px -> validPoi.test(level, (BlockPos)px.getSecond()))
                     .collect(Collectors.toSet());
                  Path path = findPathToPois(body, poiPositions);
                  if (path != null && path.canReach()) {
                     BlockPos targetPos = path.getTarget();
                     poiManager.getType(targetPos).ifPresent(type -> {
                        poiManager.take(poiType, (t, poiPos) -> poiPos.equals(targetPos), targetPos, 1);
                        toAcquire.set(GlobalPos.of(level.dimension(), targetPos));
                        onPoiAcquisitionEvent.ifPresent(event -> level.broadcastEntityEvent(body, event));
                        batchCache.clear();
                        level.debugSynchronizers().updatePoi(targetPos);
                     });
                  } else {
                     for (Pair<Holder<PoiType>, BlockPos> p : poiPositions) {
                        batchCache.computeIfAbsent(((BlockPos)p.getSecond()).asLong(), key -> new AcquirePoi.JitteredLinearRetry(random, timestamp));
                     }
                  }

                  return true;
               }
            )
      );
      return memoryToAcquire == memoryToValidate
         ? acquirePoi
         : BehaviorBuilder.create(i -> i.group(i.absent(memoryToValidate)).apply(i, toValidate -> acquirePoi));
   }

   public static @Nullable Path findPathToPois(final Mob body, final Set<Pair<Holder<PoiType>, BlockPos>> pois) {
      if (pois.isEmpty()) {
         return null;
      }

      Set<BlockPos> targets = new HashSet<>();
      int maxRange = 1;

      for (Pair<Holder<PoiType>, BlockPos> p : pois) {
         maxRange = Math.max(maxRange, ((PoiType)((Holder)p.getFirst()).value()).validRange());
         targets.add((BlockPos)p.getSecond());
      }

      return body.getNavigation().createPath(targets, maxRange);
   }

   private static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      public JitteredLinearRetry(final RandomSource random, final long firstAttemptTimestamp) {
         this.random = random;
         this.markAttempt(firstAttemptTimestamp);
      }

      public void markAttempt(final long timestamp) {
         this.previousAttemptTimestamp = timestamp;
         int suggestedDelay = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(suggestedDelay, 400);
         this.nextScheduledAttemptTimestamp = timestamp + this.currentDelay;
      }

      public boolean isStillValid(final long timestamp) {
         return timestamp - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(final long timestamp) {
         return timestamp >= this.nextScheduledAttemptTimestamp;
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
