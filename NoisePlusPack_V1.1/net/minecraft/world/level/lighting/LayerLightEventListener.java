package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;

public interface LayerLightEventListener extends LightEventListener {
   @Nullable DataLayer getDataLayerData(SectionPos var1);

   int getLightValue(BlockPos var1);

   enum DummyLightLayerEventListener implements LayerLightEventListener {
      INSTANCE;

      @Override
      public @Nullable DataLayer getDataLayerData(SectionPos p_75718_) {
         return null;
      }

      @Override
      public int getLightValue(BlockPos p_75723_) {
         return 0;
      }

      @Override
      public void checkBlock(BlockPos p_164434_) {
      }

      @Override
      public boolean hasLightWork() {
         return false;
      }

      @Override
      public int runLightUpdates() {
         return 0;
      }

      @Override
      public void updateSectionStatus(SectionPos p_75720_, boolean p_75721_) {
      }

      @Override
      public void setLightEnabled(ChunkPos p_164431_, boolean p_164432_) {
      }

      @Override
      public void propagateLightSources(ChunkPos p_285209_) {
      }
   }
}
