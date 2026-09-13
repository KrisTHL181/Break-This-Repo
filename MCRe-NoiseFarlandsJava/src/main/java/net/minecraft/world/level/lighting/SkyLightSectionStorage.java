package net.minecraft.world.level.lighting;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

/**
 * SkyLightSectionStorage — 天空光照存储（MCRe NoiseFarlands 对象化版）
 * long sectionNode → SectionPos 对象；topSections 键 = 列零节点（SectionPos）。
 */
public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
    protected SkyLightSectionStorage(final LightChunkGetter chunkSource) {
        super(
            LightLayer.SKY,
            chunkSource,
            new SkyLightSectionStorage.SkyDataLayerStorageMap(new HashMap<>(), new HashMap<>(), Integer.MAX_VALUE)
        );
    }

    @Override
    protected int getLightValue(final BlockPos blockNode) {
        return this.getLightValue(blockNode, false);
    }

    protected int getLightValue(final BlockPos blockNode, final boolean updating) {
        SectionPos sectionNode = SectionPos.of(blockNode);
        int sectionY = sectionNode.y();
        SkyLightSectionStorage.SkyDataLayerStorageMap sections = updating ? this.updatingSectionData : this.visibleSectionData;
        int topSection = sections.topSections.getOrDefault(SectionPos.of(sectionNode.x(), 0, sectionNode.z()), sections.currentLowestY);
        if (topSection != sections.currentLowestY && sectionY < topSection) {
            DataLayer layer = this.getDataLayer(sections, sectionNode);
            if (layer == null) {
                BlockPos flatNode = blockNode.atY(blockNode.getY() & ~15);

                while (layer == null) {
                    if (++sectionY >= topSection) {
                        return 15;
                    }

                    sectionNode = sectionNode.offset(0, 1, 0);
                    layer = this.getDataLayer(sections, sectionNode);
                }

                return layer.get(
                    SectionPos.sectionRelative(flatNode.getX()),
                    SectionPos.sectionRelative(flatNode.getY()),
                    SectionPos.sectionRelative(flatNode.getZ())
                );
            }

            return layer.get(
                SectionPos.sectionRelative(blockNode.getX()),
                SectionPos.sectionRelative(blockNode.getY()),
                SectionPos.sectionRelative(blockNode.getZ())
            );
        } else {
            return updating && !this.lightOnInSection(sectionNode) ? 0 : 15;
        }
    }

    @Override
    protected void onNodeAdded(final SectionPos sectionNode) {
        int y = sectionNode.y();
        if (this.updatingSectionData.currentLowestY > y) {
            this.updatingSectionData.currentLowestY = y;
        }

        SectionPos zeroNode = SectionPos.of(sectionNode.x(), 0, sectionNode.z());
        int oldTop = this.updatingSectionData.topSections.getOrDefault(zeroNode, this.updatingSectionData.currentLowestY);
        if (oldTop < y + 1) {
            this.updatingSectionData.topSections.put(zeroNode, y + 1);
        }
    }

    @Override
    protected void onNodeRemoved(final SectionPos sectionNode) {
        SectionPos zeroNode = SectionPos.of(sectionNode.x(), 0, sectionNode.z());
        int y = sectionNode.y();
        if (this.updatingSectionData.topSections.getOrDefault(zeroNode, this.updatingSectionData.currentLowestY) == y + 1) {
            SectionPos newTopSection;
            for (newTopSection = sectionNode;
                !this.storingLightForSection(newTopSection) && this.hasLightDataAtOrBelow(y);
                newTopSection = newTopSection.offset(0, -1, 0)
            ) {
                y--;
            }

            if (this.storingLightForSection(newTopSection)) {
                this.updatingSectionData.topSections.put(zeroNode, y + 1);
            } else {
                this.updatingSectionData.topSections.remove(zeroNode);
            }
        }
    }

    @Override
    protected DataLayer createDataLayer(final SectionPos sectionNode) {
        DataLayer queuedLayer = this.queuedSections.get(sectionNode);
        if (queuedLayer != null) {
            return queuedLayer;
        }

        int topSection = this.updatingSectionData.topSections.getOrDefault(SectionPos.of(sectionNode.x(), 0, sectionNode.z()), this.updatingSectionData.currentLowestY);
        if (topSection != this.updatingSectionData.currentLowestY && sectionNode.y() < topSection) {
            SectionPos aboveSection = sectionNode.offset(0, 1, 0);

            // far lands 防御：顶部数据节状态错乱时向上查找可能无界，限制查找深度（世界高度上限）
            DataLayer aboveData = null;
            for (int guard = 0; guard < 1024 && (aboveData = this.getDataLayer(aboveSection, true)) == null; guard++) {
                aboveSection = aboveSection.offset(0, 1, 0);
            }

            if (aboveData == null) {
                return new DataLayer(15);
            }

            return repeatFirstLayer(aboveData);
        } else {
            return this.lightOnInSection(sectionNode) ? new DataLayer(15) : new DataLayer();
        }
    }

    private static DataLayer repeatFirstLayer(final DataLayer data) {
        if (data.isDefinitelyHomogenous()) {
            return data.copy();
        }

        byte[] input = data.getData();
        byte[] output = new byte[2048];

        for (int i = 0; i < 16; i++) {
            System.arraycopy(input, 0, output, i * 128, 128);
        }

        return new DataLayer(output);
    }

    protected boolean hasLightDataAtOrBelow(final int sectionY) {
        return sectionY >= this.updatingSectionData.currentLowestY;
    }

    protected boolean isAboveData(final SectionPos sectionNode) {
        SectionPos zeroNode = SectionPos.of(sectionNode.x(), 0, sectionNode.z());
        int topSection = this.updatingSectionData.topSections.getOrDefault(zeroNode, this.updatingSectionData.currentLowestY);
        return topSection == this.updatingSectionData.currentLowestY || sectionNode.y() >= topSection;
    }

    protected int getTopSectionY(final SectionPos zeroNode) {
        return this.updatingSectionData.topSections.getOrDefault(zeroNode, this.updatingSectionData.currentLowestY);
    }

    protected int getBottomSectionY() {
        return this.updatingSectionData.currentLowestY;
    }

    protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
        private int currentLowestY;
        private final HashMap<SectionPos, Integer> topSections;

        public SkyDataLayerStorageMap(
            final HashMap<SectionPos, DataLayer> map, final HashMap<SectionPos, Integer> topSections, final int currentLowestY
        ) {
            super(map);
            this.topSections = topSections;
            this.currentLowestY = currentLowestY;
        }

        public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
            return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.copyMap(), new HashMap<>(this.topSections), this.currentLowestY);
        }
    }
}
