package net.minecraft.network.protocol.cookie;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public class CookiePacketTypes {
   public static final PacketType<ClientboundCookieRequestPacket> CLIENTBOUND_COOKIE_REQUEST = createClientbound("cookie_request");
   public static final PacketType<ServerboundCookieResponsePacket> SERVERBOUND_COOKIE_RESPONSE = createServerbound("cookie_response");

   private static <T extends Packet<ClientCookiePacketListener>> PacketType<T> createClientbound(final String id) {
      return new PacketType<>(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(id));
   }

   private static <T extends Packet<ServerCookiePacketListener>> PacketType<T> createServerbound(final String id) {
      return new PacketType<>(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(id));
   }
}
