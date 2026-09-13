package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
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
   private static final Dynamic2CommandExceptionType TOO_MANY_ITEMS = new Dynamic2CommandExceptionType(
      (maxCount, item) -> Component.translatableEscape("commands.give.failed.toomanyitems", maxCount, item)
   );
   private static final CommandResponseTracker.MessagesWithArgs<ServerPlayer, Integer, ItemStack> RESPONSE_GIVE = CommandResponseTracker.messages(
      (player, var1, count, prototypeItemStack) -> Component.translatable(
         "commands.give.success.single", count, prototypeItemStack.getDisplayName(), player.getDisplayName()
      ),
      (playerCount, var1, count, prototypeItemStack) -> Component.translatable(
         "commands.give.success.multiple", count, prototypeItemStack.getDisplayName(), playerCount
      )
   );
   public static final int MAX_ALLOWED_ITEMSTACKS = 100;

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("give").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(context))
                           .executes(
                              c -> giveItem((CommandSourceStack)c.getSource(), ItemArgument.getItem(c, "item"), EntityArgument.getPlayers(c, "targets"), 1)
                           ))
                        .then(
                           Commands.argument("count", IntegerArgumentType.integer(1))
                              .executes(
                                 c -> giveItem(
                                    (CommandSourceStack)c.getSource(),
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
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();
      ItemStack prototypeItemStack = input.createItemStack(1);
      int maxStackSize = prototypeItemStack.getMaxStackSize();
      int maxAllowedCount = maxStackSize * 100;
      if (count > maxAllowedCount) {
         throw TOO_MANY_ITEMS.create(maxAllowedCount, prototypeItemStack.getDisplayName());
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

         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, RESPONSE_GIVE, count, prototypeItemStack);
   }
}
