package net.minecraft.world.entity.ai.goal;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public class GoalSelector {
   private static final WrappedGoal NO_GOAL = new WrappedGoal(Integer.MAX_VALUE, new Goal() {
      @Override
      public boolean canUse() {
         return false;
      }
   }) {
      @Override
      public boolean isRunning() {
         return false;
      }
   };
   private final Map<Goal.Flag, WrappedGoal> lockedFlags = new EnumMap<>(Goal.Flag.class);
   private final Set<WrappedGoal> availableGoals = new ObjectLinkedOpenHashSet();
   private final EnumSet<Goal.Flag> disabledFlags = EnumSet.noneOf(Goal.Flag.class);

   public void addGoal(int p_25353_, Goal p_25354_) {
      this.availableGoals.add(new WrappedGoal(p_25353_, p_25354_));
   }

   public void removeAllGoals(Predicate<Goal> p_262575_) {
      this.availableGoals.removeIf(p_262564_ -> p_262575_.test(p_262564_.getGoal()));
   }

   public void removeGoal(Goal p_25364_) {
      for (WrappedGoal wrappedgoal : this.availableGoals) {
         if (wrappedgoal.getGoal() == p_25364_ && wrappedgoal.isRunning()) {
            wrappedgoal.stop();
         }
      }

      this.availableGoals.removeIf(p_25378_ -> p_25378_.getGoal() == p_25364_);
   }

   private static boolean goalContainsAnyFlags(WrappedGoal p_186076_, EnumSet<Goal.Flag> p_186077_) {
      for (Goal.Flag goal$flag : p_186076_.getFlags()) {
         if (p_186077_.contains(goal$flag)) {
            return true;
         }
      }

      return false;
   }

   private static boolean goalCanBeReplacedForAllFlags(WrappedGoal p_186079_, Map<Goal.Flag, WrappedGoal> p_186080_) {
      for (Goal.Flag goal$flag : p_186079_.getFlags()) {
         if (!p_186080_.getOrDefault(goal$flag, NO_GOAL).canBeReplacedBy(p_186079_)) {
            return false;
         }
      }

      return true;
   }

   public void tick() {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("goalCleanup");

      for (WrappedGoal wrappedgoal : this.availableGoals) {
         if (wrappedgoal.isRunning() && (goalContainsAnyFlags(wrappedgoal, this.disabledFlags) || !wrappedgoal.canContinueToUse())) {
            wrappedgoal.stop();
         }
      }

      this.lockedFlags.entrySet().removeIf(p_326929_ -> !p_326929_.getValue().isRunning());
      profilerfiller.pop();
      profilerfiller.push("goalUpdate");

      for (WrappedGoal wrappedgoal2 : this.availableGoals) {
         if (!wrappedgoal2.isRunning()
            && !goalContainsAnyFlags(wrappedgoal2, this.disabledFlags)
            && goalCanBeReplacedForAllFlags(wrappedgoal2, this.lockedFlags)
            && wrappedgoal2.canUse()) {
            for (Goal.Flag goal$flag : wrappedgoal2.getFlags()) {
               WrappedGoal wrappedgoal1 = this.lockedFlags.getOrDefault(goal$flag, NO_GOAL);
               wrappedgoal1.stop();
               this.lockedFlags.put(goal$flag, wrappedgoal2);
            }

            wrappedgoal2.start();
         }
      }

      profilerfiller.pop();
      this.tickRunningGoals(true);
   }

   public void tickRunningGoals(boolean p_186082_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("goalTick");

      for (WrappedGoal wrappedgoal : this.availableGoals) {
         if (wrappedgoal.isRunning() && (p_186082_ || wrappedgoal.requiresUpdateEveryTick())) {
            wrappedgoal.tick();
         }
      }

      profilerfiller.pop();
   }

   public Set<WrappedGoal> getAvailableGoals() {
      return this.availableGoals;
   }

   public void disableControlFlag(Goal.Flag p_25356_) {
      this.disabledFlags.add(p_25356_);
   }

   public void enableControlFlag(Goal.Flag p_25375_) {
      this.disabledFlags.remove(p_25375_);
   }

   public void setControlFlag(Goal.Flag p_25361_, boolean p_25362_) {
      if (p_25362_) {
         this.enableControlFlag(p_25361_);
      } else {
         this.disableControlFlag(p_25361_);
      }
   }
}
