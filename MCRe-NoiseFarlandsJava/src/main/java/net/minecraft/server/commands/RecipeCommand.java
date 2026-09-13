package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeCommand {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.give.failed"));
    private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.take.failed"));

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("recipe")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("give")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                        .executes(
                                            c -> giveRecipes(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                Collections.singleton(ResourceKeyArgument.getRecipe(c, "recipe"))
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("*")
                                        .executes(
                                            c -> giveRecipes(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                c.getSource().getServer().getRecipeManager().getRecipes()
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("take")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                        .executes(
                                            c -> takeRecipes(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                Collections.singleton(ResourceKeyArgument.getRecipe(c, "recipe"))
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("*")
                                        .executes(
                                            c -> takeRecipes(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                c.getSource().getServer().getRecipeManager().getRecipes()
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int giveRecipes(final CommandSourceStack source, final Collection<ServerPlayer> players, final Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException {
        int success = 0;

        for (ServerPlayer player : players) {
            success += player.awardRecipes(recipes);
        }

        if (success == 0) {
            throw ERROR_GIVE_FAILED.create();
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.recipe.give.success.single", recipes.size(), players.iterator().next().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.recipe.give.success.multiple", recipes.size(), players.size()), true);
        }

        return success;
    }

    private static int takeRecipes(final CommandSourceStack source, final Collection<ServerPlayer> players, final Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException {
        int success = 0;

        for (ServerPlayer player : players) {
            success += player.resetRecipes(recipes);
        }

        if (success == 0) {
            throw ERROR_TAKE_FAILED.create();
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.recipe.take.success.single", recipes.size(), players.iterator().next().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.recipe.take.success.multiple", recipes.size(), players.size()), true);
        }

        return success;
    }
}