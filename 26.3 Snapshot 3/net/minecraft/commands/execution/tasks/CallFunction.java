package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.InstantiatedFunction;

public class CallFunction<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
   private final InstantiatedFunction<T> function;
   private final CommandResultCallback resultCallback;
   private final boolean returnParentFrame;

   public CallFunction(final InstantiatedFunction<T> function, final CommandResultCallback resultCallback, final boolean returnParentFrame) {
      this.function = function;
      this.resultCallback = resultCallback;
      this.returnParentFrame = returnParentFrame;
   }

   public void execute(final T sender, final ExecutionContext<T> context, final Frame frame) {
      context.incrementCost();
      List<UnboundEntryAction<T>> contents = this.function.entries();
      TraceCallbacks tracer = context.tracer();
      if (tracer != null) {
         tracer.onCall(frame.depth(), this.function.id(), this.function.entries().size());
      }

      int newDepth = frame.depth() + 1;
      Frame.FrameControl frameControl = this.returnParentFrame ? frame.frameControl() : context.frameControlForDepth(newDepth);
      Frame newFrame = new Frame(newDepth, this.resultCallback, frameControl);
      ContinuationTask.schedule(context, newFrame, contents, (frame1, entryAction) -> new CommandQueueEntry<>(frame1, entryAction.bind(sender)));
   }
}
