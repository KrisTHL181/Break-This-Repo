package net.minecraft.server.level;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ChunkTaskPriorityQueue {
   public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
   private final List<Long2ObjectLinkedOpenHashMap<List<Runnable>>> queuesPerPriority = IntStream.range(0, PRIORITY_LEVEL_COUNT)
      .mapToObj(p_140520_ -> new Long2ObjectLinkedOpenHashMap())
      .toList();
   private volatile int topPriorityQueueIndex = PRIORITY_LEVEL_COUNT;
   private final String name;

   public ChunkTaskPriorityQueue(String p_140516_) {
      this.name = p_140516_;
   }

   protected void resortChunkTasks(int p_140522_, ChunkPos p_140523_, int p_140524_) {
      if (p_140522_ < PRIORITY_LEVEL_COUNT) {
         Long2ObjectLinkedOpenHashMap<List<Runnable>> long2objectlinkedopenhashmap = this.queuesPerPriority.get(p_140522_);
         List<Runnable> list = (List<Runnable>)long2objectlinkedopenhashmap.remove(p_140523_.toLong());
         if (p_140522_ == this.topPriorityQueueIndex) {
            while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
               this.topPriorityQueueIndex++;
            }
         }

         if (list != null && !list.isEmpty()) {
            ((List)this.queuesPerPriority.get(p_140524_).computeIfAbsent(p_140523_.toLong(), p_140547_ -> Lists.newArrayList())).addAll(list);
            this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, p_140524_);
         }
      }
   }

   protected void submit(Runnable p_369824_, long p_140537_, int p_140538_) {
      ((List)this.queuesPerPriority.get(p_140538_).computeIfAbsent(p_140537_, p_140545_ -> Lists.newArrayList())).add(p_369824_);
      this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, p_140538_);
   }

   protected void release(long p_140531_, boolean p_140532_) {
      for (Long2ObjectLinkedOpenHashMap<List<Runnable>> long2objectlinkedopenhashmap : this.queuesPerPriority) {
         List<Runnable> list = (List<Runnable>)long2objectlinkedopenhashmap.get(p_140531_);
         if (list != null) {
            if (p_140532_) {
               list.clear();
            }

            if (list.isEmpty()) {
               long2objectlinkedopenhashmap.remove(p_140531_);
            }
         }
      }

      while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
         this.topPriorityQueueIndex++;
      }
   }

   public ChunkTaskPriorityQueue.@Nullable TasksForChunk pop() {
      if (!this.hasWork()) {
         return null;
      }

      int i = this.topPriorityQueueIndex;
      Long2ObjectLinkedOpenHashMap<List<Runnable>> long2objectlinkedopenhashmap = this.queuesPerPriority.get(i);
      long j = long2objectlinkedopenhashmap.firstLongKey();
      List<Runnable> list = (List<Runnable>)long2objectlinkedopenhashmap.removeFirst();

      while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
         this.topPriorityQueueIndex++;
      }

      return new ChunkTaskPriorityQueue.TasksForChunk(j, list);
   }

   public boolean hasWork() {
      return this.topPriorityQueueIndex < PRIORITY_LEVEL_COUNT;
   }

   @Override
   public String toString() {
      return this.name + " " + this.topPriorityQueueIndex + "...";
   }

   public record TasksForChunk(long chunkPos, List<Runnable> tasks) {
   }
}
