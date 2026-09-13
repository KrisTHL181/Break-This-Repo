package net.minecraft.network.protocol.handshake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) implements Packet<ServerHandshakePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientIntentionPacket> STREAM_CODEC = Packet.codec(ClientIntentionPacket::write, ClientIntentionPacket::new);
   private static final int MAX_HOST_LENGTH = 255;

   @Deprecated
   public ClientIntentionPacket {
   }

   private ClientIntentionPacket(FriendlyByteBuf p_179801_) {
      this(p_179801_.readVarInt(), p_179801_.readUtf(255), p_179801_.readUnsignedShort(), ClientIntent.byId(p_179801_.readVarInt()));
   }

   private void write(FriendlyByteBuf p_134737_) {
      p_134737_.writeVarInt(this.protocolVersion);
      p_134737_.writeUtf(this.hostName);
      p_134737_.writeShort(this.port);
      p_134737_.writeVarInt(this.intention.id());
   }

   @Override
   public PacketType<ClientIntentionPacket> type() {
      return HandshakePacketTypes.CLIENT_INTENTION;
   }

   public void handle(ServerHandshakePacketListener p_134734_) {
      p_134734_.handleIntention(this);
   }

   @Override
   public boolean isTerminal() {
      return true;
   }
}
