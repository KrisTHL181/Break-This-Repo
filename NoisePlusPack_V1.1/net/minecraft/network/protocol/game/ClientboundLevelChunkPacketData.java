package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class ClientboundLevelChunkPacketData {
   private static final StreamCodec<ByteBuf, Map<Heightmap.Types, long[]>> HEIGHTMAPS_STREAM_CODEC = ByteBufCodecs.map(
      p_389934_ -> new EnumMap<>(Heightmap.Types.class), Heightmap.Types.STREAM_CODEC, ByteBufCodecs.LONG_ARRAY
   );
   private static final int TWO_MEGABYTES = 2097152;
   private final Map<Heightmap.Types, long[]> heightmaps;
   private final byte[] buffer;
   private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

   public ClientboundLevelChunkPacketData(LevelChunk p_195651_) {
      this.heightmaps = p_195651_.getHeightmaps()
         .stream()
         .filter(p_389936_ -> p_389936_.getKey().sendToClient())
         .collect(Collectors.toMap(Entry::getKey, p_389935_ -> (long[])p_389935_.getValue().getRawData().clone()));
      this.buffer = new byte[calculateChunkSize(p_195651_)];
      extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), p_195651_);
      this.blockEntitiesData = Lists.newArrayList();

      for (Entry<BlockPos, BlockEntity> entry : p_195651_.getBlockEntities().entrySet()) {
         this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create(entry.getValue()));
      }
   }

   public ClientboundLevelChunkPacketData(RegistryFriendlyByteBuf p_335775_, int p_195654_, int p_195655_) {
      this.heightmaps = HEIGHTMAPS_STREAM_CODEC.decode(p_335775_);
      int i = p_335775_.readVarInt();
      if (i > 2097152) {
         throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
      }

      this.buffer = new byte[i];
      p_335775_.readBytes(this.buffer);
      this.blockEntitiesData = ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.decode(p_335775_);
   }

   public void write(RegistryFriendlyByteBuf p_331012_) {
      HEIGHTMAPS_STREAM_CODEC.encode(p_331012_, this.heightmaps);
      p_331012_.writeVarInt(this.buffer.length);
      p_331012_.writeBytes(this.buffer);
      ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(p_331012_, this.blockEntitiesData);
   }

   private static int calculateChunkSize(LevelChunk p_195665_) {
      int i = 0;

      for (LevelChunkSection levelchunksection : p_195665_.getSections()) {
         i += levelchunksection.getSerializedSize();
      }

      return i;
   }

   private ByteBuf getWriteBuffer() {
      ByteBuf bytebuf = Unpooled.wrappedBuffer(this.buffer);
      bytebuf.writerIndex(0);
      return bytebuf;
   }

   public static void extractChunkData(FriendlyByteBuf p_195669_, LevelChunk p_195670_) {
      for (LevelChunkSection levelchunksection : p_195670_.getSections()) {
         levelchunksection.write(p_195669_);
      }

      if (p_195669_.writerIndex() != p_195669_.capacity()) {
         throw new IllegalStateException("Didn't fill chunk buffer: expected " + p_195669_.capacity() + " bytes, got " + p_195669_.writerIndex());
      }
   }

   public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int p_195658_, int p_195659_) {
      return p_195663_ -> this.getBlockEntitiesTags(p_195663_, p_195658_, p_195659_);
   }

   private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput p_195675_, int p_195676_, int p_195677_) {
      int i = 16 * p_195676_;
      int j = 16 * p_195677_;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (ClientboundLevelChunkPacketData.BlockEntityInfo clientboundlevelchunkpacketdata$blockentityinfo : this.blockEntitiesData) {
         int k = i + SectionPos.sectionRelative(clientboundlevelchunkpacketdata$blockentityinfo.packedXZ >> 4);
         int l = j + SectionPos.sectionRelative(clientboundlevelchunkpacketdata$blockentityinfo.packedXZ);
         blockpos$mutableblockpos.set(k, clientboundlevelchunkpacketdata$blockentityinfo.y, l);
         p_195675_.accept(blockpos$mutableblockpos, clientboundlevelchunkpacketdata$blockentityinfo.type, clientboundlevelchunkpacketdata$blockentityinfo.tag);
      }
   }

   public FriendlyByteBuf getReadBuffer() {
      return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
   }

   public Map<Heightmap.Types, long[]> getHeightmaps() {
      return this.heightmaps;
   }

   static class BlockEntityInfo {
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkPacketData.BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(
         ClientboundLevelChunkPacketData.BlockEntityInfo::write, ClientboundLevelChunkPacketData.BlockEntityInfo::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, List<ClientboundLevelChunkPacketData.BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(
         ByteBufCodecs.list()
      );
      final int packedXZ;
      final int y;
      final BlockEntityType<?> type;
      final @Nullable CompoundTag tag;

      private BlockEntityInfo(int p_195685_, int p_195686_, BlockEntityType<?> p_195687_, @Nullable CompoundTag p_195688_) {
         this.packedXZ = p_195685_;
         this.y = p_195686_;
         this.type = p_195687_;
         this.tag = p_195688_;
      }

      private BlockEntityInfo(RegistryFriendlyByteBuf p_335103_) {
         this.packedXZ = p_335103_.readByte();
         this.y = p_335103_.readShort();
         this.type = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(p_335103_);
         this.tag = p_335103_.readNbt();
      }

      private void write(RegistryFriendlyByteBuf p_332659_) {
         p_332659_.writeByte(this.packedXZ);
         p_332659_.writeShort(this.y);
         ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(p_332659_, this.type);
         p_332659_.writeNbt(this.tag);
      }

      static ClientboundLevelChunkPacketData.BlockEntityInfo create(BlockEntity p_195692_) {
         CompoundTag compoundtag = p_195692_.getUpdateTag(p_195692_.getLevel().registryAccess());
         BlockPos blockpos = p_195692_.getBlockPos();
         int i = SectionPos.sectionRelative(blockpos.getX()) << 4 | SectionPos.sectionRelative(blockpos.getZ());
         return new ClientboundLevelChunkPacketData.BlockEntityInfo(i, blockpos.getY(), p_195692_.getType(), compoundtag.isEmpty() ? null : compoundtag);
      }
   }

   @FunctionalInterface
   public interface BlockEntityTagOutput {
      void accept(BlockPos var1, BlockEntityType<?> var2, @Nullable CompoundTag var3);
   }
}
