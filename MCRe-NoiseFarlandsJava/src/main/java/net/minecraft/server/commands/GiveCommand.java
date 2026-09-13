package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class GiveCommand {
    public static final int MAX_ALLOWED_ITEMSTACKS = 100;

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("give")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .then(
                            Commands.argument("item", ItemArgument.item(context))
                                .executes(c -> giveItem(c.getSource(), ItemArgument.getItem(c, "item"), EntityArgument.getPlayers(c, "targets"), 1))
                                .then(
                                    Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(
                                            c -> giveItem(
                                                c.getSource(),
                                                ItemArgument.getItem(c, "item"),
                                                EntityArgument.getPlayers(c, "targets"),
                                                IntegerArgumentType.getInteger(c, "count")
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int giveItem(final CommandSourceStack source, final ItemInput input, final Collection<ServerPlayer> players, final int count) throws CommandSyntaxException {
        ItemStack prototypeItemStack = input.createItemStack(1);
        int maxStackSize = prototypeItemStack.getMaxStackSize();
        int maxAllowedCount = maxStackSize * 100;
        if (count > maxAllowedCount) {
            source.sendFailure(Component.translatable("commands.give.failed.toomanyitems", maxAllowedCount, prototypeItemStack.getDisplayName()));
            return 0;
        }

        for (ServerPlayer player : players) {
            int remaining = count;

            while (remaining > 0) {
                int size = Math.min(maxStackSize, remaining);
                remaining -= size;
                ItemStack copyToDrop = prototypeItemStack.copyWithCount(size);
                boolean added = player.getInventory().add(copyToDrop);
                if (added && copyToDrop.isEmpty()) {
                    ItemEntity drop = player.drop(prototypeItemStack.copy(), false);
                    if (drop != null) {
                        drop.makeFakeItem();
                    }

                    player.level()
                        .playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ITEM_PICKUP,
                            SoundSource.PLAYERS,
                            0.2F,
                            ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                        );
                    player.containerMenu.broadcastChanges();
                } else {
                    ItemEntity drop = player.drop(copyToDrop, false);
                    if (drop != null) {
                        drop.setNoPickUpDelay();
                        drop.setTarget(player.getUUID());
                    }
                }
            }
        }

        if (players.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.give.success.single", count, prototypeItemStack.getDisplayName(), players.iterator().next().getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.give.success.single", count, prototypeItemStack.getDisplayName(), players.size()), true);
        }

        return players.size();
    }
}