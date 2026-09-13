package net.minecraft.util.profiling.jfr.event;

import java.net.SocketAddress;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name("minecraft.PacketReceived")
@Label("Network Packet Received")
public class PacketReceivedEvent extends PacketEvent {
   public static final String NAME = "minecraft.PacketReceived";
   public static final EventType TYPE = EventType.getEventType(PacketReceivedEvent.class);

   public PacketReceivedEvent(
      final String protocolId, final String packetDirection, final String packetId, final SocketAddress remoteAddress, final int readableBytes
   ) {
      super(protocolId, packetDirection, packetId, remoteAddress, readableBytes);
   }
}
