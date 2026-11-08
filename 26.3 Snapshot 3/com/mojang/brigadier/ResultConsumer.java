package com.mojang.brigadier;

import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface ResultConsumer<S> {
   void onCommandComplete(CommandContext<S> var1, boolean var2, int var3);
}
