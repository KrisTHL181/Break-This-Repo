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

   private ContinuationTask(ContinuationTask.TaskProvider<T, P> p_312248_, List<P> p_311891_, Frame p_311182_) {
      this.taskFactory = p_312248_;
      this.arguments = p_311891_;
      this.selfEntry = new CommandQueueEntry<>(p_311182_, this);
   }

   @Override
   public void execute(ExecutionContext<T> p_310507_, Frame p_311035_) {
      P p = this.arguments.get(this.index);
      p_310507_.queueNext(this.taskFactory.create(p_311035_, p));
      if (++this.index < this.arguments.size()) {
         p_310507_.queueNext(this.selfEntry);
      }
   }

   public static <T, P> void schedule(ExecutionContext<T> p_311894_, Frame p_312100_, List<P> p_310159_, ContinuationTask.TaskProvider<T, P> p_309687_) {
      int i = p_310159_.size();
      switch (i) {
         case 0:
            break;
         case 1:
            p_311894_.queueNext(p_309687_.create(p_312100_, p_310159_.get(0)));
            break;
         case 2:
            p_311894_.queueNext(p_309687_.create(p_312100_, p_310159_.get(0)));
            p_311894_.queueNext(p_309687_.create(p_312100_, p_310159_.get(1)));
            break;
         default:
            p_311894_.queueNext((new ContinuationTask<>(p_309687_, p_310159_, p_312100_)).selfEntry);
      }
   }

   @FunctionalInterface
   public interface TaskProvider<T, P> {
      CommandQueueEntry<T> create(Frame var1, P var2);
   }
}
