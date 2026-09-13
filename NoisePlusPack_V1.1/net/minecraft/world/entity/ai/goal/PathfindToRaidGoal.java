package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;

public class PathfindToRaidGoal<T extends Raider> extends Goal {
   private static final int RECRUITMENT_SEARCH_TICK_DELAY = 20;
   private static final float SPEED_MODIFIER = 1.0F;
   private final T mob;
   private int recruitmentTick;

   public PathfindToRaidGoal(T p_25706_) {
      this.mob = p_25706_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      return this.mob.getTarget() == null
         && !this.mob.hasControllingPassenger()
         && this.mob.hasActiveRaid()
         && !this.mob.getCurrentRaid().isOver()
         && !getServerLevel(this.mob.level()).isVillage(this.mob.blockPosition());
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.hasActiveRaid() && !this.mob.getCurrentRaid().isOver() && !getServerLevel(this.mob.level()).isVillage(this.mob.blockPosition());
   }

   @Override
   public void tick() {
      if (this.mob.hasActiveRaid()) {
         Raid raid = this.mob.getCurrentRaid();
         if (this.mob.tickCount > this.recruitmentTick) {
            this.recruitmentTick = this.mob.tickCount + 20;
            this.recruitNearby(raid);
         }

         if (!this.mob.isPathFinding()) {
            Vec3 vec3 = DefaultRandomPos.getPosTowards(this.mob, 15, 4, Vec3.atBottomCenterOf(raid.getCenter()), (float) (Math.PI / 2));
            if (vec3 != null) {
               this.mob.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, 1.0);
            }
         }
      }
   }

   private void recruitNearby(Raid p_25709_) {
      if (p_25709_.isActive()) {
         ServerLevel serverlevel = getServerLevel(this.mob.level());
         Set<Raider> set = Sets.newHashSet();
         List<Raider> list = serverlevel.getEntitiesOfClass(
            Raider.class, this.mob.getBoundingBox().inflate(16.0), p_390635_ -> !p_390635_.hasActiveRaid() && Raids.canJoinRaid(p_390635_)
         );
         set.addAll(list);

         for (Raider raider : set) {
            p_25709_.joinRaid(serverlevel, p_25709_.getGroupsSpawned(), raider, null, true);
         }
      }
   }
}
