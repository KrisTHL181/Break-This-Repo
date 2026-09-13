package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChunkBatchReceivedPacket(float desiredChunksPerTick) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChunkBatchReceivedPacket> STREAM_CODEC = Packet.codec(
      ServerboundChunkBatchReceivedPacket::write, ServerboundChunkBatchReceivedPacket::new
   );

   private ServerboundChunkBatchReceivedPacket(FriendlyByteBuf p_297860_) {
      this(p_297860_.readFloat());
   }

   private void write(FriendlyByteBuf p_299711_) {
      p_299711_.writeFloat(this.desiredChunksPerTick);
   }

   @Override
   public PacketType<ServerboundChunkBatchReceivedPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHUNK_BATCH_RECEIVED;
   }

   public void handle(ServerGamePacketListener p_299816_) {
      p_299816_.handleChunkBatchReceived(this);
   }
}
