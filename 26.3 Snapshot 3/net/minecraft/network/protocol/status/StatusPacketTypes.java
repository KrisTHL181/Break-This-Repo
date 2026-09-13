package net.minecraft.network.protocol.status;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public class StatusPacketTypes {
   public static final PacketType<ClientboundStatusResponsePacket> CLIENTBOUND_STATUS_RESPONSE = createClientbound("status_response");
   public static final PacketType<ServerboundStatusRequestPacket> SERVERBOUND_STATUS_REQUEST = createServerbound("status_request");

   private static <T extends Packet<ClientStatusPacketListener>> PacketType<T> createClientbound(final String id) {
      return new PacketType<>(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(id));
   }

   private static <T extends Packet<ServerStatusPacketListener>> PacketType<T> createServerbound(final String id) {
      return new PacketType<>(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(id));
   }
}
