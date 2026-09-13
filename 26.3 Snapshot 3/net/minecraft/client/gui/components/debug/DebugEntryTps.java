package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryTps implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      IntegratedServer server = minecraft.getSingleplayerServer();
      ClientPacketListener connectionListener = minecraft.getConnection();
      if (connectionListener != null && serverOrClientLevel != null) {
         Connection connection = connectionListener.getConnection();
         float averageSentPackets = connection.getAverageSentPackets();
         float averageReceivedPackets = connection.getAverageReceivedPackets();
         TickRateManager tickRateManager = serverOrClientLevel.tickRateManager();
         String runStatus;
         if (tickRateManager.isSteppingForward()) {
            runStatus = " (frozen - stepping)";
         } else if (tickRateManager.isFrozen()) {
            runStatus = " (frozen)";
         } else {
            runStatus = "";
         }

         String tps;
         if (server != null) {
            ServerTickRateManager serverTickRateManager = server.tickRateManager();
            boolean isSpriting = serverTickRateManager.isSprinting();
            if (isSpriting) {
               runStatus = " (sprinting)";
            }

            String tpsTarget = isSpriting ? "-" : String.format(Locale.ROOT, "%.1f", tickRateManager.millisecondsPerTick());
            tps = String.format(
               Locale.ROOT,
               "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx",
               server.getCurrentSmoothedTickTime(),
               tpsTarget,
               runStatus,
               averageSentPackets,
               averageReceivedPackets
            );
         } else {
            tps = String.format(
               Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", connectionListener.serverBrand(), runStatus, averageSentPackets, averageReceivedPackets
            );
         }

         displayer.addLine(tps);
      }
   }

   @Override
   public boolean isAllowed(final boolean reducedDebugInfo) {
      return true;
   }
}
