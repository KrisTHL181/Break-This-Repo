package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker extends DynamicGraphMinFixedPoint {
   protected SectionTracker(final int levelCount, final int minQueueSize, final int minMapSize) {
      super(levelCount, minQueueSize, minMapSize);
   }

   @Override
   protected void checkNeighborsAfterUpdate(final long node, final int level, final boolean onlyDecrease) {
      if (!onlyDecrease || level < this.levelCount - 2) {
         for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
               for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                  long neighbor = SectionPos.offset(node, offsetX, offsetY, offsetZ);
                  if (neighbor != node) {
                     this.checkNeighbor(node, neighbor, level, onlyDecrease);
                  }
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(final long node, final long knownParent, final int knownLevelFromParent) {
      int computedLevel = knownLevelFromParent;

      for (int offsetX = -1; offsetX <= 1; offsetX++) {
         for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
               long neighbor = SectionPos.offset(node, offsetX, offsetY, offsetZ);
               if (neighbor == node) {
                  neighbor = Long.MAX_VALUE;
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
      }

      return computedLevel;
   }

   @Override
   protected int computeLevelFromNeighbor(final long from, final long to, final int fromLevel) {
      return this.isSource(from) ? this.getLevelFromSource(to) : fromLevel + 1;
   }

   protected abstract int getLevelFromSource(long to);

   public void update(final long node, final int newLevelFrom, final boolean onlyDecreased) {
      this.checkEdge(Long.MAX_VALUE, node, newLevelFrom, onlyDecreased);
   }
}
