package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class KillCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("kill")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> kill(c.getSource(), ImmutableList.of(c.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities()).executes(c -> kill(c.getSource(), EntityArgument.getEntities(c, "targets"))))
        );
    }

    private static int kill(final CommandSourceStack source, final Collection<? extends Entity> victims) {
        for (Entity entity : victims) {
            entity.kill(source.getLevel());
        }

        if (victims.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.kill.success.single", victims.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", victims.size()), true);
        }

        return victims.size();
    }
}