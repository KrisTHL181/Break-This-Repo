package net.minecraft.world.level.lighting;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

/**
 * BlockLightSectionStorage — 方块光照存储（MCRe NoiseFarlands 对象化版）
 */
public class BlockLightSectionStorage extends LayerLightSectionStorage<BlockLightSectionStorage.BlockDataLayerStorageMap> {
    protected BlockLightSectionStorage(final LightChunkGetter chunkSource) {
        super(LightLayer.BLOCK, chunkSource, new BlockLightSectionStorage.BlockDataLayerStorageMap(new HashMap<>()));
    }

    @Override
    protected int getLightValue(final BlockPos blockNode) {
        SectionPos sectionNode = SectionPos.of(blockNode);
        DataLayer layer = this.getDataLayer(sectionNode, false);
        return layer == null
            ? 0
            : layer.get(
                SectionPos.sectionRelative(blockNode.getX()),
                SectionPos.sectionRelative(blockNode.getY()),
                SectionPos.sectionRelative(blockNode.getZ())
            );
    }

    protected static final class BlockDataLayerStorageMap extends DataLayerStorageMap<BlockLightSectionStorage.BlockDataLayerStorageMap> {
        public BlockDataLayerStorageMap(final HashMap<SectionPos, DataLayer> map) {
            super(map);
        }

        public BlockLightSectionStorage.BlockDataLayerStorageMap copy() {
            return new BlockLightSectionStorage.BlockDataLayerStorageMap(this.copyMap());
        }
    }
}
