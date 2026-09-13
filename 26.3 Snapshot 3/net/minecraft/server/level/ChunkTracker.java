package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
   protected ChunkTracker(final int levelCount, final int minQueueSize, final int minMapSize) {
      super(levelCount, minQueueSize, minMapSize);
   }

   @Override
   protected boolean isSource(final long node) {
      return node == ChunkPos.INVALID_CHUNK_POS;
   }

   @Override
   protected void checkNeighborsAfterUpdate(final long node, final int level, final boolean onlyDecrease) {
      if (!onlyDecrease || level < this.levelCount - 2) {
         ChunkPos pos = ChunkPos.unpack(node);
         int x = pos.x();
         int z = pos.z();

         for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
               long neighbor = ChunkPos.pack(x + offsetX, z + offsetZ);
               if (neighbor != node) {
                  this.checkNeighbor(node, neighbor, level, onlyDecrease);
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(final long node, final long knownParent, final int knownLevelFromParent) {
      int computedLevel = knownLevelFromParent;
      ChunkPos pos = ChunkPos.unpack(node);
      int x = pos.x();
      int z = pos.z();

      for (int offsetX = -1; offsetX <= 1; offsetX++) {
         for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
            long neighbor = ChunkPos.pack(x + offsetX, z + offsetZ);
            if (neighbor == node) {
               neighbor = ChunkPos.INVALID_CHUNK_POS;
            }

            if (neighbor != knownParent) {
               int costFromNeighbor = this.computeLevelFromNeighbor(neighbor, node, this.getLevel(neighbor));
               if (computedLevel > costFromNeighbor) {
                  computedLevel = costFromNeighbor;
               }

               if (computedLevel == 0) {
                  return computedLevel;
               }
            }
         }
      }

      return computedLevel;
   }

   @Override
   protected int computeLevelFromNeighbor(final long from, final long to, final int fromLevel) {
      return from == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(to) : fromLevel + 1;
   }

   protected abstract int getLevelFromSource(long to);

   public void update(final long node, final int newLevelFrom, final boolean onlyDecreased) {
      this.checkEdge(ChunkPos.INVALID_CHUNK_POS, node, newLevelFrom, onlyDecreased);
   }
}
