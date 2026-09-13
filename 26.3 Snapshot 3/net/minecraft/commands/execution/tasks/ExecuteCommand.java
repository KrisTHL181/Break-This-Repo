package net.minecraft.commands.execution.tasks;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;

public class ExecuteCommand<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
   private final String commandInput;
   private final ChainModifiers modifiers;
   private final CommandContext<T> executionContext;

   public ExecuteCommand(final String commandInput, final ChainModifiers modifiers, final CommandContext<T> executionContext) {
      this.commandInput = commandInput;
      this.modifiers = modifiers;
      this.executionContext = executionContext;
   }

   public void execute(final T sender, final ExecutionContext<T> context, final Frame frame) {
      context.profiler().push(() -> "execute " + this.commandInput);

      try {
         context.incrementCost();
         int result = ContextChain.runExecutable(this.executionContext, sender, ExecutionCommandSource.resultConsumer(), this.modifiers.isForked());
         TraceCallbacks tracer = context.tracer();
         if (tracer != null) {
            tracer.onReturn(frame.depth(), this.commandInput, result);
         }
      } catch (CommandSyntaxException e) {
         sender.handleError(e, this.modifiers.isForked(), context.tracer());
      } finally {
         context.profiler().pop();
      }
   }
}
