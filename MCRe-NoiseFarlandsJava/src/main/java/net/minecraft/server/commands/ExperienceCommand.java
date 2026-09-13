package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ExperienceCommand {
    private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID = new SimpleCommandExceptionType(
        Component.translatable("commands.experience.set.points.invalid")
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(
            Commands.literal("experience")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("target", EntityArgument.players())
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(
                                            c -> addExperience(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "target"),
                                                IntegerArgumentType.getInteger(c, "amount"),
                                                ExperienceCommand.Type.POINTS
                                            )
                                        )
                                        .then(
                                            Commands.literal("points")
                                                .executes(
                                                    c -> addExperience(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "target"),
                                                        IntegerArgumentType.getInteger(c, "amount"),
                                                        ExperienceCommand.Type.POINTS
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("levels")
                                                .executes(
                                                    c -> addExperience(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "target"),
                                                        IntegerArgumentType.getInteger(c, "amount"),
                                                        ExperienceCommand.Type.LEVELS
                                                    )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("set")
                        .then(
                            Commands.argument("target", EntityArgument.players())
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(
                                            c -> setExperience(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "target"),
                                                IntegerArgumentType.getInteger(c, "amount"),
                                                ExperienceCommand.Type.POINTS
                                            )
                                        )
                                        .then(
                                            Commands.literal("points")
                                                .executes(
                                                    c -> setExperience(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "target"),
                                                        IntegerArgumentType.getInteger(c, "amount"),
                                                        ExperienceCommand.Type.POINTS
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("levels")
                                                .executes(
                                                    c -> setExperience(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "target"),
                                                        IntegerArgumentType.getInteger(c, "amount"),
                                                        ExperienceCommand.Type.LEVELS
                                                    )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("query")
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .then(
                                    Commands.literal("points")
                                        .executes(c -> queryExperience(c.getSource(), EntityArgument.getPlayer(c, "target"), ExperienceCommand.Type.POINTS))
                                )
                                .then(
                                    Commands.literal("levels")
                                        .executes(c -> queryExperience(c.getSource(), EntityArgument.getPlayer(c, "target"), ExperienceCommand.Type.LEVELS))
                                )
                        )
                )
        );
        dispatcher.register(Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(command));
    }

    private static int queryExperience(final CommandSourceStack source, final ServerPlayer target, final ExperienceCommand.Type type) {
        int result = type.query.applyAsInt(target);
        source.sendSuccess(() -> Component.translatable("commands.experience.query." + type.name, target.getDisplayName(), result), false);
        return result;
    }

    private static int addExperience(
        final CommandSourceStack source, final Collection<? extends ServerPlayer> players, final int amount, final ExperienceCommand.Type type
    ) {
        for (ServerPlayer player : players) {
            type.add.accept(player, amount);
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.experience.add." + type.name + ".success.single", amount, players.iterator().next().getDisplayName()),
                true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.experience.add." + type.name + ".success.multiple", amount, players.size()), true);
        }

        return players.size();
    }

    private static int setExperience(
        final CommandSourceStack source, final Collection<? extends ServerPlayer> players, final int amount, final ExperienceCommand.Type type
    ) throws CommandSyntaxException {
        int success = 0;

        for (ServerPlayer player : players) {
            if (type.set.test(player, amount)) {
                success++;
            }
        }

        if (success == 0) {
            throw ERROR_SET_POINTS_INVALID.create();
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.experience.set." + type.name + ".success.single", amount, players.iterator().next().getDisplayName()),
                true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.experience.set." + type.name + ".success.multiple", amount, players.size()), true);
        }

        return players.size();
    }

    private enum Type {
        POINTS("points", Player::giveExperiencePoints, (p, a) -> {
            if (a >= p.getXpNeededForNextLevel()) {
                return false;
            }

            p.setExperiencePoints(a);
            return true;
        }, p -> Mth.floor(p.experienceProgress * p.getXpNeededForNextLevel())),
        LEVELS("levels", ServerPlayer::giveExperienceLevels, (p, a) -> {
            p.setExperienceLevels(a);
            return true;
        }, p -> p.experienceLevel);

        public final BiConsumer<ServerPlayer, Integer> add;
        public final BiPredicate<ServerPlayer, Integer> set;
        public final String name;
        private final ToIntFunction<ServerPlayer> query;

        Type(
            final String name,
            final BiConsumer<ServerPlayer, Integer> add,
            final BiPredicate<ServerPlayer, Integer> set,
            final ToIntFunction<ServerPlayer> query
        ) {
            this.add = add;
            this.name = name;
            this.set = set;
            this.query = query;
        }
    }
}