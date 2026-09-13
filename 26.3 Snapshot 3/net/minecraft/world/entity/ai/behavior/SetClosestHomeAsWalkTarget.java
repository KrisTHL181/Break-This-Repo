package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;

public class SetClosestHomeAsWalkTarget {
   private static final int CACHE_TIMEOUT = 40;
   private static final int BATCH_SIZE = 5;
   private static final int RATE = 20;
   private static final int OK_DISTANCE_SQR = 4;

   public static BehaviorControl<PathfinderMob> create(final float speedModifier) {
      Long2LongMap batchCache = new Long2LongOpenHashMap();
      MutableLong lastUpdate = new MutableLong(0L);
      return BehaviorBuilder.create(
         i -> i.group(i.absent(MemoryModuleType.WALK_TARGET), i.absent(MemoryModuleType.HOME))
            .apply(
               i,
               (walkTarget, home) -> (level, body, timestamp) -> {
                  if (level.getGameTime() - lastUpdate.longValue() < 20L) {
                     return false;
                  }

                  PoiManager poiManager = level.getPoiManager();
                  Optional<BlockPos> closest = poiManager.findClosest(p -> p.is(PoiTypes.HOME), body.blockPosition(), 48, PoiManager.Occupancy.ANY);
                  if (!closest.isEmpty() && !(closest.get().distSqr(body.blockPosition()) <= 4.0)) {
                     MutableInt triedCount = new MutableInt(0);
                     lastUpdate.setValue(level.getGameTime() + level.getRandom().nextInt(20));
                     Predicate<BlockPos> cacheTest = pos -> {
                        long key = pos.asLong();
                        if (batchCache.containsKey(key)) {
                           return false;
                        }

                        if (triedCount.incrementAndGet() >= 5) {
                           return false;
                        }

                        batchCache.put(key, lastUpdate.longValue() + 40L);
                        return true;
                     };
                     Set<Pair<Holder<PoiType>, BlockPos>> pois = poiManager.findAllWithType(
                           p -> p.is(PoiTypes.HOME), cacheTest, body.blockPosition(), 48, PoiManager.Occupancy.ANY
                        )
                        .collect(Collectors.toSet());
                     Path path = AcquirePoi.findPathToPois(body, pois);
                     if (path != null && path.canReach()) {
                        BlockPos targetPos = path.getTarget();
                        Optional<Holder<PoiType>> type = poiManager.getType(targetPos);
                        if (type.isPresent()) {
                           walkTarget.set(new WalkTarget(targetPos, speedModifier, 1));
                           level.debugSynchronizers().updatePoi(targetPos);
                        }
                     } else if (triedCount.intValue() < 5) {
                        batchCache.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < lastUpdate.longValue());
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}
