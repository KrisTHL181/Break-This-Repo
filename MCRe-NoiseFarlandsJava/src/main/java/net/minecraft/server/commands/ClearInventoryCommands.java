package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
    private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(
        name -> Component.translatableEscape("clear.failed.single", name)
    );
    private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(
        count -> Component.translatableEscape("clear.failed.multiple", count)
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("clear")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> clearUnlimited(c.getSource(), Collections.singleton(c.getSource().getPlayerOrException()), i -> true))
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .executes(c -> clearUnlimited(c.getSource(), EntityArgument.getPlayers(c, "targets"), i -> true))
                        .then(
                            Commands.argument("item", ItemPredicateArgument.itemPredicate(context))
                                .executes(
                                    c -> clearUnlimited(
                                        c.getSource(), EntityArgument.getPlayers(c, "targets"), ItemPredicateArgument.getItemPredicate(c, "item")
                                    )
                                )
                                .then(
                                    Commands.argument("maxCount", IntegerArgumentType.integer(0))
                                        .executes(
                                            c -> clearInventory(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                ItemPredicateArgument.getItemPredicate(c, "item"),
                                                IntegerArgumentType.getInteger(c, "maxCount")
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int clearUnlimited(final CommandSourceStack source, final Collection<ServerPlayer> players, final Predicate<ItemStack> predicate) throws CommandSyntaxException {
        return clearInventory(source, players, predicate, -1);
    }

    private static int clearInventory(
        final CommandSourceStack source, final Collection<ServerPlayer> players, final Predicate<ItemStack> predicate, final int maxCount
    ) throws CommandSyntaxException {
        int count = 0;

        for (ServerPlayer player : players) {
            count += player.getInventory().clearOrCountMatchingItems(predicate, maxCount, player.inventoryMenu.getCraftSlots());
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
        }

        if (count == 0) {
            if (players.size() == 1) {
                throw ERROR_SINGLE.create(players.iterator().next().getName());
            } else {
                throw ERROR_MULTIPLE.create(players.size());
            }
        } else {
            int finalCount = count;
            if (maxCount == 0) {
                if (players.size() == 1) {
                    source.sendSuccess(() -> Component.translatable("commands.clear.test.single", finalCount, players.iterator().next().getDisplayName()), true);
                } else {
                    source.sendSuccess(() -> Component.translatable("commands.clear.test.multiple", finalCount, players.size()), true);
                }
            } else if (players.size() == 1) {
                source.sendSuccess(() -> Component.translatable("commands.clear.success.single", finalCount, players.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.clear.success.multiple", finalCount, players.size()), true);
            }

            return count;
        }
    }
}