package net.minecraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class SectionUpdateTracker {
   private final RotatingSectionStorage<SectionUpdateTracker.SectionDirtyState> storage;

   public SectionUpdateTracker(final LevelHeightAccessor levelHeightAccessor, final int renderDistance) {
      this.storage = new RotatingSectionStorage<>(
         renderDistance,
         levelHeightAccessor.getMinSectionY(),
         levelHeightAccessor.getMaxSectionY(),
         (index, sectionNode) -> new SectionUpdateTracker.SectionDirtyState(true, false, sectionNode)
      );
   }

   public void setDirty(final int sectionX, final int sectionY, final int sectionZ, final boolean playerChanged) {
      SectionUpdateTracker.SectionDirtyState section = this.storage.getValue(sectionX, sectionY, sectionZ);
      if (section != null) {
         section.setDirty(playerChanged);
      }
   }

   public void repositionCamera(final SectionPos cameraSectionPos) {
      this.storage.repositionCenter(cameraSectionPos);
   }

   public int size() {
      return this.storage.size();
   }

   public SectionUpdateTracker.@Nullable SectionDirtyState getDirtyState(final long sectionNode) {
      return this.storage.getValue(sectionNode);
   }

   public boolean hasAllNeighbors(final ClientLevel level, final long sectionNode) {
      return this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.WEST))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.NORTH))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.EAST))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.SOUTH))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, -1, 0, -1))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, -1, 0, 1))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, 1, 0, -1))
         && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, 1, 0, 1));
   }

   private boolean doesChunkExistAt(final ClientLevel level, final long sectionNode) {
      ChunkAccess chunk = level.getChunk(SectionPos.x(sectionNode), SectionPos.z(sectionNode), ChunkStatus.FULL, false);
      return chunk != null && level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(sectionNode));
   }

   public static class SectionDirtyState implements RotatingSectionStorage.Value {
      private boolean isDirty;
      private boolean isDirtyFromPlayer;
      private long sectionNode;

      private SectionDirtyState(final boolean isDirty, final boolean isDirtyFromPlayer, final long sectionNode) {
         this.isDirty = isDirty;
         this.isDirtyFromPlayer = isDirtyFromPlayer;
      }

      public void setDirty(final boolean fromPlayer) {
         boolean wasDirty = this.isDirty;
         this.isDirty = true;
         this.isDirtyFromPlayer = fromPlayer | (wasDirty && this.isDirtyFromPlayer);
      }

      public void setNotDirty() {
         this.isDirty = false;
         this.isDirtyFromPlayer = false;
      }

      @Override
      public void setSectionNode(final long sectionNode) {
         if (this.sectionNode != sectionNode) {
            this.sectionNode = sectionNode;
            this.isDirty = true;
            this.isDirtyFromPlayer = false;
         }
      }

      @Override
      public long getSectionNode() {
         return this.sectionNode;
      }

      public boolean isDirty() {
         return this.isDirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.isDirtyFromPlayer;
      }
   }
}
