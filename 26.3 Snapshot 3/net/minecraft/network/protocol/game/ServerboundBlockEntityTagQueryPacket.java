package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundBlockEntityTagQueryPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundBlockEntityTagQueryPacket> STREAM_CODEC = Packet.codec(
      ServerboundBlockEntityTagQueryPacket::write, ServerboundBlockEntityTagQueryPacket::new
   );
   private final int transactionId;
   private final BlockPos pos;

   public ServerboundBlockEntityTagQueryPacket(final int transactionId, final BlockPos pos) {
      this.transactionId = transactionId;
      this.pos = pos;
   }

   private ServerboundBlockEntityTagQueryPacket(final FriendlyByteBuf input) {
      this.transactionId = input.readVarInt();
      this.pos = input.readBlockPos();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.transactionId);
      output.writeBlockPos(this.pos);
   }

   @Override
   public PacketType<ServerboundBlockEntityTagQueryPacket> type() {
      return GamePacketTypes.SERVERBOUND_BLOCK_ENTITY_TAG_QUERY;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleBlockEntityTagQuery(this);
   }

   public int getTransactionId() {
      return this.transactionId;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
