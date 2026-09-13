package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoiList {
   public static BehaviorControl<Villager> create(
      final MemoryModuleType<List<GlobalPos>> strollToMemoryType,
      final float speedModifier,
      final int closeEnoughDist,
      final int maxDistanceFromPoi,
      final MemoryModuleType<GlobalPos> mustBeCloseToMemoryType
   ) {
      MutableLong nextOkStartTime = new MutableLong(0L);
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(strollToMemoryType), i.present(mustBeCloseToMemoryType))
            .apply(i, (walkTarget, strollToMemory, mustBeCloseToMemory) -> (level, body, timestamp) -> {
               List<GlobalPos> strollTo = i.get(strollToMemory);
               GlobalPos stayCloseTo = i.get(mustBeCloseToMemory);
               if (strollTo.isEmpty()) {
                  return false;
               }

               GlobalPos targetPos = strollTo.get(level.getRandom().nextInt(strollTo.size()));
               if (targetPos != null && level.dimension() == targetPos.dimension() && stayCloseTo.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)
                  )
                {
                  if (timestamp > nextOkStartTime.longValue()) {
                     walkTarget.set(new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDist));
                     nextOkStartTime.setValue(timestamp + 100L);
                  }

                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
