package net.minecraft.server;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.TickRateManager;

public class ServerTickRateManager extends TickRateManager {
   private long remainingSprintTicks = 0L;
   private long sprintTickStartTime = 0L;
   private long sprintTimeSpend = 0L;
   private long scheduledCurrentSprintTicks = 0L;
   private boolean previousIsFrozen = false;
   private final MinecraftServer server;

   public ServerTickRateManager(final MinecraftServer server) {
      this.server = server;
   }

   public boolean isSprinting() {
      return this.scheduledCurrentSprintTicks > 0L;
   }

   @Override
   public void setFrozen(final boolean frozen) {
      super.setFrozen(frozen);
      this.updateStateToClients();
   }

   private void updateStateToClients() {
      this.server.getPlayerList().broadcastAll(ClientboundTickingStatePacket.from(this));
   }

   private void updateStepTicks() {
      this.server.getPlayerList().broadcastAll(ClientboundTickingStepPacket.from(this));
   }

   public boolean stepGameIfPaused(final int ticks) {
      if (!this.isFrozen()) {
         return false;
      }

      this.frozenTicksToRun = ticks;
      this.updateStepTicks();
      return true;
   }

   public boolean stopStepping() {
      if (this.frozenTicksToRun > 0) {
         this.frozenTicksToRun = 0;
         this.updateStepTicks();
         return true;
      } else {
         return false;
      }
   }

   public boolean stopSprinting() {
      if (this.remainingSprintTicks > 0L) {
         this.finishTickSprint();
         return true;
      } else {
         return false;
      }
   }

   public boolean requestGameToSprint(final int time) {
      boolean interrupted = this.remainingSprintTicks > 0L;
      this.sprintTimeSpend = 0L;
      this.scheduledCurrentSprintTicks = time;
      this.remainingSprintTicks = time;
      this.previousIsFrozen = this.isFrozen();
      this.setFrozen(false);
      return interrupted;
   }

   private void finishTickSprint() {
      long completedTicks = this.scheduledCurrentSprintTicks - this.remainingSprintTicks;
      double millisecondsToComplete = Math.max(1.0, this.sprintTimeSpend) / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      int ticksPerSecond = (int)(TimeUtil.MILLISECONDS_PER_SECOND * completedTicks / millisecondsToComplete);
      String millisecondsPerTick = String.format(
         Locale.ROOT, "%.2f", completedTicks == 0L ? this.millisecondsPerTick() : millisecondsToComplete / completedTicks
      );
      this.scheduledCurrentSprintTicks = 0L;
      this.sprintTimeSpend = 0L;
      this.server
         .createCommandSourceStack()
         .sendSuccess(() -> Component.translatable("commands.tick.sprint.report", ticksPerSecond, millisecondsPerTick), true);
      this.remainingSprintTicks = 0L;
      this.setFrozen(this.previousIsFrozen);
      this.server.onTickRateChanged();
   }

   public boolean checkShouldSprintThisTick() {
      if (!this.runGameElements) {
         return false;
      } else if (this.remainingSprintTicks > 0L) {
         this.sprintTickStartTime = System.nanoTime();
         this.remainingSprintTicks--;
         return true;
      } else {
         this.finishTickSprint();
         return false;
      }
   }

   public void endTickWork() {
      this.sprintTimeSpend = this.sprintTimeSpend + (System.nanoTime() - this.sprintTickStartTime);
   }

   @Override
   public void setTickRate(final float rate) {
      super.setTickRate(rate);
      this.server.onTickRateChanged();
      this.updateStateToClients();
   }

   public void updateJoiningPlayer(final ServerPlayer player) {
      player.connection.send(ClientboundTickingStatePacket.from(this));
      player.connection.send(ClientboundTickingStepPacket.from(this));
   }
}
