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

   public PathfindToRaidGoal(final T mob) {
      this.mob = mob;
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
            Vec3 posTowards = DefaultRandomPos.getPosTowards(this.mob, 15, 4, Vec3.atBottomCenterOf(raid.getCenter()), (float) (Math.PI / 2));
            if (posTowards != null) {
               this.mob.getNavigation().moveTo(posTowards.x, posTowards.y, posTowards.z, 1.0);
            }
         }
      }
   }

   private void recruitNearby(final Raid raid) {
      if (raid.isActive()) {
         ServerLevel level = getServerLevel(this.mob.level());
         Set<Raider> raidersToAdd = Sets.newHashSet();
         List<Raider> raidersNearby = level.getEntitiesOfClass(
            Raider.class, this.mob.getBoundingBox().inflate(16.0), mob -> !mob.hasActiveRaid() && Raids.canJoinRaid(mob)
         );
         raidersToAdd.addAll(raidersNearby);

         for (Raider raider : raidersToAdd) {
            raid.joinRaid(level, raid.getGroupsSpawned(), raider, null, true);
         }
      }
   }
}
