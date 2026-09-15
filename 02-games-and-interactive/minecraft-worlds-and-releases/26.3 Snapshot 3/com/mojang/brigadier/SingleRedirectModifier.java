package com.mojang.brigadier;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface SingleRedirectModifier<S> {
   S apply(CommandContext<S> var1) throws CommandSyntaxException;
}
