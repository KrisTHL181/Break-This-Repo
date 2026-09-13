package net.minecraft.network.protocol.ping;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public class PingPacketTypes {
   public static final PacketType<ClientboundPongResponsePacket> CLIENTBOUND_PONG_RESPONSE = createClientbound("pong_response");
   public static final PacketType<ServerboundPingRequestPacket> SERVERBOUND_PING_REQUEST = createServerbound("ping_request");

   private static <T extends Packet<ClientPongPacketListener>> PacketType<T> createClientbound(String p_331071_) {
      return new PacketType<>(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(p_331071_));
   }

   private static <T extends Packet<ServerPingPacketListener>> PacketType<T> createServerbound(String p_330390_) {
      return new PacketType<>(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(p_330390_));
   }
}
