package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
   ChunkTrackingView EMPTY = new ChunkTrackingView() {
      @Override
      public boolean contains(int p_298894_, int p_300849_, boolean p_299623_) {
         return false;
      }

      @Override
      public void forEach(Consumer<ChunkPos> p_298868_) {
      }
   };

   static ChunkTrackingView of(ChunkPos p_299839_, int p_298969_) {
      return new ChunkTrackingView.Positioned(p_299839_, p_298969_);
   }

   static void difference(ChunkTrackingView p_297320_, ChunkTrackingView p_298920_, Consumer<ChunkPos> p_300281_, Consumer<ChunkPos> p_298429_) {
      if (!p_297320_.equals(p_298920_)) {
         if (p_297320_ instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
            && p_298920_ instanceof ChunkTrackingView.Positioned chunktrackingview$positioned1
            && chunktrackingview$positioned.squareIntersects(chunktrackingview$positioned1)) {
            int i = Math.min(chunktrackingview$positioned.minX(), chunktrackingview$positioned1.minX());
            int j = Math.min(chunktrackingview$positioned.minZ(), chunktrackingview$positioned1.minZ());
            int k = Math.max(chunktrackingview$positioned.maxX(), chunktrackingview$positioned1.maxX());
            int l = Math.max(chunktrackingview$positioned.maxZ(), chunktrackingview$positioned1.maxZ());

            for (int i1 = i; i1 <= k; i1++) {
               for (int j1 = j; j1 <= l; j1++) {
                  boolean flag = chunktrackingview$positioned.contains(i1, j1);
                  boolean flag1 = chunktrackingview$positioned1.contains(i1, j1);
                  if (flag != flag1) {
                     if (flag1) {
                        p_300281_.accept(new ChunkPos(i1, j1));
                     } else {
                        p_298429_.accept(new ChunkPos(i1, j1));
                     }
                  }
               }
            }
         } else {
            p_297320_.forEach(p_298429_);
            p_298920_.forEach(p_300281_);
         }
      }
   }

   default boolean contains(ChunkPos p_298506_) {
      return this.contains(p_298506_.x, p_298506_.z);
   }

   default boolean contains(int p_298205_, int p_299033_) {
      return this.contains(p_298205_, p_299033_, true);
   }

   boolean contains(int var1, int var2, boolean var3);

   void forEach(Consumer<ChunkPos> var1);

   default boolean isInViewDistance(int p_299368_, int p_297466_) {
      return this.contains(p_299368_, p_297466_, false);
   }

   static boolean isInViewDistance(int p_300363_, int p_300565_, int p_297699_, int p_299801_, int p_300142_) {
      return isWithinDistance(p_300363_, p_300565_, p_297699_, p_299801_, p_300142_, false);
   }

   static boolean isWithinDistance(int p_299483_, int p_297415_, int p_300799_, int p_299157_, int p_301327_, boolean p_301271_) {
      int i = p_301271_ ? 2 : 1;
      long j = Math.max(0, Math.abs(p_299157_ - p_299483_) - i);
      long k = Math.max(0, Math.abs(p_301327_ - p_297415_) - i);
      long l = j * j + k * k;
      int i1 = p_300799_ * p_300799_;
      return l < i1;
   }

   record Positioned(ChunkPos center, int viewDistance) implements ChunkTrackingView {
      int minX() {
         return this.center.x - this.viewDistance - 1;
      }

      int minZ() {
         return this.center.z - this.viewDistance - 1;
      }

      int maxX() {
         return this.center.x + this.viewDistance + 1;
      }

      int maxZ() {
         return this.center.z + this.viewDistance + 1;
      }

      @VisibleForTesting
      protected boolean squareIntersects(ChunkTrackingView.Positioned p_300776_) {
         return this.minX() <= p_300776_.maxX() && this.maxX() >= p_300776_.minX() && this.minZ() <= p_300776_.maxZ() && this.maxZ() >= p_300776_.minZ();
      }

      @Override
      public boolean contains(int p_297345_, int p_300837_, boolean p_298477_) {
         return ChunkTrackingView.isWithinDistance(this.center.x, this.center.z, this.viewDistance, p_297345_, p_300837_, p_298477_);
      }

      @Override
      public void forEach(Consumer<ChunkPos> p_299048_) {
         for (int i = this.minX(); i <= this.maxX(); i++) {
            for (int j = this.minZ(); j <= this.maxZ(); j++) {
               if (this.contains(i, j)) {
                  p_299048_.accept(new ChunkPos(i, j));
               }
            }
         }
      }
   }
}
