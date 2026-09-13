package net.minecraft.commands.execution;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import org.jspecify.annotations.Nullable;

public interface CustomCommandExecutor<T> {
   void run(T var1, ContextChain<T> var2, ChainModifiers var3, ExecutionControl<T> var4);

   interface CommandAdapter<T> extends Command<T>, CustomCommandExecutor<T> {
      default int run(CommandContext<T> p_309955_) throws CommandSyntaxException {
         throw new UnsupportedOperationException("This function should not run");
      }
   }

   abstract class WithErrorHandling<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor<T> {
      public final void run(T p_310241_, ContextChain<T> p_311766_, ChainModifiers p_310779_, ExecutionControl<T> p_309382_) {
         try {
            this.runGuarded(p_310241_, p_311766_, p_310779_, p_309382_);
         } catch (CommandSyntaxException commandsyntaxexception) {
            this.onError(commandsyntaxexception, p_310241_, p_310779_, p_309382_.tracer());
            p_310241_.callback().onFailure();
         }
      }

      protected void onError(CommandSyntaxException p_313040_, T p_312743_, ChainModifiers p_309642_, @Nullable TraceCallbacks p_309545_) {
         p_312743_.handleError(p_313040_, p_309642_.isForked(), p_309545_);
      }

      protected abstract void runGuarded(T var1, ContextChain<T> var2, ChainModifiers var3, ExecutionControl<T> var4) throws CommandSyntaxException;
   }
}
