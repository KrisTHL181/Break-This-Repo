package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
   protected SkyLightSectionStorage(LightChunkGetter p_75868_) {
      super(
         LightLayer.SKY,
         p_75868_,
         new SkyLightSectionStorage.SkyDataLayerStorageMap(new Long2ObjectOpenHashMap(), new Long2IntOpenHashMap(), Integer.MAX_VALUE)
      );
   }

   @Override
   protected int getLightValue(long p_75880_) {
      return this.getLightValue(p_75880_, false);
   }

   protected int getLightValue(long p_164458_, boolean p_164459_) {
      long i = SectionPos.blockToSection(p_164458_);
      int j = SectionPos.y(i);
      SkyLightSectionStorage.SkyDataLayerStorageMap skylightsectionstorage$skydatalayerstoragemap = p_164459_
         ? this.updatingSectionData
         : this.visibleSectionData;
      int k = skylightsectionstorage$skydatalayerstoragemap.topSections.get(SectionPos.getZeroNode(i));
      if (k != skylightsectionstorage$skydatalayerstoragemap.currentLowestY && j < k) {
         DataLayer datalayer = this.getDataLayer(skylightsectionstorage$skydatalayerstoragemap, i);
         if (datalayer == null) {
            p_164458_ = BlockPos.getFlatIndex(p_164458_);

            while (datalayer == null) {
               if (++j >= k) {
                  return 15;
               }

               i = SectionPos.offset(i, Direction.UP);
               datalayer = this.getDataLayer(skylightsectionstorage$skydatalayerstoragemap, i);
            }
         }

         return datalayer.get(
            SectionPos.sectionRelative(BlockPos.getX(p_164458_)),
            SectionPos.sectionRelative(BlockPos.getY(p_164458_)),
            SectionPos.sectionRelative(BlockPos.getZ(p_164458_))
         );
      } else {
         return p_164459_ && !this.lightOnInSection(i) ? 0 : 15;
      }
   }

   @Override
   protected void onNodeAdded(long p_75885_) {
      int i = SectionPos.y(p_75885_);
      if (this.updatingSectionData.currentLowestY > i) {
         this.updatingSectionData.currentLowestY = i;
         this.updatingSectionData.topSections.defaultReturnValue(this.updatingSectionData.currentLowestY);
      }

      long j = SectionPos.getZeroNode(p_75885_);
      int k = this.updatingSectionData.topSections.get(j);
      if (k < i + 1) {
         this.updatingSectionData.topSections.put(j, i + 1);
      }
   }

   @Override
   protected void onNodeRemoved(long p_75887_) {
      long i = SectionPos.getZeroNode(p_75887_);
      int j = SectionPos.y(p_75887_);
      if (this.updatingSectionData.topSections.get(i) == j + 1) {
         long k;
         for (k = p_75887_; !this.storingLightForSection(k) && this.hasLightDataAtOrBelow(j); k = SectionPos.offset(k, Direction.DOWN)) {
            j--;
         }

         if (this.storingLightForSection(k)) {
            this.updatingSectionData.topSections.put(i, j + 1);
         } else {
            this.updatingSectionData.topSections.remove(i);
         }
      }
   }

   @Override
   protected DataLayer createDataLayer(long p_75883_) {
      DataLayer datalayer = (DataLayer)this.queuedSections.get(p_75883_);
      if (datalayer != null) {
         return datalayer;
      }

      int i = this.updatingSectionData.topSections.get(SectionPos.getZeroNode(p_75883_));
      if (i != this.updatingSectionData.currentLowestY && SectionPos.y(p_75883_) < i) {
         long j = SectionPos.offset(p_75883_, Direction.UP);

         DataLayer datalayer1;
         while ((datalayer1 = this.getDataLayer(j, true)) == null) {
            j = SectionPos.offset(j, Direction.UP);
         }

         return repeatFirstLayer(datalayer1);
      } else {
         return this.lightOnInSection(p_75883_) ? new DataLayer(15) : new DataLayer();
      }
   }

   private static DataLayer repeatFirstLayer(DataLayer p_182513_) {
      if (p_182513_.isDefinitelyHomogenous()) {
         return p_182513_.copy();
      }

      byte[] abyte = p_182513_.getData();
      byte[] abyte1 = new byte[2048];

      for (int i = 0; i < 16; i++) {
         System.arraycopy(abyte, 0, abyte1, i * 128, 128);
      }

      return new DataLayer(abyte1);
   }

   protected boolean hasLightDataAtOrBelow(int p_278270_) {
      return p_278270_ >= this.updatingSectionData.currentLowestY;
   }

   protected boolean isAboveData(long p_75891_) {
      long i = SectionPos.getZeroNode(p_75891_);
      int j = this.updatingSectionData.topSections.get(i);
      return j == this.updatingSectionData.currentLowestY || SectionPos.y(p_75891_) >= j;
   }

   protected int getTopSectionY(long p_285094_) {
      return this.updatingSectionData.topSections.get(p_285094_);
   }

   protected int getBottomSectionY() {
      return this.updatingSectionData.currentLowestY;
   }

   protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
      int currentLowestY;
      final Long2IntOpenHashMap topSections;

      public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> p_75903_, Long2IntOpenHashMap p_75904_, int p_75905_) {
         super(p_75903_);
         this.topSections = p_75904_;
         p_75904_.defaultReturnValue(p_75905_);
         this.currentLowestY = p_75905_;
      }

      public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
         return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.map.clone(), this.topSections.clone(), this.currentLowestY);
      }
   }
}
