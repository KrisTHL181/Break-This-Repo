package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BulkSectionAccess implements AutoCloseable {
   private final LevelAccessor level;
   private final Long2ObjectMap<LevelChunkSection> acquiredSections = new Long2ObjectOpenHashMap();
   private @Nullable LevelChunkSection lastSection;
   private long lastSectionKey;

   public BulkSectionAccess(final LevelAccessor level) {
      this.level = level;
   }

   public @Nullable LevelChunkSection getSection(final BlockPos pos) {
      int sectionIndex = this.level.getSectionIndex(pos.getY());
      if (sectionIndex >= 0 && sectionIndex < this.level.getSectionsCount()) {
         long sectionKey = SectionPos.asLong(pos);
         if (this.lastSection == null || this.lastSectionKey != sectionKey) {
            this.lastSection = (LevelChunkSection)this.acquiredSections.computeIfAbsent(sectionKey, key -> {
               ChunkAccess chunk = this.level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
               LevelChunkSection result = chunk.getSection(sectionIndex);
               result.acquire();
               return result;
            });
            this.lastSectionKey = sectionKey;
         }

         return this.lastSection;
      } else {
         return null;
      }
   }

   public BlockState getBlockState(final BlockPos pos) {
      LevelChunkSection section = this.getSection(pos);
      if (section == null) {
         return Blocks.AIR.defaultBlockState();
      }

      int sectionRelativeX = SectionPos.sectionRelative(pos.getX());
      int sectionRelativeY = SectionPos.sectionRelative(pos.getY());
      int sectionRelativeZ = SectionPos.sectionRelative(pos.getZ());
      return section.getBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ);
   }

   @Override
   public void close() {
      ObjectIterator var1 = this.acquiredSections.values().iterator();

      while (var1.hasNext()) {
         LevelChunkSection section = (LevelChunkSection)var1.next();
         section.release();
      }
   }
}
