package net.minecraft.client.multiplayer;

import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;
import net.minecraft.util.debugchart.LocalSampleLogger;

public class PingDebugMonitor {
   private final ClientPacketListener connection;
   private final LocalSampleLogger delayTimer;

   public PingDebugMonitor(final ClientPacketListener connection, final LocalSampleLogger delayTimer) {
      this.connection = connection;
      this.delayTimer = delayTimer;
   }

   public void tick() {
      this.connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
   }

   public void onPongReceived(final ClientboundPongResponsePacket packet) {
      this.delayTimer.logSample(Util.getMillis() - packet.time());
   }
}
