package net.minecraft.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.slf4j.Logger;

public class RateKickingConnection extends Connection {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component EXCEED_REASON = Component.translatable("disconnect.exceeded_packet_rate");
   private final int rateLimitPacketsPerSecond;

   public RateKickingConnection(final int rateLimitPacketsPerSecond) {
      super(PacketFlow.SERVERBOUND);
      this.rateLimitPacketsPerSecond = rateLimitPacketsPerSecond;
   }

   @Override
   protected void tickSecond() {
      super.tickSecond();
      float averageReceivedPackets = this.getAverageReceivedPackets();
      if (averageReceivedPackets > this.rateLimitPacketsPerSecond) {
         LOGGER.warn("Player exceeded rate-limit (sent {} packets per second)", averageReceivedPackets);
         this.send(new ClientboundDisconnectPacket(EXCEED_REASON), PacketSendListener.thenRun(() -> this.disconnect(EXCEED_REASON)));
         this.setReadOnly();
      }
   }
}
