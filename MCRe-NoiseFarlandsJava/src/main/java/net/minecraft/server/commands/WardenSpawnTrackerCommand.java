package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;

public class WardenSpawnTrackerCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("warden_spawn_tracker")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("clear").executes(c -> resetTracker(c.getSource(), ImmutableList.of(c.getSource().getPlayerOrException()))))
                .then(
                    Commands.literal("set")
                        .then(
                            Commands.argument("warning_level", IntegerArgumentType.integer(0, 4))
                                .executes(
                                    c -> setWarningLevel(
                                        c.getSource(),
                                        ImmutableList.of(c.getSource().getPlayerOrException()),
                                        IntegerArgumentType.getInteger(c, "warning_level")
                                    )
                                )
                        )
                )
        );
    }

    private static int setWarningLevel(final CommandSourceStack source, final Collection<? extends Player> players, final int warningLevel) {
        for (Player player : players) {
            player.getWardenSpawnTracker().ifPresent(wardenSpawnTracker -> wardenSpawnTracker.setWarningLevel(warningLevel));
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.warden_spawn_tracker.set.success.single", players.iterator().next().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.set.success.multiple", players.size()), true);
        }

        return players.size();
    }

    private static int resetTracker(final CommandSourceStack source, final Collection<? extends Player> players) {
        for (Player player : players) {
            player.getWardenSpawnTracker().ifPresent(WardenSpawnTracker::reset);
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.warden_spawn_tracker.clear.success.single", players.iterator().next().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.clear.success.multiple", players.size()), true);
        }

        return players.size();
    }
}