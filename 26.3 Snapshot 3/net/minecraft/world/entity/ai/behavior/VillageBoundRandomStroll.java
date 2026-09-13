package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public static OneShot<PathfinderMob> create(final float speedModifier) {
      return create(speedModifier, 10, 7);
   }

   public static OneShot<PathfinderMob> create(final float speedModifier, final int maxXyDist, final int maxYDist) {
      return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply(i, walkTarget -> (level, body, timestamp) -> {
         BlockPos bodyPos = body.blockPosition();
         Vec3 landPos;
         if (level.isVillage(bodyPos)) {
            landPos = LandRandomPos.getPos(body, maxXyDist, maxYDist);
         } else {
            SectionPos sectionPos = SectionPos.of(bodyPos);
            SectionPos optimalSectionPos = BehaviorUtils.findSectionClosestToVillage(level, sectionPos, 2);
            if (optimalSectionPos != sectionPos) {
               landPos = DefaultRandomPos.getPosTowards(body, maxXyDist, maxYDist, Vec3.atBottomCenterOf(optimalSectionPos.center()), (float) (Math.PI / 2));
            } else {
               landPos = LandRandomPos.getPos(body, maxXyDist, maxYDist);
            }
         }

         walkTarget.setOrErase(Optional.ofNullable(landPos).map(pos -> new WalkTarget(pos, speedModifier, 0)));
         return true;
      }));
   }
}
