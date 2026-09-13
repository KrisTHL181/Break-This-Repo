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

   public TemptGoal(PathfinderMob p_25939_, double p_25940_, Predicate<ItemStack> p_329244_, boolean p_25942_) {
      this((Mob)p_25939_, p_25940_, p_329244_, p_25942_, 2.5);
   }

   public TemptGoal(PathfinderMob p_406939_, double p_408370_, Predicate<ItemStack> p_409411_, boolean p_405821_, double p_409287_) {
      this((Mob)p_406939_, p_408370_, p_409411_, p_405821_, p_409287_);
   }

   TemptGoal(Mob p_405921_, double p_409743_, Predicate<ItemStack> p_409922_, boolean p_406295_, double p_407335_) {
      this.mob = p_405921_;
      this.speedModifier = p_409743_;
      this.items = p_409922_;
      this.canScare = p_406295_;
      this.stopDistance = p_407335_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      this.targetingConditions = TEMPT_TARGETING.copy().selector((p_370043_, p_361565_) -> this.shouldFollow(p_370043_));
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

   private boolean shouldFollow(LivingEntity p_148139_) {
      return this.items.test(p_148139_.getMainHandItem()) || this.items.test(p_148139_.getOffhandItem());
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

   protected void navigateTowards(Player p_407647_) {
      this.mob.getNavigation().moveTo(p_407647_, this.speedModifier);
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   public static class ForNonPathfinders extends TemptGoal {
      public ForNonPathfinders(Mob p_407234_, double p_407396_, Predicate<ItemStack> p_410152_, boolean p_410644_, double p_408141_) {
         super(p_407234_, p_407396_, p_410152_, p_410644_, p_408141_);
      }

      @Override
      protected void stopNavigation() {
         this.mob.getMoveControl().setWait();
      }

      @Override
      protected void navigateTowards(Player p_410163_) {
         Vec3 vec3 = p_410163_.getEyePosition().subtract(this.mob.position()).scale(this.mob.getRandom().nextDouble()).add(this.mob.position());
         this.mob.getMoveControl().setWantedPosition(vec3.x, vec3.y, vec3.z, this.speedModifier);
      }
   }
}
