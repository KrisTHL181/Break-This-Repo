package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public record ClientboundChunksBiomesPacket(List<ClientboundChunksBiomesPacket.ChunkBiomeData> chunkBiomeData) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundChunksBiomesPacket> STREAM_CODEC = Packet.codec(
      ClientboundChunksBiomesPacket::write, ClientboundChunksBiomesPacket::new
   );
   private static final int TWO_MEGABYTES = 2097152;

   private ClientboundChunksBiomesPacket(final FriendlyByteBuf input) {
      this(input.readList(ClientboundChunksBiomesPacket.ChunkBiomeData::new));
   }

   public static ClientboundChunksBiomesPacket forChunks(final List<LevelChunk> chunks) {
      return new ClientboundChunksBiomesPacket(chunks.stream().map(ClientboundChunksBiomesPacket.ChunkBiomeData::new).toList());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeCollection(this.chunkBiomeData, (o, c) -> c.write(o));
   }

   @Override
   public PacketType<ClientboundChunksBiomesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CHUNKS_BIOMES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleChunksBiomes(this);
   }

   public record ChunkBiomeData(ChunkPos pos, byte[] buffer) {
      public ChunkBiomeData(final LevelChunk chunk) {
         this(chunk.getPos(), new byte[calculateChunkSize(chunk)]);
         extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), chunk);
      }

      public ChunkBiomeData(final FriendlyByteBuf input) {
         this(input.readChunkPos(), input.readByteArray(2097152));
      }

      private static int calculateChunkSize(final LevelChunk chunk) {
         int total = 0;

         for (LevelChunkSection section : chunk.getSections()) {
            total += section.getBiomes().getSerializedSize();
         }

         return total;
      }

      public FriendlyByteBuf getReadBuffer() {
         return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
      }

      private ByteBuf getWriteBuffer() {
         ByteBuf buffer = Unpooled.wrappedBuffer(this.buffer);
         buffer.writerIndex(0);
         return buffer;
      }

      public static void extractChunkData(final FriendlyByteBuf buffer, final LevelChunk chunk) {
         for (LevelChunkSection section : chunk.getSections()) {
            section.getBiomes().write(buffer);
         }

         if (buffer.writerIndex() != buffer.capacity()) {
            throw new IllegalStateException("Didn't fill biome buffer: expected " + buffer.capacity() + " bytes, got " + buffer.writerIndex());
         }
      }

      public void write(final FriendlyByteBuf output) {
         output.writeChunkPos(this.pos);
         output.writeByteArray(this.buffer);
      }
   }
}
