package net.minecraft.commands.execution;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;

public interface CustomModifierExecutor<T> {
   void apply(T var1, List<T> var2, ContextChain<T> var3, ChainModifiers var4, ExecutionControl<T> var5);

   interface ModifierAdapter<T> extends RedirectModifier<T>, CustomModifierExecutor<T> {
      default Collection<T> apply(CommandContext<T> p_311038_) throws CommandSyntaxException {
         throw new UnsupportedOperationException("This function should not run");
      }
   }
}
