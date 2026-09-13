package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
   private static BlockPos getNearbyPos(final Mob body, final BlockPos pos) {
      RandomSource random = body.level().getRandom();
      return pos.offset(getRandomOffset(random), 0, getRandomOffset(random));
   }

   private static int getRandomOffset(final RandomSource random) {
      return random.nextInt(3) - 1;
   }

   public static <E extends Mob> OneShot<E> create(final MemoryModuleType<BlockPos> locationMemory, final int closeEnoughDist, final float speedModifier) {
      return BehaviorBuilder.create(
         i -> i.group(
               i.present(locationMemory),
               i.absent(MemoryModuleType.ATTACK_TARGET),
               i.absent(MemoryModuleType.WALK_TARGET),
               i.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(i, (location, attackTarget, walkTarget, lookTarget) -> (level, body, timestamp) -> {
               BlockPos celebrateLocation = i.get(location);
               boolean closeEnoughToTarget = celebrateLocation.closerThan(body.blockPosition(), closeEnoughDist);
               if (!closeEnoughToTarget) {
                  BehaviorUtils.setWalkAndLookTargetMemories(body, getNearbyPos(body, celebrateLocation), speedModifier, closeEnoughDist);
               }

               return true;
            })
      );
   }
}
