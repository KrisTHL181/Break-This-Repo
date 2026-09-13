package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public abstract class Goal {
   private final EnumSet<Goal.Flag> flags = EnumSet.noneOf(Goal.Flag.class);

   public abstract boolean canUse();

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public boolean isInterruptable() {
      return true;
   }

   public void start() {
   }

   public void stop() {
   }

   public boolean requiresUpdateEveryTick() {
      return false;
   }

   public void tick() {
   }

   public void setFlags(EnumSet<Goal.Flag> p_25328_) {
      this.flags.clear();
      this.flags.addAll(p_25328_);
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName();
   }

   public EnumSet<Goal.Flag> getFlags() {
      return this.flags;
   }

   protected int adjustedTickDelay(int p_186072_) {
      return this.requiresUpdateEveryTick() ? p_186072_ : reducedTickDelay(p_186072_);
   }

   protected static int reducedTickDelay(int p_186074_) {
      return Mth.positiveCeilDiv(p_186074_, 2);
   }

   protected static ServerLevel getServerLevel(Entity p_363316_) {
      return (ServerLevel)p_363316_.level();
   }

   protected static ServerLevel getServerLevel(Level p_366684_) {
      return (ServerLevel)p_366684_;
   }

   public enum Flag {
      MOVE,
      LOOK,
      JUMP,
      TARGET;
   }
}
