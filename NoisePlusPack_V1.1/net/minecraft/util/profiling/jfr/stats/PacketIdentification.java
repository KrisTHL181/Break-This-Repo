package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record PacketIdentification(String direction, String protocolId, String packetId) {
   public static PacketIdentification from(RecordedEvent p_333273_) {
      return new PacketIdentification(p_333273_.getString("packetDirection"), p_333273_.getString("protocolId"), p_333273_.getString("packetId"));
   }
}
