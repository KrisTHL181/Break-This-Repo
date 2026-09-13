package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;

public interface LayerLightEventListener extends LightEventListener {
   @Nullable DataLayer getDataLayerData(final SectionPos pos);

   int getLightValue(final BlockPos pos);

   enum DummyLightLayerEventListener implements LayerLightEventListener {
      INSTANCE;

      @Override
      public @Nullable DataLayer getDataLayerData(final SectionPos pos) {
         return null;
      }

      @Override
      public int getLightValue(final BlockPos pos) {
         return 0;
      }

      @Override
      public void checkBlock(final BlockPos pos) {
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
      public void updateSectionStatus(final SectionPos pos, final boolean sectionEmpty) {
      }

      @Override
      public void setLightEnabled(final ChunkPos pos, final boolean enable) {
      }

      @Override
      public void propagateLightSources(final ChunkPos pos) {
      }
   }
}
