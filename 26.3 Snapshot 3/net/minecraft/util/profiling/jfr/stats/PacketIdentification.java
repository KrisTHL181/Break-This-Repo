package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record PacketIdentification(String direction, String protocolId, String packetId) {
   public static PacketIdentification from(final RecordedEvent event) {
      return new PacketIdentification(event.getString("packetDirection"), event.getString("protocolId"), event.getString("packetId"));
   }
}
