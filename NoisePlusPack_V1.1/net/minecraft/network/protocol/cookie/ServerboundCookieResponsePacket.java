package net.minecraft.network.protocol.cookie;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record ServerboundCookieResponsePacket(Identifier key, byte @Nullable [] payload) implements Packet<ServerCookiePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundCookieResponsePacket> STREAM_CODEC = Packet.codec(
      ServerboundCookieResponsePacket::write, ServerboundCookieResponsePacket::new
   );

   private ServerboundCookieResponsePacket(FriendlyByteBuf p_335580_) {
      this(p_335580_.readIdentifier(), p_335580_.readNullable(ClientboundStoreCookiePacket.PAYLOAD_STREAM_CODEC));
   }

   private void write(FriendlyByteBuf p_329068_) {
      p_329068_.writeIdentifier(this.key);
      p_329068_.writeNullable(this.payload, ClientboundStoreCookiePacket.PAYLOAD_STREAM_CODEC);
   }

   @Override
   public PacketType<ServerboundCookieResponsePacket> type() {
      return CookiePacketTypes.SERVERBOUND_COOKIE_RESPONSE;
   }

   public void handle(ServerCookiePacketListener p_329041_) {
      p_329041_.handleCookieResponse(this);
   }
}
