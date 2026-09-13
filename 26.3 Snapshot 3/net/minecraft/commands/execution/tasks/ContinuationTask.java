package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;

public class ContinuationTask<T, P> implements EntryAction<T> {
   private final ContinuationTask.TaskProvider<T, P> taskFactory;
   private final List<P> arguments;
   private final CommandQueueEntry<T> selfEntry;
   private int index;

   private ContinuationTask(final ContinuationTask.TaskProvider<T, P> taskFactory, final List<P> arguments, final Frame frame) {
      this.taskFactory = taskFactory;
      this.arguments = arguments;
      this.selfEntry = new CommandQueueEntry<>(frame, this);
   }

   @Override
   public void execute(final ExecutionContext<T> context, final Frame frame) {
      P argument = this.arguments.get(this.index);
      context.queueNext(this.taskFactory.create(frame, argument));
      if (++this.index < this.arguments.size()) {
         context.queueNext(this.selfEntry);
      }
   }

   public static <T, P> void schedule(
      final ExecutionContext<T> context, final Frame frame, final List<P> arguments, final ContinuationTask.TaskProvider<T, P> taskFactory
   ) {
      int argumentCount = arguments.size();
      switch (argumentCount) {
         case 0:
            break;
         case 1:
            context.queueNext(taskFactory.create(frame, arguments.get(0)));
            break;
         case 2:
            context.queueNext(taskFactory.create(frame, arguments.get(0)));
            context.queueNext(taskFactory.create(frame, arguments.get(1)));
            break;
         default:
            context.queueNext((new ContinuationTask<>(taskFactory, arguments, frame)).selfEntry);
      }
   }

   @FunctionalInterface
   public interface TaskProvider<T, P> {
      CommandQueueEntry<T> create(Frame frame, P argument);
   }
}
