package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;

public abstract class DynamicGraphMinFixedPoint {
   public static final long SOURCE = Long.MAX_VALUE;
   private static final int NO_COMPUTED_LEVEL = 255;
   protected final int levelCount;
   private final LeveledPriorityQueue priorityQueue;
   private final Long2ByteMap computedLevels;
   private volatile boolean hasWork;

   protected DynamicGraphMinFixedPoint(final int levelCount, final int minQueueSize, final int minMapSize) {
      if (levelCount >= 254) {
         throw new IllegalArgumentException("Level count must be < 254.");
      }

      this.levelCount = levelCount;
      this.priorityQueue = new LeveledPriorityQueue(levelCount, minQueueSize);
      this.computedLevels = new Long2ByteOpenHashMap(minMapSize, 0.5F) {
         protected void rehash(final int newN) {
            if (newN > minMapSize) {
               super.rehash(newN);
            }
         }
      };
      this.computedLevels.defaultReturnValue((byte)-1);
   }

   protected void removeFromQueue(final long node) {
      int computedLevel = this.computedLevels.remove(node) & 255;
      if (computedLevel != 255) {
         int level = this.getLevel(node);
         int priority = this.calculatePriority(level, computedLevel);
         this.priorityQueue.dequeue(node, priority, this.levelCount);
         this.hasWork = !this.priorityQueue.isEmpty();
      }
   }

   public void removeIf(final LongPredicate pred) {
      LongList nodesToRemove = new LongArrayList();
      this.computedLevels.keySet().forEach(node -> {
         if (pred.test(node)) {
            nodesToRemove.add(node);
         }
      });
      nodesToRemove.forEach(this::removeFromQueue);
   }

   private int calculatePriority(final int level, final int computedLevel) {
      return Math.min(Math.min(level, computedLevel), this.levelCount - 1);
   }

   protected void checkNode(final long node) {
      this.checkEdge(node, node, this.levelCount - 1, false);
   }

   protected void checkEdge(final long from, final long to, final int newLevelFrom, final boolean onlyDecreased) {
      this.checkEdge(from, to, newLevelFrom, this.getLevel(to), this.computedLevels.get(to) & 255, onlyDecreased);
      this.hasWork = !this.priorityQueue.isEmpty();
   }

   private void checkEdge(final long from, final long to, int newLevelFrom, int levelTo, int oldComputedLevel, final boolean onlyDecreased) {
      if (!this.isSource(to)) {
         newLevelFrom = Mth.clamp(newLevelFrom, 0, this.levelCount - 1);
         levelTo = Mth.clamp(levelTo, 0, this.levelCount - 1);
         boolean wasConsistent = oldComputedLevel == 255;
         if (wasConsistent) {
            oldComputedLevel = levelTo;
         }

         int newComputedLevel;
         if (onlyDecreased) {
            newComputedLevel = Math.min(oldComputedLevel, newLevelFrom);
         } else {
            newComputedLevel = Mth.clamp(this.getComputedLevel(to, from, newLevelFrom), 0, this.levelCount - 1);
         }

         int oldPriority = this.calculatePriority(levelTo, oldComputedLevel);
         if (levelTo != newComputedLevel) {
            int newPriority = this.calculatePriority(levelTo, newComputedLevel);
            if (oldPriority != newPriority && !wasConsistent) {
               this.priorityQueue.dequeue(to, oldPriority, newPriority);
            }

            this.priorityQueue.enqueue(to, newPriority);
            this.computedLevels.put(to, (byte)newComputedLevel);
         } else if (!wasConsistent) {
            this.priorityQueue.dequeue(to, oldPriority, this.levelCount);
            this.computedLevels.remove(to);
         }
      }
   }

   protected final void checkNeighbor(final long from, final long to, final int level, final boolean onlyDecreased) {
      int storedOldComputedLevel = this.computedLevels.get(to) & 255;
      int levelFrom = Mth.clamp(this.computeLevelFromNeighbor(from, to, level), 0, this.levelCount - 1);
      if (onlyDecreased) {
         this.checkEdge(from, to, levelFrom, this.getLevel(to), storedOldComputedLevel, onlyDecreased);
      } else {
         boolean wasConsistent = storedOldComputedLevel == 255;
         int oldComputedLevel;
         if (wasConsistent) {
            oldComputedLevel = Mth.clamp(this.getLevel(to), 0, this.levelCount - 1);
         } else {
            oldComputedLevel = storedOldComputedLevel;
         }

         if (levelFrom == oldComputedLevel) {
            this.checkEdge(from, to, this.levelCount - 1, wasConsistent ? oldComputedLevel : this.getLevel(to), storedOldComputedLevel, onlyDecreased);
         }
      }
   }

   protected final boolean hasWork() {
      return this.hasWork;
   }

   protected final int runUpdates(int count) {
      if (this.priorityQueue.isEmpty()) {
         return count;
      }

      while (!this.priorityQueue.isEmpty() && count > 0) {
         count--;
         long node = this.priorityQueue.removeFirstLong();
         int level = Mth.clamp(this.getLevel(node), 0, this.levelCount - 1);
         int computedLevel = this.computedLevels.remove(node) & 255;
         if (computedLevel < level) {
            this.setLevel(node, computedLevel);
            this.checkNeighborsAfterUpdate(node, computedLevel, true);
         } else if (computedLevel > level) {
            this.setLevel(node, this.levelCount - 1);
            if (computedLevel != this.levelCount - 1) {
               this.priorityQueue.enqueue(node, this.calculatePriority(this.levelCount - 1, computedLevel));
               this.computedLevels.put(node, (byte)computedLevel);
            }

            this.checkNeighborsAfterUpdate(node, level, false);
         }
      }

      this.hasWork = !this.priorityQueue.isEmpty();
      return count;
   }

   public int getQueueSize() {
      return this.computedLevels.size();
   }

   protected boolean isSource(final long node) {
      return node == Long.MAX_VALUE;
   }

   protected abstract int getComputedLevel(final long node, final long knownParent, final int knownLevelFromParent);

   protected abstract void checkNeighborsAfterUpdate(final long node, final int level, final boolean onlyDecrease);

   protected abstract int getLevel(long node);

   protected abstract void setLevel(long node, int level);

   protected abstract int computeLevelFromNeighbor(long from, long to, final int fromLevel);
}
