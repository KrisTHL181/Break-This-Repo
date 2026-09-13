package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("dialog")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("show")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("dialog", ResourceOrIdArgument.dialog(context))
                                        .executes(
                                            c -> showDialog(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                ResourceOrIdArgument.getDialog(c, "dialog")
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("clear")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .executes(c -> clearDialog(c.getSource(), EntityArgument.getPlayers(c, "targets")))
                        )
                )
        );
    }

    private static int showDialog(final CommandSourceStack sender, final Collection<ServerPlayer> targets, final Holder<Dialog> dialog) {
        for (ServerPlayer target : targets) {
            target.openDialog(dialog);
        }

        if (targets.size() == 1) {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.show.single", targets.iterator().next().getDisplayName()), true);
        } else {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.show.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int clearDialog(final CommandSourceStack sender, final Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            target.connection.send(ClientboundClearDialogPacket.INSTANCE);
        }

        if (targets.size() == 1) {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.clear.single", targets.iterator().next().getDisplayName()), true);
        } else {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.clear.multiple", targets.size()), true);
        }

        return targets.size();
    }
}