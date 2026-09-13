package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkTaskDispatcher implements ChunkHolder.LevelChangeListener, AutoCloseable {
   public static final int DISPATCHER_PRIORITY_COUNT = 4;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ChunkTaskPriorityQueue queue;
   private final TaskScheduler<Runnable> executor;
   private final PriorityConsecutiveExecutor dispatcher;
   protected boolean sleeping;

   public ChunkTaskDispatcher(TaskScheduler<Runnable> p_361144_, Executor p_369214_) {
      this.queue = new ChunkTaskPriorityQueue(p_361144_.name() + "_queue");
      this.executor = p_361144_;
      this.dispatcher = new PriorityConsecutiveExecutor(4, p_369214_, "dispatcher");
      this.sleeping = true;
   }

   public boolean hasWork() {
      return this.dispatcher.hasWork() || this.queue.hasWork();
   }

   @Override
   public void onLevelChange(ChunkPos p_368881_, IntSupplier p_362965_, int p_369655_, IntConsumer p_365320_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(0, () -> {
         int i = p_362965_.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("RES {} {} -> {}", new Object[]{p_368881_, i, p_369655_});
         }

         this.queue.resortChunkTasks(i, p_368881_, p_369655_);
         p_365320_.accept(p_369655_);
      }));
   }

   public void release(long p_369489_, Runnable p_365183_, boolean p_369881_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(1, () -> {
         this.queue.release(p_369489_, p_369881_);
         this.onRelease(p_369489_);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }

         p_365183_.run();
      }));
   }

   public void submit(Runnable p_364984_, long p_364993_, IntSupplier p_367388_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(2, () -> {
         int i = p_367388_.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("SUB {} {} {} {}", new Object[]{new ChunkPos(p_364993_), i, this.executor, this.queue});
         }

         this.queue.submit(p_364984_, p_364993_, i);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }
      }));
   }

   protected void pollTask() {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(3, () -> {
         ChunkTaskPriorityQueue.TasksForChunk chunktaskpriorityqueue$tasksforchunk = this.popTasks();
         if (chunktaskpriorityqueue$tasksforchunk == null) {
            this.sleeping = true;
         } else {
            this.scheduleForExecution(chunktaskpriorityqueue$tasksforchunk);
         }
      }));
   }

   protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk p_361766_) {
      CompletableFuture.allOf(p_361766_.tasks().stream().map(p_363376_ -> this.executor.scheduleWithResult(p_366925_ -> {
         p_363376_.run();
         p_366925_.complete(Unit.INSTANCE);
      })).toArray(CompletableFuture[]::new)).thenAccept(p_367735_ -> this.pollTask());
   }

   protected void onRelease(long p_362676_) {
   }

   protected ChunkTaskPriorityQueue.@Nullable TasksForChunk popTasks() {
      return this.queue.pop();
   }

   @Override
   public void close() {
      this.executor.close();
   }
}
