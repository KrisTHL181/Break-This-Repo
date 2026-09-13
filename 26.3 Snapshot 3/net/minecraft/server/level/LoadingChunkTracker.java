package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

public class LoadingChunkTracker extends ChunkTracker {
   private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
   private final DistanceManager distanceManager;
   private final TicketStorage ticketStorage;

   public LoadingChunkTracker(final DistanceManager distanceManager, final TicketStorage ticketStorage) {
      super(MAX_LEVEL + 1, 16, 256);
      this.distanceManager = distanceManager;
      this.ticketStorage = ticketStorage;
      ticketStorage.setLoadingChunkUpdatedListener(this::update);
   }

   @Override
   protected int getLevelFromSource(final long to) {
      return this.ticketStorage.getTicketLevelAt(to, false);
   }

   @Override
   protected int getLevel(final long node) {
      if (!this.distanceManager.isChunkToRemove(node)) {
         ChunkHolder chunk = this.distanceManager.getChunk(node);
         if (chunk != null) {
            return chunk.getTicketLevel();
         }
      }

      return MAX_LEVEL;
   }

   @Override
   protected void setLevel(final long node, final int level) {
      ChunkHolder chunk = this.distanceManager.getChunk(node);
      int oldLevel = chunk == null ? MAX_LEVEL : chunk.getTicketLevel();
      if (oldLevel != level) {
         chunk = this.distanceManager.updateChunkScheduling(node, level, chunk, oldLevel);
         if (chunk != null) {
            this.distanceManager.chunksToUpdateFutures.add(chunk);
         }
      }
   }

   public int runDistanceUpdates(final int count) {
      return this.runUpdates(count);
   }
}
