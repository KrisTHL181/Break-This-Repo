package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

/**
 * ClientboundLightUpdatePacketData — 光照增量数据（MCRe NoiseFarlands P4b 版）
 *
 * <p>🔧 相对原版核心改动：wire 格式从「BitSet mask + 隐式 getMinLightSection()+index」
 * 改为「绝对 sectionY 直编」——超高世界光照窗口锚定 -1.34 亿，隐式索引全错位且
 * BitSet 差值溢出。现在每层为：
 * <pre>
 *   VarInt dataCount
 *   for each: VarInt sectionY + byte[2048]
 *   VarInt emptyCount
 *   for each: VarInt sectionY            （空层只发 sectionY，不发 2048 字节）
 * </pre>
 * 服务端/客户端同源码整包改造，无协议兼容负担。
 */
public class ClientboundLightUpdatePacketData {
    private static final StreamCodec<ByteBuf, byte[]> DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(2048);
    /** 有数据的绝对 sectionY 列表（与 skyUpdates 对齐） */
    private final List<Integer> skySectionYs;
    private final List<Integer> blockSectionYs;
    /** 空数据的绝对 sectionY 列表 */
    private final List<Integer> emptySkySectionYs;
    private final List<Integer> emptyBlockSectionYs;
    private final List<byte[]> skyUpdates;
    private final List<byte[]> blockUpdates;

    /**
     * 构造光照增量包数据。
     *
     * @param skyChangedSections  sky 变化的绝对 sectionY 集合；null = 全量发送
     *                            （windowMinSection..windowMaxSection 全窗口遍历）
     * @param blockChangedSections block 同理
     * @param windowMinSection     全量模式窗口锚定（chunk 窗口 - padding）
     * @param windowMaxSection     全量模式窗口锚定（chunk 窗口 + padding）
     */
    public ClientboundLightUpdatePacketData(
        final ChunkPos chunkPos,
        final LevelLightEngine lightEngine,
        final @Nullable LongOpenHashSet skyChangedSections,
        final @Nullable LongOpenHashSet blockChangedSections,
        final int windowMinSection,
        final int windowMaxSection
    ) {
        this.skySectionYs = new ArrayList<>();
        this.blockSectionYs = new ArrayList<>();
        this.emptySkySectionYs = new ArrayList<>();
        this.emptyBlockSectionYs = new ArrayList<>();
        this.skyUpdates = new ArrayList<>();
        this.blockUpdates = new ArrayList<>();

        if (skyChangedSections == null) {
            for (int sectionY = windowMinSection; sectionY <= windowMaxSection; sectionY++) {
                this.prepareSectionData(chunkPos, lightEngine, LightLayer.SKY, sectionY, this.skySectionYs, this.emptySkySectionYs, this.skyUpdates);
            }
        } else {
            for (long v : skyChangedSections) {
                this.prepareSectionData(chunkPos, lightEngine, LightLayer.SKY, (int)v, this.skySectionYs, this.emptySkySectionYs, this.skyUpdates);
            }
        }

        if (blockChangedSections == null) {
            for (int sectionY = windowMinSection; sectionY <= windowMaxSection; sectionY++) {
                this.prepareSectionData(chunkPos, lightEngine, LightLayer.BLOCK, sectionY, this.blockSectionYs, this.emptyBlockSectionYs, this.blockUpdates);
            }
        } else {
            for (long v : blockChangedSections) {
                this.prepareSectionData(chunkPos, lightEngine, LightLayer.BLOCK, (int)v, this.blockSectionYs, this.emptyBlockSectionYs, this.blockUpdates);
            }
        }
    }

    public ClientboundLightUpdatePacketData(final FriendlyByteBuf input) {
        this.skySectionYs = new ArrayList<>();
        this.blockSectionYs = new ArrayList<>();
        this.emptySkySectionYs = new ArrayList<>();
        this.emptyBlockSectionYs = new ArrayList<>();
        this.skyUpdates = new ArrayList<>();
        this.blockUpdates = new ArrayList<>();

        readLayer(input, this.skySectionYs, this.emptySkySectionYs, this.skyUpdates);
        readLayer(input, this.blockSectionYs, this.emptyBlockSectionYs, this.blockUpdates);
    }

    private static void readLayer(
        final FriendlyByteBuf input,
        final List<Integer> dataSections,
        final List<Integer> emptySections,
        final List<byte[]> updates
    ) {
        int dataCount = input.readVarInt();
        for (int i = 0; i < dataCount; i++) {
            dataSections.add(input.readVarInt());
            updates.add(DATA_LAYER_STREAM_CODEC.decode(input));
        }
        int emptyCount = input.readVarInt();
        for (int i = 0; i < emptyCount; i++) {
            emptySections.add(input.readVarInt());
        }
    }

    public void write(final FriendlyByteBuf output) {
        writeLayer(output, this.skySectionYs, this.emptySkySectionYs, this.skyUpdates);
        writeLayer(output, this.blockSectionYs, this.emptyBlockSectionYs, this.blockUpdates);
    }

    private static void writeLayer(
        final FriendlyByteBuf output,
        final List<Integer> dataSections,
        final List<Integer> emptySections,
        final List<byte[]> updates
    ) {
        output.writeVarInt(dataSections.size());
        for (int i = 0; i < dataSections.size(); i++) {
            output.writeVarInt(dataSections.get(i));
            DATA_LAYER_STREAM_CODEC.encode(output, updates.get(i));
        }
        output.writeVarInt(emptySections.size());
        for (int sectionY : emptySections) {
            output.writeVarInt(sectionY);
        }
    }

    private void prepareSectionData(
        final ChunkPos pos,
        final LevelLightEngine lightEngine,
        final LightLayer layer,
        final int sectionY,
        final List<Integer> dataSections,
        final List<Integer> emptySections,
        final List<byte[]> updates
    ) {
        DataLayer data = lightEngine.getLayerListener(layer).getDataLayerData(SectionPos.of(pos, sectionY));
        if (data != null) {
            if (data.isEmpty()) {
                emptySections.add(sectionY);
            } else {
                dataSections.add(sectionY);
                updates.add(data.copy().getData());
            }
        }
    }

    public List<Integer> getSkySectionYs() {
        return this.skySectionYs;
    }

    public List<Integer> getBlockSectionYs() {
        return this.blockSectionYs;
    }

    public List<Integer> getEmptySkySectionYs() {
        return this.emptySkySectionYs;
    }

    public List<Integer> getEmptyBlockSectionYs() {
        return this.emptyBlockSectionYs;
    }

    public List<byte[]> getSkyUpdates() {
        return this.skyUpdates;
    }

    public List<byte[]> getBlockUpdates() {
        return this.blockUpdates;
    }
}