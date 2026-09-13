package net.minecraft.world.entity.ai.behavior;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk {
   public static BehaviorControl<PathfinderMob> create(final float speedModifier) {
      return BehaviorBuilder.create(
         i -> i.group(i.absent(MemoryModuleType.WALK_TARGET))
            .apply(
               i,
               walkTarget -> (level, body, timestamp) -> {
                  if (level.canSeeSky(body.blockPosition())) {
                     return false;
                  }

                  BlockPos bodyPos = body.blockPosition();
                  List<BlockPos> poses = BlockPos.betweenClosedStream(bodyPos.offset(-1, -1, -1), bodyPos.offset(1, 1, 1))
                     .map(BlockPos::immutable)
                     .collect(Util.toMutableList());
                  Collections.shuffle(poses);
                  poses.stream()
                     .filter(pos -> !level.canSeeSky(pos))
                     .filter(pos -> level.loadedAndEntityCanStandOn(pos, body))
                     .filter(pos -> level.noCollision(body))
                     .findFirst()
                     .ifPresent(target -> walkTarget.set(new WalkTarget(target, speedModifier, 0)));
                  return true;
               }
            )
      );
   }
}
