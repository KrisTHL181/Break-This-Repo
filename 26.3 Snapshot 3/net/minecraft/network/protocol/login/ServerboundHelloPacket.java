package net.minecraft.network.protocol.login;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundHelloPacket(String name, UUID profileId) implements Packet<ServerLoginPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundHelloPacket> STREAM_CODEC = Packet.codec(
      ServerboundHelloPacket::write, ServerboundHelloPacket::new
   );

   private ServerboundHelloPacket(final FriendlyByteBuf input) {
      this(input.readUtf(16), input.readUUID());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUtf(this.name, 16);
      output.writeUUID(this.profileId);
   }

   @Override
   public PacketType<ServerboundHelloPacket> type() {
      return LoginPacketTypes.SERVERBOUND_HELLO;
   }

   public void handle(final ServerLoginPacketListener listener) {
      listener.handleHello(this);
   }
}
