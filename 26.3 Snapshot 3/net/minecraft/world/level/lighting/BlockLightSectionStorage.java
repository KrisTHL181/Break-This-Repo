package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class BlockLightSectionStorage extends LayerLightSectionStorage<BlockLightSectionStorage.BlockDataLayerStorageMap> {
   protected BlockLightSectionStorage(final LightChunkGetter chunkSource) {
      super(LightLayer.BLOCK, chunkSource, new BlockLightSectionStorage.BlockDataLayerStorageMap(new Long2ObjectOpenHashMap()));
   }

   @Override
   protected int getLightValue(final long blockNode) {
      long sectionNode = SectionPos.blockToSection(blockNode);
      DataLayer layer = this.getDataLayer(sectionNode, false);
      return layer == null
         ? 0
         : layer.get(
            SectionPos.sectionRelative(BlockPos.getX(blockNode)),
            SectionPos.sectionRelative(BlockPos.getY(blockNode)),
            SectionPos.sectionRelative(BlockPos.getZ(blockNode))
         );
   }

   protected static final class BlockDataLayerStorageMap extends DataLayerStorageMap<BlockLightSectionStorage.BlockDataLayerStorageMap> {
      public BlockDataLayerStorageMap(final Long2ObjectOpenHashMap<DataLayer> map) {
         super(map);
      }

      public BlockLightSectionStorage.BlockDataLayerStorageMap copy() {
         return new BlockLightSectionStorage.BlockDataLayerStorageMap(this.map.clone());
      }
   }
}
