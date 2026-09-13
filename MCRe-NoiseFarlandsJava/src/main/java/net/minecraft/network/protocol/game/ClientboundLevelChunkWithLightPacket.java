package net.minecraft.network.protocol.game;

import java.util.BitSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ClientboundLevelChunkWithLightPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkWithLightPacket> STREAM_CODEC = Packet.codec(
        ClientboundLevelChunkWithLightPacket::write, ClientboundLevelChunkWithLightPacket::new
    );
    private final int x;
    private final int z;
    private final ClientboundLevelChunkPacketData chunkData;
    private final ClientboundLightUpdatePacketData lightData;

    public ClientboundLevelChunkWithLightPacket(
        final LevelChunk levelChunk,
        final LevelLightEngine lightEngine,
        // 修正：将 @Nullable 移到 LongOpenHashSet 前面
        final it.unimi.dsi.fastutil.longs.@Nullable LongOpenHashSet skyChangedLightSections,
        final it.unimi.dsi.fastutil.longs.@Nullable LongOpenHashSet blockChangedLightSections
    ) {
        ChunkPos chunkPos = levelChunk.getPos();
        this.x = (int)chunkPos.x();
        this.z = (int)chunkPos.z();
        this.chunkData = new ClientboundLevelChunkPacketData(levelChunk);
        // 🔧 MCRe P4b：全量模式用 chunk 窗口锚定（超高世界不能依赖 level 底部光照窗口）
        int winMin;
        int winMax;
        if (levelChunk instanceof net.minecraft.world.level.chunk.WindowedChunk wc) {
            winMin = wc.getWindowMinY();
            winMax = wc.getWindowMaxY();
        } else {
            winMin = lightEngine.getMinLightSection();
            winMax = lightEngine.getMaxLightSection();
        }
        this.lightData = new ClientboundLightUpdatePacketData(
            chunkPos, lightEngine, skyChangedLightSections, blockChangedLightSections, winMin, winMax
        );
    }

    private ClientboundLevelChunkWithLightPacket(final RegistryFriendlyByteBuf input) {
        this.x = input.readInt();
        this.z = input.readInt();
        this.chunkData = new ClientboundLevelChunkPacketData(input, this.x, this.z);
        this.lightData = new ClientboundLightUpdatePacketData(input);
    }

    private void write(final RegistryFriendlyByteBuf output) {
        output.writeInt(this.x);
        output.writeInt(this.z);
        this.chunkData.write(output);
        this.lightData.write(output);
    }

    @Override
    public PacketType<ClientboundLevelChunkWithLightPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT;
    }

    public void handle(final ClientGamePacketListener listener) {
        listener.handleLevelChunkWithLight(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLevelChunkPacketData getChunkData() {
        return this.chunkData;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}