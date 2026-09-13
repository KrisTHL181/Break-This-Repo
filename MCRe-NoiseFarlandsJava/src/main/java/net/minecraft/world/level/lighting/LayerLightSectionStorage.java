package net.minecraft.world.level.lighting;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jspecify.annotations.Nullable;

/**
 * LayerLightSectionStorage — 区块节光照存储（MCRe NoiseFarlands 对象化版）
 * 原版以 long sectionNode 为键（SectionPos.asLong 打包），本版直接用 SectionPos 对象为键。
 */
public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> {
    private final LightLayer layer;
    protected final LightChunkGetter chunkSource;
    protected final Map<SectionPos, Byte> sectionStates = new HashMap<>();
    private final Set<SectionPos> columnsWithSources = new HashSet<>();
    protected volatile M visibleSectionData;
    protected final M updatingSectionData;
    protected final Set<SectionPos> changedSections = new HashSet<>();
    protected final Set<SectionPos> sectionsAffectedByLightUpdates = new HashSet<>();
    protected final Map<SectionPos, DataLayer> queuedSections = Collections.synchronizedMap(new HashMap<>());
    private final Set<SectionPos> columnsToRetainQueuedDataFor = new HashSet<>();
    private final Set<SectionPos> toRemove = new HashSet<>();
    protected volatile boolean hasInconsistencies;

    protected LayerLightSectionStorage(final LightLayer layer, final LightChunkGetter chunkSource, final M initialMap) {
        this.layer = layer;
        this.chunkSource = chunkSource;
        this.updatingSectionData = initialMap;
        this.visibleSectionData = initialMap.copy();
        this.visibleSectionData.disableCache();
    }

    protected boolean storingLightForSection(final SectionPos sectionNode) {
        return this.getDataLayer(sectionNode, true) != null;
    }

    protected @Nullable DataLayer getDataLayer(final SectionPos sectionNode, final boolean updating) {
        return this.getDataLayer(updating ? this.updatingSectionData : this.visibleSectionData, sectionNode);
    }

    protected @Nullable DataLayer getDataLayer(final M sections, final SectionPos sectionNode) {
        return sections.getLayer(sectionNode);
    }

    protected @Nullable DataLayer getDataLayerToWrite(final SectionPos sectionNode) {
        DataLayer dataLayer = this.updatingSectionData.getLayer(sectionNode);
        if (dataLayer == null) {
            return null;
        }

        if (this.changedSections.add(sectionNode)) {
            dataLayer = dataLayer.copy();
            this.updatingSectionData.setLayer(sectionNode, dataLayer);
            this.updatingSectionData.clearCache();
        }

        return dataLayer;
    }

    public @Nullable DataLayer getDataLayerData(final SectionPos sectionNode) {
        DataLayer layer = this.queuedSections.get(sectionNode);
        return layer != null ? layer : this.getDataLayer(sectionNode, false);
    }

    protected abstract int getLightValue(final BlockPos blockNode);

    protected int getStoredLevel(final BlockPos blockNode) {
        SectionPos sectionNode = SectionPos.of(blockNode);
        DataLayer layer = this.getDataLayer(sectionNode, true);
        if (layer == null) {
            // far lands：光照队列残留节点对应的 section 数据已被移除（区块卸载/状态变化），按无光照处理
            return 0;
        }
        return layer.get(
            SectionPos.sectionRelative(blockNode.getX()),
            SectionPos.sectionRelative(blockNode.getY()),
            SectionPos.sectionRelative(blockNode.getZ())
        );
    }

    protected void setStoredLevel(final BlockPos blockNode, final int level) {
        SectionPos sectionNode = SectionPos.of(blockNode);
        DataLayer layer;
        if (this.changedSections.add(sectionNode)) {
            layer = this.updatingSectionData.copyDataLayer(sectionNode);
        } else {
            layer = this.getDataLayer(sectionNode, true);
        }

        if (layer == null) {
            // far lands：section 无光照数据层（已移除/尚未创建），跳过写入
            return;
        }

        layer.set(
            SectionPos.sectionRelative(blockNode.getX()),
            SectionPos.sectionRelative(blockNode.getY()),
            SectionPos.sectionRelative(blockNode.getZ()),
            level
        );
        SectionPos.aroundAndAtBlockPos(blockNode, this.sectionsAffectedByLightUpdates::add);
    }

    protected void markSectionAndNeighborsAsAffected(final SectionPos sectionNode) {
        for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    this.sectionsAffectedByLightUpdates.add(sectionNode.offset(offsetX, offsetY, offsetZ));
                }
            }
        }
    }

    protected DataLayer createDataLayer(final SectionPos sectionNode) {
        DataLayer queuedLayer = this.queuedSections.get(sectionNode);
        return queuedLayer != null ? queuedLayer : new DataLayer();
    }

    protected boolean hasInconsistencies() {
        return this.hasInconsistencies;
    }

    protected void markNewInconsistencies(final LightEngine<M, ?> engine) {
        if (this.hasInconsistencies) {
            this.hasInconsistencies = false;

            for (SectionPos node : this.toRemove) {
                DataLayer queued = this.queuedSections.remove(node);
                DataLayer stored = this.updatingSectionData.removeLayer(node);
                if (this.columnsToRetainQueuedDataFor.contains(SectionPos.of(node.x(), 0, node.z()))) {
                    if (queued != null) {
                        this.queuedSections.put(node, queued);
                    } else if (stored != null) {
                        this.queuedSections.put(node, stored);
                    }
                }
            }

            this.updatingSectionData.clearCache();

            for (SectionPos node : this.toRemove) {
                this.onNodeRemoved(node);
                this.changedSections.add(node);
            }

            this.toRemove.clear();
            var iterator = this.queuedSections.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<SectionPos, DataLayer> entry = iterator.next();
                SectionPos sectionNode = entry.getKey();
                if (this.storingLightForSection(sectionNode)) {
                    DataLayer data = entry.getValue();
                    if (this.updatingSectionData.getLayer(sectionNode) != data) {
                        this.updatingSectionData.setLayer(sectionNode, data);
                        this.changedSections.add(sectionNode);
                    }

                    iterator.remove();
                }
            }

            this.updatingSectionData.clearCache();
        }
    }

    protected void onNodeAdded(final SectionPos sectionNode) {
    }

    protected void onNodeRemoved(final SectionPos sectionNode) {
    }

    protected void setLightEnabled(final SectionPos zeroNode, final boolean enable) {
        if (enable) {
            this.columnsWithSources.add(zeroNode);
        } else {
            this.columnsWithSources.remove(zeroNode);
        }
    }

    protected boolean lightOnInSection(final SectionPos sectionNode) {
        SectionPos zeroNode = SectionPos.of(sectionNode.x(), 0, sectionNode.z());
        return this.columnsWithSources.contains(zeroNode);
    }

    protected boolean lightOnInColumn(final SectionPos sectionZeroNode) {
        return this.columnsWithSources.contains(sectionZeroNode);
    }

    public void retainData(final SectionPos zeroNode, final boolean retain) {
        if (retain) {
            this.columnsToRetainQueuedDataFor.add(zeroNode);
        } else {
            this.columnsToRetainQueuedDataFor.remove(zeroNode);
        }
    }

    protected void queueSectionData(final SectionPos sectionNode, final @Nullable DataLayer data) {
        if (data != null) {
            this.queuedSections.put(sectionNode, data);
            this.hasInconsistencies = true;
        } else {
            this.queuedSections.remove(sectionNode);
        }
    }

    protected void updateSectionStatus(final SectionPos sectionNode, final boolean sectionEmpty) {
        byte state = this.sectionStates.getOrDefault(sectionNode, (byte)0);
        byte newState = LayerLightSectionStorage.SectionState.hasData(state, !sectionEmpty);
        if (state != newState) {
            this.putSectionState(sectionNode, newState);
            int neighborIncrement = sectionEmpty ? -1 : 1;

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX != 0 || offsetY != 0 || offsetZ != 0) {
                            SectionPos neighborNode = sectionNode.offset(offsetX, offsetY, offsetZ);
                            byte neighborState = this.sectionStates.getOrDefault(neighborNode, (byte)0);
                            this.putSectionState(
                                neighborNode,
                                LayerLightSectionStorage.SectionState.neighborCount(
                                    neighborState, LayerLightSectionStorage.SectionState.neighborCount(neighborState) + neighborIncrement
                                )
                            );
                        }
                    }
                }
            }
        }
    }

    protected void putSectionState(final SectionPos sectionNode, final byte state) {
        if (state != 0) {
            if (this.sectionStates.put(sectionNode, state) == null) {
                this.initializeSection(sectionNode);
            }
        } else if (this.sectionStates.remove(sectionNode) != null) {
            this.removeSection(sectionNode);
        }
    }

    private void initializeSection(final SectionPos sectionNode) {
        if (!this.toRemove.remove(sectionNode)) {
            this.updatingSectionData.setLayer(sectionNode, this.createDataLayer(sectionNode));
            this.changedSections.add(sectionNode);
            this.onNodeAdded(sectionNode);
            this.markSectionAndNeighborsAsAffected(sectionNode);
            this.hasInconsistencies = true;
        }
    }

    private void removeSection(final SectionPos sectionNode) {
        this.toRemove.add(sectionNode);
        this.hasInconsistencies = true;
    }

    protected void swapSectionMap() {
        if (!this.changedSections.isEmpty()) {
            M copy = this.updatingSectionData.copy();
            copy.disableCache();
            this.visibleSectionData = copy;
            this.changedSections.clear();
        }

        if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
            for (SectionPos sectionNode : this.sectionsAffectedByLightUpdates) {
                this.chunkSource.onLightUpdate(this.layer, sectionNode);
            }

            this.sectionsAffectedByLightUpdates.clear();
        }
    }

    public LayerLightSectionStorage.SectionType getDebugSectionType(final SectionPos sectionNode) {
        return LayerLightSectionStorage.SectionState.type(this.sectionStates.getOrDefault(sectionNode, (byte)0));
    }

    protected static class SectionState {
        public static final byte EMPTY = 0;
        private static final int MIN_NEIGHBORS = 0;
        private static final int MAX_NEIGHBORS = 26;
        private static final byte HAS_DATA_BIT = 32;
        private static final byte NEIGHBOR_COUNT_BITS = 31;

        public static byte hasData(final byte state, final boolean hasData) {
            return (byte)(hasData ? state | 32 : state & -33);
        }

        public static byte neighborCount(final byte state, final int neighborCount) {
            if (neighborCount >= 0 && neighborCount <= 26) {
                return (byte)(state & -32 | neighborCount & 31);
            } else {
                throw new IllegalArgumentException("Neighbor count was not within range [0; 26]");
            }
        }

        public static boolean hasData(final byte state) {
            return (state & 32) != 0;
        }

        public static int neighborCount(final byte state) {
            return state & 31;
        }

        public static LayerLightSectionStorage.SectionType type(final byte state) {
            if (state == 0) {
                return LayerLightSectionStorage.SectionType.EMPTY;
            } else {
                return hasData(state) ? LayerLightSectionStorage.SectionType.LIGHT_AND_DATA : LayerLightSectionStorage.SectionType.LIGHT_ONLY;
            }
        }
    }

    public enum SectionType {
        EMPTY("2"),
        LIGHT_ONLY("1"),
        LIGHT_AND_DATA("0");

        private final String display;

        SectionType(final String display) {
            this.display = display;
        }

        public String display() {
            return this.display;
        }
    }
}
