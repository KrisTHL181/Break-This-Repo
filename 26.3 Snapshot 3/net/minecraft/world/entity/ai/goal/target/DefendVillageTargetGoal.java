package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class DefendVillageTargetGoal extends TargetGoal {
   private final IronGolem golem;
   private @Nullable LivingEntity potentialTarget;
   private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);

   public DefendVillageTargetGoal(final IronGolem golem) {
      super(golem, false, true);
      this.golem = golem;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      AABB grow = this.golem.getBoundingBox().inflate(10.0, 8.0, 10.0);
      ServerLevel level = getServerLevel(this.golem);
      List<? extends LivingEntity> villagers = level.getNearbyEntities(Villager.class, this.attackTargeting, this.golem, grow);
      List<Player> players = level.getNearbyPlayers(this.attackTargeting, this.golem, grow);

      for (LivingEntity livingEntity : villagers) {
         Villager villager = (Villager)livingEntity;

         for (Player player : players) {
            int reputation = villager.getPlayerReputation(player);
            if (reputation <= -100) {
               this.potentialTarget = player;
            }
         }
      }

      return this.potentialTarget == null ? false : !(this.potentialTarget instanceof Player player && (player.isSpectator() || player.isCreative()));
   }

   @Override
   public void start() {
      this.golem.setTarget(this.potentialTarget);
      super.start();
   }
}
