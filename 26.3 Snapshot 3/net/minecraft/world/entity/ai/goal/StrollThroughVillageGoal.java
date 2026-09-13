package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StrollThroughVillageGoal extends Goal {
   private static final int DISTANCE_THRESHOLD = 10;
   private final PathfinderMob mob;
   private final int interval;
   private @Nullable BlockPos wantedPos;

   public StrollThroughVillageGoal(final PathfinderMob mob, final int interval) {
      this.mob = mob;
      this.interval = reducedTickDelay(interval);
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (this.mob.hasControllingPassenger()) {
         return false;
      }

      if (this.mob.level().isBrightOutside()) {
         return false;
      }

      if (this.mob.getRandom().nextInt(this.interval) != 0) {
         return false;
      }

      ServerLevel level = (ServerLevel)this.mob.level();
      BlockPos pos = this.mob.blockPosition();
      if (!level.isCloseToVillage(pos, 6)) {
         return false;
      }

      Vec3 landPos = LandRandomPos.getPos(this.mob, 15, 7, p -> -level.sectionsToVillage(SectionPos.of(p)));
      this.wantedPos = landPos == null ? null : BlockPos.containing(landPos);
      return this.wantedPos != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.wantedPos != null && !this.mob.getNavigation().isDone() && this.mob.getNavigation().getTargetPos().equals(this.wantedPos);
   }

   @Override
   public void tick() {
      if (this.wantedPos != null) {
         PathNavigation navigation = this.mob.getNavigation();
         if (navigation.isDone() && !this.wantedPos.closerToCenterThan(this.mob.position(), 10.0)) {
            Vec3 longDistanceTarget = Vec3.atBottomCenterOf(this.wantedPos);
            Vec3 selfVector = this.mob.position();
            Vec3 distance = selfVector.subtract(longDistanceTarget);
            longDistanceTarget = distance.scale(0.4).add(longDistanceTarget);
            Vec3 moveTarget = longDistanceTarget.subtract(selfVector).normalize().scale(10.0).add(selfVector);
            BlockPos pathTarget = BlockPos.containing(moveTarget);
            pathTarget = this.mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pathTarget);
            if (!navigation.moveTo(pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), 1.0)) {
               this.moveRandomly();
            }
         }
      }
   }

   private void moveRandomly() {
      RandomSource random = this.mob.getRandom();
      BlockPos pathTarget = this.mob
         .level()
         .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
      this.mob.getNavigation().moveTo(pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), 1.0);
   }
}
