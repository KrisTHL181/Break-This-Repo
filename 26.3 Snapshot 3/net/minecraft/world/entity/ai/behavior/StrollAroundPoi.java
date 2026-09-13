package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollAroundPoi {
   private static final int MIN_TIME_BETWEEN_STROLLS = 180;
   private static final int STROLL_MAX_XZ_DIST = 8;
   private static final int STROLL_MAX_Y_DIST = 6;

   public static OneShot<PathfinderMob> create(final MemoryModuleType<GlobalPos> memoryType, final float speedModifier, final int maxDistanceFromPoi) {
      MutableLong nextOkStartTime = new MutableLong(0L);
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(memoryType)).apply(i, (walkTarget, memory) -> (level, body, timestamp) -> {
            GlobalPos pos = i.get(memory);
            if (level.dimension() != pos.dimension() || !pos.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)) {
               return false;
            }

            if (timestamp <= nextOkStartTime.longValue()) {
               return true;
            }

            Optional<Vec3> landPos = Optional.ofNullable(LandRandomPos.getPos(body, 8, 6));
            walkTarget.setOrErase(landPos.map(p -> new WalkTarget(p, speedModifier, 1)));
            nextOkStartTime.setValue(timestamp + 180L);
            return true;
         })
      );
   }
}
