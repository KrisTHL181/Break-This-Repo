package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
   public static OneShot<Villager> create(
      final MemoryModuleType<GlobalPos> memoryType,
      final float speedModifier,
      final int closeEnoughDist,
      final int tooFarDistance,
      final int tooLongUnreachableDuration
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE), i.absent(MemoryModuleType.WALK_TARGET), i.present(memoryType))
            .apply(
               i,
               (cantReachSince, walkTarget, memory) -> (level, body, timestamp) -> {
                  GlobalPos targetPos = i.get(memory);
                  Optional<Long> cantReachTargetSince = i.tryGet(cantReachSince);
                  if (targetPos.dimension() == level.dimension()
                     && (!cantReachTargetSince.isPresent() || level.getGameTime() - cantReachTargetSince.get() <= tooLongUnreachableDuration)) {
                     if (targetPos.pos().distManhattan(body.blockPosition()) > tooFarDistance) {
                        Vec3 towardsTargetPos = null;
                        int tries = 0;
                        int MAX_TRIES = 1000;

                        while (towardsTargetPos == null || BlockPos.containing(towardsTargetPos).distManhattan(body.blockPosition()) > tooFarDistance) {
                           towardsTargetPos = DefaultRandomPos.getPosTowards(body, 15, 7, Vec3.atBottomCenterOf(targetPos.pos()), (float) (Math.PI / 2));
                           if (++tries == 1000) {
                              body.releasePoi(memoryType);
                              memory.erase();
                              cantReachSince.set(timestamp);
                              return true;
                           }
                        }

                        walkTarget.set(new WalkTarget(towardsTargetPos, speedModifier, closeEnoughDist));
                     } else if (targetPos.pos().distManhattan(body.blockPosition()) > closeEnoughDist) {
                        walkTarget.set(new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDist));
                     }
                  } else {
                     body.releasePoi(memoryType);
                     memory.erase();
                     cantReachSince.set(timestamp);
                  }

                  return true;
               }
            )
      );
   }
}
