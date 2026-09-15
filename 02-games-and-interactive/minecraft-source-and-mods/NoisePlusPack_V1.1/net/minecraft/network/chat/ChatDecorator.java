package net.minecraft.network.chat;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ChatDecorator {
   ChatDecorator PLAIN = (p_296388_, p_296389_) -> p_296389_;

   Component decorate(@Nullable ServerPlayer var1, Component var2);
}
