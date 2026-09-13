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

   private ServerboundHelloPacket(FriendlyByteBuf p_179827_) {
      this(p_179827_.readUtf(16), p_179827_.readUUID());
   }

   private void write(FriendlyByteBuf p_134851_) {
      p_134851_.writeUtf(this.name, 16);
      p_134851_.writeUUID(this.profileId);
   }

   @Override
   public PacketType<ServerboundHelloPacket> type() {
      return LoginPacketTypes.SERVERBOUND_HELLO;
   }

   public void handle(ServerLoginPacketListener p_134848_) {
      p_134848_.handleHello(this);
   }
}
