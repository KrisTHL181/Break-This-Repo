package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StopCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stop").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)).executes(c -> {
            c.getSource().sendSuccess(() -> Component.translatable("commands.stop.stopping"), true);
            c.getSource().getServer().halt(false);
            return 1;
        }));
    }
}