package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPongPacket implements Packet<ServerCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPongPacket> STREAM_CODEC = Packet.codec(ServerboundPongPacket::write, ServerboundPongPacket::new);
   private final int id;

   public ServerboundPongPacket(final int id) {
      this.id = id;
   }

   private ServerboundPongPacket(final FriendlyByteBuf input) {
      this.id = input.readInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeInt(this.id);
   }

   @Override
   public PacketType<ServerboundPongPacket> type() {
      return CommonPacketTypes.SERVERBOUND_PONG;
   }

   public void handle(final ServerCommonPacketListener listener) {
      listener.handlePong(this);
   }

   public int getId() {
      return this.id;
   }
}
