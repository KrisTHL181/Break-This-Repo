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

   public ServerTickRateManager(MinecraftServer p_311395_) {
      this.server = p_311395_;
   }

   public boolean isSprinting() {
      return this.scheduledCurrentSprintTicks > 0L;
   }

   @Override
   public void setFrozen(boolean p_313235_) {
      super.setFrozen(p_313235_);
      this.updateStateToClients();
   }

   private void updateStateToClients() {
      this.server.getPlayerList().broadcastAll(ClientboundTickingStatePacket.from(this));
   }

   private void updateStepTicks() {
      this.server.getPlayerList().broadcastAll(ClientboundTickingStepPacket.from(this));
   }

   public boolean stepGameIfPaused(int p_312205_) {
      if (!this.isFrozen()) {
         return false;
      }

      this.frozenTicksToRun = p_312205_;
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

   public boolean requestGameToSprint(int p_311983_) {
      boolean flag = this.remainingSprintTicks > 0L;
      this.sprintTimeSpend = 0L;
      this.scheduledCurrentSprintTicks = p_311983_;
      this.remainingSprintTicks = p_311983_;
      this.previousIsFrozen = this.isFrozen();
      this.setFrozen(false);
      return flag;
   }

   private void finishTickSprint() {
      long i = this.scheduledCurrentSprintTicks - this.remainingSprintTicks;
      double d0 = Math.max(1.0, this.sprintTimeSpend) / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      int j = (int)(TimeUtil.MILLISECONDS_PER_SECOND * i / d0);
      String s = String.format(Locale.ROOT, "%.2f", i == 0L ? this.millisecondsPerTick() : d0 / i);
      this.scheduledCurrentSprintTicks = 0L;
      this.sprintTimeSpend = 0L;
      this.server.createCommandSourceStack().sendSuccess(() -> Component.translatable("commands.tick.sprint.report", j, s), true);
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
   public void setTickRate(float p_312065_) {
      super.setTickRate(p_312065_);
      this.server.onTickRateChanged();
      this.updateStateToClients();
   }

   public void updateJoiningPlayer(ServerPlayer p_310808_) {
      p_310808_.connection.send(ClientboundTickingStatePacket.from(this));
      p_310808_.connection.send(ClientboundTickingStepPacket.from(this));
   }
}
