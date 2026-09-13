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

   public StrollThroughVillageGoal(PathfinderMob p_25907_, int p_25908_) {
      this.mob = p_25907_;
      this.interval = reducedTickDelay(p_25908_);
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

      ServerLevel serverlevel = (ServerLevel)this.mob.level();
      BlockPos blockpos = this.mob.blockPosition();
      if (!serverlevel.isCloseToVillage(blockpos, 6)) {
         return false;
      }

      Vec3 vec3 = LandRandomPos.getPos(this.mob, 15, 7, p_25912_ -> -serverlevel.sectionsToVillage(SectionPos.of(p_25912_)));
      this.wantedPos = vec3 == null ? null : BlockPos.containing(vec3);
      return this.wantedPos != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.wantedPos != null && !this.mob.getNavigation().isDone() && this.mob.getNavigation().getTargetPos().equals(this.wantedPos);
   }

   @Override
   public void tick() {
      if (this.wantedPos != null) {
         PathNavigation pathnavigation = this.mob.getNavigation();
         if (pathnavigation.isDone() && !this.wantedPos.closerToCenterThan(this.mob.position(), 10.0)) {
            Vec3 vec3 = Vec3.atBottomCenterOf(this.wantedPos);
            Vec3 vec31 = this.mob.position();
            Vec3 vec32 = vec31.subtract(vec3);
            vec3 = vec32.scale(0.4).add(vec3);
            Vec3 vec33 = vec3.subtract(vec31).normalize().scale(10.0).add(vec31);
            BlockPos blockpos = BlockPos.containing(vec33);
            blockpos = this.mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos);
            if (!pathnavigation.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.0)) {
               this.moveRandomly();
            }
         }
      }
   }

   private void moveRandomly() {
      RandomSource randomsource = this.mob.getRandom();
      BlockPos blockpos = this.mob
         .level()
         .getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + randomsource.nextInt(16), 0, -8 + randomsource.nextInt(16))
         );
      this.mob.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.0);
   }
}
