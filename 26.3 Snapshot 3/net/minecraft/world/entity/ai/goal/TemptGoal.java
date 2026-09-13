package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TemptGoal extends Goal {
   private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().ignoreLineOfSight();
   private static final double DEFAULT_STOP_DISTANCE = 2.5;
   private final TargetingConditions targetingConditions;
   protected final Mob mob;
   protected final double speedModifier;
   private double px;
   private double py;
   private double pz;
   private double pRotX;
   private double pRotY;
   protected @Nullable Player player;
   private int calmDown;
   private boolean isRunning;
   private final Predicate<ItemStack> items;
   private final boolean canScare;
   private final double stopDistance;

   public TemptGoal(final PathfinderMob mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare) {
      this((Mob)mob, speedModifier, items, canScare, 2.5);
   }

   public TemptGoal(final PathfinderMob mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare, final double stopDistance) {
      this((Mob)mob, speedModifier, items, canScare, stopDistance);
   }

   private TemptGoal(final Mob mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare, final double stopDistance) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.items = items;
      this.canScare = canScare;
      this.stopDistance = stopDistance;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      this.targetingConditions = TEMPT_TARGETING.copy().selector((target, level) -> this.shouldFollow(target));
   }

   @Override
   public boolean canUse() {
      if (this.calmDown > 0) {
         this.calmDown--;
         return false;
      } else {
         this.player = getServerLevel(this.mob).getNearestPlayer(this.targetingConditions.range(this.mob.getAttributeValue(Attributes.TEMPT_RANGE)), this.mob);
         return this.player != null;
      }
   }

   private boolean shouldFollow(final LivingEntity player) {
      return this.items.test(player.getMainHandItem()) || this.items.test(player.getOffhandItem());
   }

   @Override
   public boolean canContinueToUse() {
      if (this.canScare()) {
         if (this.mob.distanceToSqr(this.player) < 36.0) {
            if (this.player.distanceToSqr(this.px, this.py, this.pz) > 0.010000000000000002) {
               return false;
            }

            if (Math.abs(this.player.getXRot() - this.pRotX) > 5.0 || Math.abs(this.player.getYRot() - this.pRotY) > 5.0) {
               return false;
            }
         } else {
            this.px = this.player.getX();
            this.py = this.player.getY();
            this.pz = this.player.getZ();
         }

         this.pRotX = this.player.getXRot();
         this.pRotY = this.player.getYRot();
      }

      return this.canUse();
   }

   protected boolean canScare() {
      return this.canScare;
   }

   @Override
   public void start() {
      this.px = this.player.getX();
      this.py = this.player.getY();
      this.pz = this.player.getZ();
      this.isRunning = true;
   }

   @Override
   public void stop() {
      this.player = null;
      this.stopNavigation();
      this.calmDown = reducedTickDelay(100);
      this.isRunning = false;
   }

   @Override
   public void tick() {
      this.mob.getLookControl().setLookAt(this.player, this.mob.getMaxHeadYRot() + 20, this.mob.getMaxHeadXRot());
      if (this.mob.distanceToSqr(this.player) < this.stopDistance * this.stopDistance) {
         this.stopNavigation();
      } else {
         this.navigateTowards(this.player);
      }
   }

   protected void stopNavigation() {
      this.mob.getNavigation().stop();
   }

   protected void navigateTowards(final Player player) {
      this.mob.getNavigation().moveTo(player, this.speedModifier);
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   public static class ForNonPathfinders extends TemptGoal {
      public ForNonPathfinders(final Mob mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare, final double stopDistance) {
         super(mob, speedModifier, items, canScare, stopDistance);
      }

      @Override
      protected void stopNavigation() {
         this.mob.getMoveControl().setWait();
      }

      @Override
      protected void navigateTowards(final Player player) {
         Vec3 target = player.getEyePosition().subtract(this.mob.position()).scale(this.mob.getRandom().nextDouble()).add(this.mob.position());
         this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, this.speedModifier);
      }
   }
}
