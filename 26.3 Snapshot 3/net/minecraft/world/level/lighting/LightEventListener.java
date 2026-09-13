package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public interface LightEventListener {
   void checkBlock(BlockPos pos);

   boolean hasLightWork();

   int runLightUpdates();

   default void updateSectionStatus(final BlockPos pos, final boolean sectionEmpty) {
      this.updateSectionStatus(SectionPos.of(pos), sectionEmpty);
   }

   void updateSectionStatus(final SectionPos pos, boolean sectionEmpty);

   void setLightEnabled(ChunkPos pos, boolean enable);

   void propagateLightSources(ChunkPos pos);
}
