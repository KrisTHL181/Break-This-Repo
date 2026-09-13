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

   private ClientboundChunksBiomesPacket(FriendlyByteBuf p_275221_) {
      this(p_275221_.readList(ClientboundChunksBiomesPacket.ChunkBiomeData::new));
   }

   public static ClientboundChunksBiomesPacket forChunks(List<LevelChunk> p_275394_) {
      return new ClientboundChunksBiomesPacket(p_275394_.stream().map(ClientboundChunksBiomesPacket.ChunkBiomeData::new).toList());
   }

   private void write(FriendlyByteBuf p_275376_) {
      p_275376_.writeCollection(this.chunkBiomeData, (p_275199_, p_275200_) -> p_275200_.write(p_275199_));
   }

   @Override
   public PacketType<ClientboundChunksBiomesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CHUNKS_BIOMES;
   }

   public void handle(ClientGamePacketListener p_275524_) {
      p_275524_.handleChunksBiomes(this);
   }

   public record ChunkBiomeData(ChunkPos pos, byte[] buffer) {
      public ChunkBiomeData(LevelChunk p_275569_) {
         this(p_275569_.getPos(), new byte[calculateChunkSize(p_275569_)]);
         extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), p_275569_);
      }

      public ChunkBiomeData(FriendlyByteBuf p_275255_) {
         this(p_275255_.readChunkPos(), p_275255_.readByteArray(2097152));
      }

      private static int calculateChunkSize(LevelChunk p_275324_) {
         int i = 0;

         for (LevelChunkSection levelchunksection : p_275324_.getSections()) {
            i += levelchunksection.getBiomes().getSerializedSize();
         }

         return i;
      }

      public FriendlyByteBuf getReadBuffer() {
         return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
      }

      private ByteBuf getWriteBuffer() {
         ByteBuf bytebuf = Unpooled.wrappedBuffer(this.buffer);
         bytebuf.writerIndex(0);
         return bytebuf;
      }

      public static void extractChunkData(FriendlyByteBuf p_275626_, LevelChunk p_275570_) {
         for (LevelChunkSection levelchunksection : p_275570_.getSections()) {
            levelchunksection.getBiomes().write(p_275626_);
         }

         if (p_275626_.writerIndex() != p_275626_.capacity()) {
            throw new IllegalStateException("Didn't fill biome buffer: expected " + p_275626_.capacity() + " bytes, got " + p_275626_.writerIndex());
         }
      }

      public void write(FriendlyByteBuf p_275467_) {
         p_275467_.writeChunkPos(this.pos);
         p_275467_.writeByteArray(this.buffer);
      }
   }
}
