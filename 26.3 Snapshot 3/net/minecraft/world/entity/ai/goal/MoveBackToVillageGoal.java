package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MoveBackToVillageGoal extends RandomStrollGoal {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public MoveBackToVillageGoal(final PathfinderMob mob, final double speedModifier, final boolean checkNoActionTime) {
      super(mob, speedModifier, 10, checkNoActionTime);
   }

   @Override
   public boolean canUse() {
      ServerLevel level = (ServerLevel)this.mob.level();
      BlockPos pos = this.mob.blockPosition();
      return level.isVillage(pos) ? false : super.canUse();
   }

   @Override
   protected @Nullable Vec3 getPosition() {
      ServerLevel level = (ServerLevel)this.mob.level();
      BlockPos pos = this.mob.blockPosition();
      SectionPos sectionPos = SectionPos.of(pos);
      SectionPos optimalSectionPos = BehaviorUtils.findSectionClosestToVillage(level, sectionPos, 2);
      return optimalSectionPos != sectionPos
         ? DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(optimalSectionPos.center()), (float) (Math.PI / 2))
         : null;
   }
}
