package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("title")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal("clear").executes(c -> clearTitle(c.getSource(), EntityArgument.getPlayers(c, "targets"))))
                        .then(Commands.literal("reset").executes(c -> resetTitle(c.getSource(), EntityArgument.getPlayers(c, "targets"))))
                        .then(
                            Commands.literal("title")
                                .then(
                                    Commands.argument("title", ComponentArgument.textComponent(context))
                                        .executes(
                                            c -> showTitle(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                ComponentArgument.getRawComponent(c, "title"),
                                                "title",
                                                ClientboundSetTitleTextPacket::new
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("subtitle")
                                .then(
                                    Commands.argument("title", ComponentArgument.textComponent(context))
                                        .executes(
                                            c -> showTitle(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                ComponentArgument.getRawComponent(c, "title"),
                                                "subtitle",
                                                ClientboundSetSubtitleTextPacket::new
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("actionbar")
                                .then(
                                    Commands.argument("title", ComponentArgument.textComponent(context))
                                        .executes(
                                            c -> showTitle(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                ComponentArgument.getRawComponent(c, "title"),
                                                "actionbar",
                                                ClientboundSetActionBarTextPacket::new
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("times")
                                .then(
                                    Commands.argument("fadeIn", TimeArgument.time())
                                        .then(
                                            Commands.argument("stay", TimeArgument.time())
                                                .then(
                                                    Commands.argument("fadeOut", TimeArgument.time())
                                                        .executes(
                                                            c -> setTimes(
                                                                c.getSource(),
                                                                EntityArgument.getPlayers(c, "targets"),
                                                                IntegerArgumentType.getInteger(c, "fadeIn"),
                                                                IntegerArgumentType.getInteger(c, "stay"),
                                                                IntegerArgumentType.getInteger(c, "fadeOut")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int clearTitle(final CommandSourceStack source, final Collection<ServerPlayer> targets) {
        ClientboundClearTitlesPacket packet = new ClientboundClearTitlesPacket(false);

        for (ServerPlayer player : targets) {
            player.connection.send(packet);
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.title.cleared.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.title.cleared.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int resetTitle(final CommandSourceStack source, final Collection<ServerPlayer> targets) {
        ClientboundClearTitlesPacket packet = new ClientboundClearTitlesPacket(true);

        for (ServerPlayer player : targets) {
            player.connection.send(packet);
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.title.reset.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.title.reset.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int showTitle(
        final CommandSourceStack source,
        final Collection<ServerPlayer> targets,
        final Component title,
        final String type,
        final Function<Component, Packet<?>> factory
    ) throws CommandSyntaxException {
        for (ServerPlayer player : targets) {
            player.connection
                .send(factory.apply(ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(player).build(), title)));
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.title.show." + type + ".single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.title.show." + type + ".multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int setTimes(final CommandSourceStack source, final Collection<ServerPlayer> targets, final int fadeIn, final int stay, final int fadeOut) {
        ClientboundSetTitlesAnimationPacket packet = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);

        for (ServerPlayer player : targets) {
            player.connection.send(packet);
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.title.times.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.title.times.multiple", targets.size()), true);
        }

        return targets.size();
    }
}