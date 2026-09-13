package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.world.level.ChunkPos;

public record ClientboundDebugChunkValuePacket(ChunkPos chunkPos, DebugSubscription.Update<?> update) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugChunkValuePacket> STREAM_CODEC = StreamCodec.composite(
      ChunkPos.STREAM_CODEC,
      ClientboundDebugChunkValuePacket::chunkPos,
      DebugSubscription.Update.STREAM_CODEC,
      ClientboundDebugChunkValuePacket::update,
      ClientboundDebugChunkValuePacket::new
   );

   @Override
   public PacketType<ClientboundDebugChunkValuePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_CHUNK_VALUE;
   }

   public void handle(ClientGamePacketListener p_427327_) {
      p_427327_.handleDebugChunkValue(this);
   }
}
