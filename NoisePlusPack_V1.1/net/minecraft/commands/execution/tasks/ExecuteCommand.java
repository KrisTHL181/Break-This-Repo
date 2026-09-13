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

   public ExecuteCommand(String p_310766_, ChainModifiers p_309629_, CommandContext<T> p_310460_) {
      this.commandInput = p_310766_;
      this.modifiers = p_309629_;
      this.executionContext = p_310460_;
   }

   public void execute(T p_310632_, ExecutionContext<T> p_310757_, Frame p_311301_) {
      p_310757_.profiler().push(() -> "execute " + this.commandInput);

      try {
         p_310757_.incrementCost();
         int i = ContextChain.runExecutable(this.executionContext, p_310632_, ExecutionCommandSource.resultConsumer(), this.modifiers.isForked());
         TraceCallbacks tracecallbacks = p_310757_.tracer();
         if (tracecallbacks != null) {
            tracecallbacks.onReturn(p_311301_.depth(), this.commandInput, i);
         }
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_310632_.handleError(commandsyntaxexception, this.modifiers.isForked(), p_310757_.tracer());
      } finally {
         p_310757_.profiler().pop();
      }
   }
}
