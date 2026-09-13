package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;

public abstract class DataLayerStorageMap<M extends DataLayerStorageMap<M>> {
   private static final int CACHE_SIZE = 2;
   private final long[] lastSectionKeys = new long[2];
   private final @Nullable DataLayer[] lastSections = new DataLayer[2];
   private boolean cacheEnabled;
   protected final Long2ObjectOpenHashMap<DataLayer> map;

   protected DataLayerStorageMap(final Long2ObjectOpenHashMap<DataLayer> map) {
      this.map = map;
      this.clearCache();
      this.cacheEnabled = true;
   }

   public abstract M copy();

   public DataLayer copyDataLayer(final long sectionNode) {
      DataLayer newDataLayer = ((DataLayer)this.map.get(sectionNode)).copy();
      this.map.put(sectionNode, newDataLayer);
      this.clearCache();
      return newDataLayer;
   }

   public boolean hasLayer(final long sectionNode) {
      return this.map.containsKey(sectionNode);
   }

   public @Nullable DataLayer getLayer(final long sectionNode) {
      if (this.cacheEnabled) {
         for (int i = 0; i < 2; i++) {
            if (sectionNode == this.lastSectionKeys[i]) {
               return this.lastSections[i];
            }
         }
      }

      DataLayer data = (DataLayer)this.map.get(sectionNode);
      if (data == null) {
         return null;
      }

      if (this.cacheEnabled) {
         for (int i = 1; i > 0; i--) {
            this.lastSectionKeys[i] = this.lastSectionKeys[i - 1];
            this.lastSections[i] = this.lastSections[i - 1];
         }

         this.lastSectionKeys[0] = sectionNode;
         this.lastSections[0] = data;
      }

      return data;
   }

   public @Nullable DataLayer removeLayer(final long sectionNode) {
      return (DataLayer)this.map.remove(sectionNode);
   }

   public void setLayer(final long sectionNode, final DataLayer layer) {
      this.map.put(sectionNode, layer);
   }

   public void clearCache() {
      for (int i = 0; i < 2; i++) {
         this.lastSectionKeys[i] = Long.MAX_VALUE;
         this.lastSections[i] = null;
      }
   }

   public void disableCache() {
      this.cacheEnabled = false;
   }
}
