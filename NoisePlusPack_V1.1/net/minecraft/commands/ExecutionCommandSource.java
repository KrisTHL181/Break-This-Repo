package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.jspecify.annotations.Nullable;

public interface ExecutionCommandSource<T extends ExecutionCommandSource<T>> extends PermissionSetSupplier {
   T withCallback(CommandResultCallback var1);

   CommandResultCallback callback();

   default T clearCallbacks() {
      return this.withCallback(CommandResultCallback.EMPTY);
   }

   CommandDispatcher<T> dispatcher();

   void handleError(CommandExceptionType var1, Message var2, boolean var3, @Nullable TraceCallbacks var4);

   boolean isSilent();

   default void handleError(CommandSyntaxException p_311076_, boolean p_310707_, @Nullable TraceCallbacks p_311569_) {
      this.handleError(p_311076_.getType(), p_311076_.getRawMessage(), p_310707_, p_311569_);
   }

   static <T extends ExecutionCommandSource<T>> ResultConsumer<T> resultConsumer() {
      return (p_310000_, p_311414_, p_311999_) -> ((ExecutionCommandSource)p_310000_.getSource()).callback().onResult(p_311414_, p_311999_);
   }
}
