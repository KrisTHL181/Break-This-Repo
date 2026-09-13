package net.minecraft.client.multiplayer;

import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PingDebugMonitor {
   private final ClientPacketListener connection;
   private final LocalSampleLogger delayTimer;

   public PingDebugMonitor(ClientPacketListener p_300283_, LocalSampleLogger p_334867_) {
      this.connection = p_300283_;
      this.delayTimer = p_334867_;
   }

   public void tick() {
      this.connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
   }

   public void onPongReceived(ClientboundPongResponsePacket p_328021_) {
      this.delayTimer.logSample(Util.getMillis() - p_328021_.time());
   }
}
