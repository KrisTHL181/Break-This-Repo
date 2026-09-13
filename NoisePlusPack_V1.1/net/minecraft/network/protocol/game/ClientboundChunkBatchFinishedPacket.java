package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundChunkBatchFinishedPacket(int batchSize) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundChunkBatchFinishedPacket> STREAM_CODEC = Packet.codec(
      ClientboundChunkBatchFinishedPacket::write, ClientboundChunkBatchFinishedPacket::new
   );

   private ClientboundChunkBatchFinishedPacket(FriendlyByteBuf p_298630_) {
      this(p_298630_.readVarInt());
   }

   private void write(FriendlyByteBuf p_299639_) {
      p_299639_.writeVarInt(this.batchSize);
   }

   @Override
   public PacketType<ClientboundChunkBatchFinishedPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_FINISHED;
   }

   public void handle(ClientGamePacketListener p_297805_) {
      p_297805_.handleChunkBatchFinished(this);
   }
}
