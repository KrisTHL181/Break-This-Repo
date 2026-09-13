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

   public ChunkTaskDispatcher(final TaskScheduler<Runnable> executor, final Executor dispatcherExecutor) {
      this.queue = new ChunkTaskPriorityQueue(executor.name() + "_queue");
      this.executor = executor;
      this.dispatcher = new PriorityConsecutiveExecutor(4, dispatcherExecutor, "dispatcher");
      this.sleeping = true;
   }

   public boolean hasWork() {
      return this.dispatcher.hasWork() || this.queue.hasWork();
   }

   @Override
   public void onLevelChange(final ChunkPos pos, final IntSupplier oldLevel, final int newLevel, final IntConsumer setQueueLevel) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(0, () -> {
         int oldTicketLevel = oldLevel.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("RES {} {} -> {}", new Object[]{pos, oldTicketLevel, newLevel});
         }

         this.queue.resortChunkTasks(oldTicketLevel, pos, newLevel);
         setQueueLevel.accept(newLevel);
      }));
   }

   public void release(final long pos, final Runnable whenReleased, final boolean clearQueue) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(1, () -> {
         this.queue.release(pos, clearQueue);
         this.onRelease(pos);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }

         whenReleased.run();
      }));
   }

   public void submit(final Runnable task, final long pos, final IntSupplier level) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(2, () -> {
         int ticketLevel = level.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("SUB {} {} {} {}", new Object[]{ChunkPos.unpack(pos), ticketLevel, this.executor, this.queue});
         }

         this.queue.submit(task, pos, ticketLevel);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }
      }));
   }

   protected void pollTask() {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(3, () -> {
         ChunkTaskPriorityQueue.TasksForChunk tasksForChunk = this.popTasks();
         if (tasksForChunk == null) {
            this.sleeping = true;
         } else {
            this.scheduleForExecution(tasksForChunk);
         }
      }));
   }

   protected void scheduleForExecution(final ChunkTaskPriorityQueue.TasksForChunk tasksForChunk) {
      CompletableFuture.allOf(tasksForChunk.tasks().stream().map(message -> this.executor.scheduleWithResult(future -> {
         message.run();
         future.complete(Unit.INSTANCE);
      })).toArray(CompletableFuture[]::new)).thenAccept(r -> this.pollTask());
   }

   protected void onRelease(final long key) {
   }

   protected ChunkTaskPriorityQueue.@Nullable TasksForChunk popTasks() {
      return this.queue.pop();
   }

   @Override
   public void close() {
      this.executor.close();
   }
}
