package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import org.jspecify.annotations.Nullable;

public class WrappedGoal extends Goal {
   private final Goal goal;
   private final int priority;
   private boolean isRunning;

   public WrappedGoal(final int priority, final Goal goal) {
      this.priority = priority;
      this.goal = goal;
   }

   public boolean canBeReplacedBy(final WrappedGoal goal) {
      return this.isInterruptable() && goal.getPriority() < this.getPriority();
   }

   @Override
   public boolean canUse() {
      return this.goal.canUse();
   }

   @Override
   public boolean canContinueToUse() {
      return this.goal.canContinueToUse();
   }

   @Override
   public boolean isInterruptable() {
      return this.goal.isInterruptable();
   }

   @Override
   public void start() {
      if (!this.isRunning) {
         this.isRunning = true;
         this.goal.start();
      }
   }

   @Override
   public void stop() {
      if (this.isRunning) {
         this.isRunning = false;
         this.goal.stop();
      }
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return this.goal.requiresUpdateEveryTick();
   }

   @Override
   protected int adjustedTickDelay(final int ticks) {
      return this.goal.adjustedTickDelay(ticks);
   }

   @Override
   public void tick() {
      this.goal.tick();
   }

   @Override
   public void setFlags(final EnumSet<Goal.Flag> requiredControlFlags) {
      this.goal.setFlags(requiredControlFlags);
   }

   @Override
   public EnumSet<Goal.Flag> getFlags() {
      return this.goal.getFlags();
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   public int getPriority() {
      return this.priority;
   }

   public Goal getGoal() {
      return this.goal;
   }

   @Override
   public boolean equals(final @Nullable Object o) {
      if (this == o) {
         return true;
      } else {
         return o != null && this.getClass() == o.getClass() ? this.goal.equals(((WrappedGoal)o).goal) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.goal.hashCode();
   }
}
