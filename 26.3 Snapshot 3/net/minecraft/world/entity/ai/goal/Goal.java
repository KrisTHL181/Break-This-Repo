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

   public void setFlags(final EnumSet<Goal.Flag> requiredControlFlags) {
      this.flags.clear();
      this.flags.addAll(requiredControlFlags);
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName();
   }

   public EnumSet<Goal.Flag> getFlags() {
      return this.flags;
   }

   protected int adjustedTickDelay(final int ticks) {
      return this.requiresUpdateEveryTick() ? ticks : reducedTickDelay(ticks);
   }

   protected static int reducedTickDelay(final int ticks) {
      return Mth.positiveCeilDiv(ticks, 2);
   }

   protected static ServerLevel getServerLevel(final Entity entity) {
      return (ServerLevel)entity.level();
   }

   protected static ServerLevel getServerLevel(final Level level) {
      return (ServerLevel)level;
   }

   public enum Flag {
      MOVE,
      LOOK,
      JUMP,
      TARGET;
   }
}
