package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

public class LeveledPriorityQueue {
   private final int levelCount;
   private final LongLinkedOpenHashSet[] queues;
   private int firstQueuedLevel;

   public LeveledPriorityQueue(int p_278289_, final int p_278259_) {
      this.levelCount = p_278289_;
      this.queues = new LongLinkedOpenHashSet[p_278289_];

      for (int i = 0; i < p_278289_; i++) {
         this.queues[i] = new LongLinkedOpenHashSet(p_278259_, 0.5F) {
            protected void rehash(int p_278313_) {
               if (p_278313_ > p_278259_) {
                  super.rehash(p_278313_);
               }
            }
         };
      }

      this.firstQueuedLevel = p_278289_;
   }

   public long removeFirstLong() {
      LongLinkedOpenHashSet longlinkedopenhashset = this.queues[this.firstQueuedLevel];
      long i = longlinkedopenhashset.removeFirstLong();
      if (longlinkedopenhashset.isEmpty()) {
         this.checkFirstQueuedLevel(this.levelCount);
      }

      return i;
   }

   public boolean isEmpty() {
      return this.firstQueuedLevel >= this.levelCount;
   }

   public void dequeue(long p_278232_, int p_278338_, int p_278345_) {
      LongLinkedOpenHashSet longlinkedopenhashset = this.queues[p_278338_];
      longlinkedopenhashset.remove(p_278232_);
      if (longlinkedopenhashset.isEmpty() && this.firstQueuedLevel == p_278338_) {
         this.checkFirstQueuedLevel(p_278345_);
      }
   }

   public void enqueue(long p_278311_, int p_278335_) {
      this.queues[p_278335_].add(p_278311_);
      if (this.firstQueuedLevel > p_278335_) {
         this.firstQueuedLevel = p_278335_;
      }
   }

   private void checkFirstQueuedLevel(int p_278303_) {
      int i = this.firstQueuedLevel;
      this.firstQueuedLevel = p_278303_;

      for (int j = i + 1; j < p_278303_; j++) {
         if (!this.queues[j].isEmpty()) {
            this.firstQueuedLevel = j;
            break;
         }
      }
   }
}
