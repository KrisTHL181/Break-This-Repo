package net.minecraft.world.entity.ai.goal.target;

import java.util.List;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

public class ResetUniversalAngerTargetGoal<T extends Mob & NeutralMob> extends Goal {
   private static final int ALERT_RANGE_Y = 10;
   private final T mob;
   private final boolean alertOthersOfSameType;
   private int lastHurtByPlayerTimestamp;

   public ResetUniversalAngerTargetGoal(final T mob, final boolean alertOthersOfSameType) {
      this.mob = mob;
      this.alertOthersOfSameType = alertOthersOfSameType;
   }

   @Override
   public boolean canUse() {
      return getServerLevel(this.mob).getGameRules().get(GameRules.UNIVERSAL_ANGER) && this.wasHurtByPlayer();
   }

   private boolean wasHurtByPlayer() {
      return this.mob.getLastHurtByMob() != null
         && this.mob.getLastHurtByMob().is(EntityTypes.PLAYER)
         && this.mob.getLastHurtByMobTimestamp() > this.lastHurtByPlayerTimestamp;
   }

   @Override
   public void start() {
      this.lastHurtByPlayerTimestamp = this.mob.getLastHurtByMobTimestamp();
      this.mob.forgetCurrentTargetAndRefreshUniversalAnger();
      if (this.alertOthersOfSameType) {
         this.getNearbyMobsOfSameType()
            .stream()
            .filter(otherMob -> otherMob != this.mob)
            .map(otherMob -> (NeutralMob)otherMob)
            .forEach(NeutralMob::forgetCurrentTargetAndRefreshUniversalAnger);
      }

      super.start();
   }

   private List<? extends Mob> getNearbyMobsOfSameType() {
      double within = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
      AABB searchAabb = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(within, 10.0, within);
      return this.mob.level().getEntitiesOfClass((Class<? extends Mob>)this.mob.getClass(), searchAabb, EntitySelector.NO_SPECTATORS);
   }
}
