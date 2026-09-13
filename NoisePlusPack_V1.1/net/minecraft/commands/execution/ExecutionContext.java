package net.minecraft.commands.execution;

import com.google.common.collect.Queues;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ExecutionContext<T> implements AutoCloseable {
   private static final int MAX_QUEUE_DEPTH = 10000000;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final int commandLimit;
   private final int forkLimit;
   private final ProfilerFiller profiler;
   private @Nullable TraceCallbacks tracer;
   private int commandQuota;
   private boolean queueOverflow;
   private final Deque<CommandQueueEntry<T>> commandQueue = Queues.newArrayDeque();
   private final List<CommandQueueEntry<T>> newTopCommands = new ObjectArrayList();
   private int currentFrameDepth;

   public ExecutionContext(int p_313193_, int p_311309_, ProfilerFiller p_309602_) {
      this.commandLimit = p_313193_;
      this.forkLimit = p_311309_;
      this.profiler = p_309602_;
      this.commandQuota = p_313193_;
   }

   private static <T extends ExecutionCommandSource<T>> Frame createTopFrame(ExecutionContext<T> p_310887_, CommandResultCallback p_311060_) {
      if (p_310887_.currentFrameDepth == 0) {
         return new Frame(0, p_311060_, p_310887_.commandQueue::clear);
      }

      int i = p_310887_.currentFrameDepth + 1;
      return new Frame(i, p_311060_, p_310887_.frameControlForDepth(i));
   }

   public static <T extends ExecutionCommandSource<T>> void queueInitialFunctionCall(
      ExecutionContext<T> p_311344_, InstantiatedFunction<T> p_309533_, T p_310187_, CommandResultCallback p_310874_
   ) {
      p_311344_.queueNext(
         new CommandQueueEntry<>(createTopFrame(p_311344_, p_310874_), new CallFunction<>(p_309533_, p_310187_.callback(), false).bind(p_310187_))
      );
   }

   public static <T extends ExecutionCommandSource<T>> void queueInitialCommandExecution(
      ExecutionContext<T> p_311278_, String p_310967_, ContextChain<T> p_311656_, T p_312145_, CommandResultCallback p_309674_
   ) {
      p_311278_.queueNext(new CommandQueueEntry<>(createTopFrame(p_311278_, p_309674_), new BuildContexts.TopLevel<>(p_310967_, p_311656_, p_312145_)));
   }

   private void handleQueueOverflow() {
      this.queueOverflow = true;
      this.newTopCommands.clear();
      this.commandQueue.clear();
   }

   public void queueNext(CommandQueueEntry<T> p_311113_) {
      if (this.newTopCommands.size() + this.commandQueue.size() > 10000000) {
         this.handleQueueOverflow();
      }

      if (!this.queueOverflow) {
         this.newTopCommands.add(p_311113_);
      }
   }

   public void discardAtDepthOrHigher(int p_313117_) {
      while (!this.commandQueue.isEmpty() && this.commandQueue.peek().frame().depth() >= p_313117_) {
         this.commandQueue.removeFirst();
      }
   }

   public Frame.FrameControl frameControlForDepth(int p_311323_) {
      return () -> this.discardAtDepthOrHigher(p_311323_);
   }

   public void runCommandQueue() {
      this.pushNewCommands();

      while (true) {
         if (this.commandQuota <= 0) {
            LOGGER.info("Command execution stopped due to limit (executed {} commands)", this.commandLimit);
            break;
         }

         CommandQueueEntry<T> commandqueueentry = this.commandQueue.pollFirst();
         if (commandqueueentry == null) {
            return;
         }

         this.currentFrameDepth = commandqueueentry.frame().depth();
         commandqueueentry.execute(this);
         if (this.queueOverflow) {
            LOGGER.error("Command execution stopped due to command queue overflow (max {})", 10000000);
            break;
         }

         this.pushNewCommands();
      }

      this.currentFrameDepth = 0;
   }

   private void pushNewCommands() {
      for (int i = this.newTopCommands.size() - 1; i >= 0; i--) {
         this.commandQueue.addFirst(this.newTopCommands.get(i));
      }

      this.newTopCommands.clear();
   }

   public void tracer(@Nullable TraceCallbacks p_309595_) {
      this.tracer = p_309595_;
   }

   public @Nullable TraceCallbacks tracer() {
      return this.tracer;
   }

   public ProfilerFiller profiler() {
      return this.profiler;
   }

   public int forkLimit() {
      return this.forkLimit;
   }

   public void incrementCost() {
      this.commandQuota--;
   }

   @Override
   public void close() {
      if (this.tracer != null) {
         this.tracer.close();
      }
   }
}
