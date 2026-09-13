package net.minecraft.network.protocol.cookie;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public class CookiePacketTypes {
   public static final PacketType<ClientboundCookieRequestPacket> CLIENTBOUND_COOKIE_REQUEST = createClientbound("cookie_request");
   public static final PacketType<ServerboundCookieResponsePacket> SERVERBOUND_COOKIE_RESPONSE = createServerbound("cookie_response");

   private static <T extends Packet<ClientCookiePacketListener>> PacketType<T> createClientbound(String p_334424_) {
      return new PacketType<>(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(p_334424_));
   }

   private static <T extends Packet<ServerCookiePacketListener>> PacketType<T> createServerbound(String p_334543_) {
      return new PacketType<>(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(p_334543_));
   }
}
