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

   public FollowParentGoal(Animal p_25319_, double p_25320_) {
      this.animal = p_25319_;
      this.speedModifier = p_25320_;
   }

   @Override
   public boolean canUse() {
      if (this.animal.getAge() >= 0) {
         return false;
      }

      List<? extends Animal> list = this.animal
         .level()
         .getEntitiesOfClass((Class<? extends Animal>)this.animal.getClass(), this.animal.getBoundingBox().inflate(8.0, 4.0, 8.0));
      Animal animal = null;
      double d0 = Double.MAX_VALUE;

      for (Animal animal1 : list) {
         if (animal1.getAge() >= 0) {
            double d1 = this.animal.distanceToSqr(animal1);
            if (!(d1 > d0)) {
               d0 = d1;
               animal = animal1;
            }
         }
      }

      if (animal == null) {
         return false;
      }

      if (d0 < 9.0) {
         return false;
      }

      this.parent = animal;
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

      double d0 = this.animal.distanceToSqr(this.parent);
      return !(d0 < 9.0) && !(d0 > 256.0);
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
