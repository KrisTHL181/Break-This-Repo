package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

public class LeveledPriorityQueue {
   private final int levelCount;
   private final LongLinkedOpenHashSet[] queues;
   private int firstQueuedLevel;

   public LeveledPriorityQueue(final int levelCount, final int minSize) {
      this.levelCount = levelCount;
      this.queues = new LongLinkedOpenHashSet[levelCount];

      for (int i = 0; i < levelCount; i++) {
         this.queues[i] = new LongLinkedOpenHashSet(minSize, 0.5F) {
            protected void rehash(final int newN) {
               if (newN > minSize) {
                  super.rehash(newN);
               }
            }
         };
      }

      this.firstQueuedLevel = levelCount;
   }

   public long removeFirstLong() {
      LongLinkedOpenHashSet queue = this.queues[this.firstQueuedLevel];
      long result = queue.removeFirstLong();
      if (queue.isEmpty()) {
         this.checkFirstQueuedLevel(this.levelCount);
      }

      return result;
   }

   public boolean isEmpty() {
      return this.firstQueuedLevel >= this.levelCount;
   }

   public void dequeue(final long node, final int key, final int upperBound) {
      LongLinkedOpenHashSet queue = this.queues[key];
      queue.remove(node);
      if (queue.isEmpty() && this.firstQueuedLevel == key) {
         this.checkFirstQueuedLevel(upperBound);
      }
   }

   public void enqueue(final long node, final int key) {
      this.queues[key].add(node);
      if (this.firstQueuedLevel > key) {
         this.firstQueuedLevel = key;
      }
   }

   private void checkFirstQueuedLevel(final int upperBound) {
      int oldLevel = this.firstQueuedLevel;
      this.firstQueuedLevel = upperBound;

      for (int i = oldLevel + 1; i < upperBound; i++) {
         if (!this.queues[i].isEmpty()) {
            this.firstQueuedLevel = i;
            break;
         }
      }
   }
}
