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
        size -> new EnumMap<>(Heightmap.Types.class), Heightmap.Types.STREAM_CODEC, ByteBufCodecs.LONG_ARRAY
    );
    private static final int TWO_MEGABYTES = 2097152;
    private final Map<Heightmap.Types, long[]> heightmaps;
    private final byte[] buffer;
    private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

    public ClientboundLevelChunkPacketData(final LevelChunk levelChunk) {
        this.heightmaps = levelChunk.getHeightmaps()
            .stream()
            .filter(entryx -> ((Heightmap.Types)entryx.getKey()).sendToClient())
            .collect(Collectors.toMap(Entry::getKey, entryx -> (long[])((Heightmap)entryx.getValue()).getRawData().clone()));
        this.buffer = new byte[calculateChunkSize(levelChunk)];
        extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), levelChunk);
        this.blockEntitiesData = Lists.newArrayList();

        for (Entry<BlockPos, BlockEntity> entry : levelChunk.getBlockEntities().entrySet()) {
            this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create(entry.getValue()));
        }
    }

    public ClientboundLevelChunkPacketData(final RegistryFriendlyByteBuf input, final int x, final int z) {
        this.heightmaps = HEIGHTMAPS_STREAM_CODEC.decode(input);
        int size = input.readVarInt();
        if (size > 2097152) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        }

        this.buffer = new byte[size];
        input.readBytes(this.buffer);
        this.blockEntitiesData = ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.decode(input);
    }

    public void write(final RegistryFriendlyByteBuf output) {
        HEIGHTMAPS_STREAM_CODEC.encode(output, this.heightmaps);
        output.writeVarInt(this.buffer.length);
        output.writeBytes(this.buffer);
        ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(output, this.blockEntitiesData);
    }

    // 🔧 MCRe：窗口过滤——只发送窗口内非空 section（按绝对 sectionY），供写侧遍历（块/生态系包共用）
    static java.util.List<java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection>> sendableSections(final LevelChunk chunk) {
        java.util.List<java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection>> out = new java.util.ArrayList<>();
        if (chunk instanceof net.minecraft.world.level.chunk.WindowedChunk wc) {
            int minY = wc.getWindowMinY();
            int maxY = wc.getWindowMaxY();
            for (java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection> e : wc.windowedAllSections().entrySet()) {
                int sy = e.getKey();
                net.minecraft.world.level.chunk.LevelChunkSection s = e.getValue();
                if (sy >= minY && sy <= maxY && s != null && !s.hasOnlyAir()) {
                    out.add(e);
                }
            }
            out.sort(java.util.Comparator.comparingInt(java.util.Map.Entry::getKey));
        } else {
            int base = chunk.getMinSectionY();
            int i = 0;
            for (net.minecraft.world.level.chunk.LevelChunkSection s : chunk.getSections()) {
                if (s != null && !s.hasOnlyAir()) {
                    out.add(java.util.Map.entry(base + i, s));
                }
                i++;
            }
        }
        return out;
    }

    private static int calculateChunkSize(final LevelChunk chunk) {
        int total = 0;
        // 🔧 MCRe：窗口过滤 + 每 section 带绝对 sectionY（5 = varInt 上限）
        for (java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection> e : sendableSections(chunk)) {
            total += 5 + e.getValue().getSerializedSize();
        }
        return total + 5;
    }

    private ByteBuf getWriteBuffer() {
        ByteBuf buffer = Unpooled.wrappedBuffer(this.buffer);
        buffer.writerIndex(0);
        return buffer;
    }

    public static void extractChunkData(final FriendlyByteBuf buffer, final LevelChunk chunk) {
        // 🔧 MCRe：写入 section 数量 + 每节 (绝对 sectionY, data)——支持任意高度世界，
        // 不再依赖服务端/客户端 layout 一致的顺序数组
        java.util.List<java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection>> toSend = sendableSections(chunk);
        buffer.writeVarInt(toSend.size());
        for (java.util.Map.Entry<Integer, net.minecraft.world.level.chunk.LevelChunkSection> e : toSend) {
            buffer.writeVarInt(e.getKey());
            e.getValue().write(buffer);
        }
    }

    public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(final int x, final int z) {
        return output -> this.getBlockEntitiesTags(output, x, z);
    }

    private void getBlockEntitiesTags(final ClientboundLevelChunkPacketData.BlockEntityTagOutput output, final int x, final int z) {
        int baseX = 16 * x;
        int baseZ = 16 * z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (ClientboundLevelChunkPacketData.BlockEntityInfo data : this.blockEntitiesData) {
            int unpackedX = baseX + SectionPos.sectionRelative(data.packedXZ >> 4);
            int unpackedZ = baseZ + SectionPos.sectionRelative(data.packedXZ);
            pos.set(unpackedX, data.y, unpackedZ);
            output.accept(pos, data.type, data.tag);
        }
    }

    public FriendlyByteBuf getReadBuffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
    }

    public Map<Heightmap.Types, long[]> getHeightmaps() {
        return this.heightmaps;
    }

    private static class BlockEntityInfo {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkPacketData.BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(
            ClientboundLevelChunkPacketData.BlockEntityInfo::write, ClientboundLevelChunkPacketData.BlockEntityInfo::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<ClientboundLevelChunkPacketData.BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(
            ByteBufCodecs.list()
        );
        private final int packedXZ;
        private final int y;
        private final BlockEntityType<?> type;
        private final @Nullable CompoundTag tag;

        private BlockEntityInfo(final int packedXZ, final int y, final BlockEntityType<?> type, final @Nullable CompoundTag tag) {
            this.packedXZ = packedXZ;
            this.y = y;
            this.type = type;
            this.tag = tag;
        }

        private BlockEntityInfo(final RegistryFriendlyByteBuf input) {
            this.packedXZ = input.readByte();
            this.y = input.readShort();
            this.type = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(input);
            this.tag = input.readNbt();
        }

        private void write(final RegistryFriendlyByteBuf output) {
            output.writeByte(this.packedXZ);
            output.writeShort(this.y);
            ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(output, this.type);
            output.writeNbt(this.tag);
        }

        private static ClientboundLevelChunkPacketData.BlockEntityInfo create(final BlockEntity blockEntity) {
            CompoundTag tag = blockEntity.getUpdateTag(blockEntity.getLevel().registryAccess());
            BlockPos pos = blockEntity.getBlockPos();
            int xz = SectionPos.sectionRelative(pos.getX()) << 4 | SectionPos.sectionRelative(pos.getZ());
            return new ClientboundLevelChunkPacketData.BlockEntityInfo(xz, pos.getY(), blockEntity.getType(), tag.isEmpty() ? null : tag);
        }
    }

    @FunctionalInterface
    public interface BlockEntityTagOutput {
        void accept(BlockPos pos, BlockEntityType<?> type, @Nullable CompoundTag tag);
    }
}