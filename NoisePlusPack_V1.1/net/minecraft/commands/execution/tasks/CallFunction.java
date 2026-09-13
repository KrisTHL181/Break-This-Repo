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

   public CallFunction(InstantiatedFunction<T> p_311175_, CommandResultCallback p_310950_, boolean p_309425_) {
      this.function = p_311175_;
      this.resultCallback = p_310950_;
      this.returnParentFrame = p_309425_;
   }

   public void execute(T p_312557_, ExecutionContext<T> p_312618_, Frame p_310825_) {
      p_312618_.incrementCost();
      List<UnboundEntryAction<T>> list = this.function.entries();
      TraceCallbacks tracecallbacks = p_312618_.tracer();
      if (tracecallbacks != null) {
         tracecallbacks.onCall(p_310825_.depth(), this.function.id(), this.function.entries().size());
      }

      int i = p_310825_.depth() + 1;
      Frame.FrameControl frame$framecontrol = this.returnParentFrame ? p_310825_.frameControl() : p_312618_.frameControlForDepth(i);
      Frame frame = new Frame(i, this.resultCallback, frame$framecontrol);
      ContinuationTask.schedule(p_312618_, frame, list, (p_310328_, p_313182_) -> new CommandQueueEntry<>(p_310328_, p_313182_.bind(p_312557_)));
   }
}
