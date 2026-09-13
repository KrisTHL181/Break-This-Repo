package net.minecraft.world.entity.ai.goal;

import java.util.List;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;

public class FollowParentGoal extends Goal {
   public static final int HORIZONTAL_SCAN_RANGE = 8;
   public static final int VERTICAL_SCAN_RANGE = 4;
   public static final int DONT_FOLLOW_IF_CLOSER_THAN = 3;
   private final Animal animal;
   private @Nullable Animal parent;
   private final double speedModifier;
   private int timeToRecalcPath;

   public FollowParentGoal(final Animal animal, final double speedModifier) {
      this.animal = animal;
      this.speedModifier = speedModifier;
   }

   @Override
   public boolean canUse() {
      if (this.animal.getAge() >= 0) {
         return false;
      }

      List<? extends Animal> parents = this.animal
         .level()
         .getEntitiesOfClass((Class<? extends Animal>)this.animal.getClass(), this.animal.getBoundingBox().inflate(8.0, 4.0, 8.0));
      Animal closest = null;
      double closestDistSqr = Double.MAX_VALUE;

      for (Animal parent : parents) {
         if (parent.getAge() >= 0) {
            double distSqr = this.animal.distanceToSqr(parent);
            if (!(distSqr > closestDistSqr)) {
               closestDistSqr = distSqr;
               closest = parent;
            }
         }
      }

      if (closest == null) {
         return false;
      }

      if (closestDistSqr < 9.0) {
         return false;
      }

      this.parent = closest;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      if (this.animal.getAge() >= 0) {
         return false;
      }

      if (!this.parent.isAlive()) {
         return false;
      }

      double distSqr = this.animal.distanceToSqr(this.parent);
      return !(distSqr < 9.0) && !(distSqr > 256.0);
   }

   @Override
   public void start() {
      this.timeToRecalcPath = 0;
   }

   @Override
   public void stop() {
      this.parent = null;
   }

   @Override
   public void tick() {
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         this.animal.getNavigation().moveTo(this.parent, this.speedModifier);
      }
   }
}
