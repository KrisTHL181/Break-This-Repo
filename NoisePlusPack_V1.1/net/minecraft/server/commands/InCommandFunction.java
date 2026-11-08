package net.minecraft.server.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface InCommandFunction<T, R> {
   R apply(T var1) throws CommandSyntaxException;
}
