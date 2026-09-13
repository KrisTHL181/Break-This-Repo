package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundBlockChangedAckPacket(int sequence) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundBlockChangedAckPacket> STREAM_CODEC = Packet.codec(
      ClientboundBlockChangedAckPacket::write, ClientboundBlockChangedAckPacket::new
   );

   private ClientboundBlockChangedAckPacket(FriendlyByteBuf p_237582_) {
      this(p_237582_.readVarInt());
   }

   private void write(FriendlyByteBuf p_237584_) {
      p_237584_.writeVarInt(this.sequence);
   }

   @Override
   public PacketType<ClientboundBlockChangedAckPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_CHANGED_ACK;
   }

   public void handle(ClientGamePacketListener p_237588_) {
      p_237588_.handleBlockChangedAck(this);
   }
}
