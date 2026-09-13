package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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

   public static void register(CommandDispatcher<CommandSourceStack> p_214446_, CommandBuildContext p_214447_) {
      p_214446_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("give").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(p_214447_))
                           .executes(
                              p_137784_ -> giveItem(
                                 (CommandSourceStack)p_137784_.getSource(),
                                 ItemArgument.getItem(p_137784_, "item"),
                                 EntityArgument.getPlayers(p_137784_, "targets"),
                                 1
                              )
                           ))
                        .then(
                           Commands.argument("count", IntegerArgumentType.integer(1))
                              .executes(
                                 p_137775_ -> giveItem(
                                    (CommandSourceStack)p_137775_.getSource(),
                                    ItemArgument.getItem(p_137775_, "item"),
                                    EntityArgument.getPlayers(p_137775_, "targets"),
                                    IntegerArgumentType.getInteger(p_137775_, "count")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int giveItem(CommandSourceStack p_137779_, ItemInput p_137780_, Collection<ServerPlayer> p_137781_, int p_137782_) throws CommandSyntaxException {
      ItemStack itemstack = p_137780_.createItemStack(1, false);
      int i = itemstack.getMaxStackSize();
      int j = i * 100;
      if (p_137782_ > j) {
         p_137779_.sendFailure(Component.translatable("commands.give.failed.toomanyitems", j, itemstack.getDisplayName()));
         return 0;
      }

      for (ServerPlayer serverplayer : p_137781_) {
         int k = p_137782_;

         while (k > 0) {
            int l = Math.min(i, k);
            k -= l;
            ItemStack itemstack1 = p_137780_.createItemStack(l, false);
            boolean flag = serverplayer.getInventory().add(itemstack1);
            if (flag && itemstack1.isEmpty()) {
               ItemEntity itementity1 = serverplayer.drop(itemstack, false);
               if (itementity1 != null) {
                  itementity1.makeFakeItem();
               }

               serverplayer.level()
                  .playSound(
                     null,
                     serverplayer.getX(),
                     serverplayer.getY(),
                     serverplayer.getZ(),
                     SoundEvents.ITEM_PICKUP,
                     SoundSource.PLAYERS,
                     0.2F,
                     ((serverplayer.getRandom().nextFloat() - serverplayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                  );
               serverplayer.containerMenu.broadcastChanges();
            } else {
               ItemEntity itementity = serverplayer.drop(itemstack1, false);
               if (itementity != null) {
                  itementity.setNoPickUpDelay();
                  itementity.setTarget(serverplayer.getUUID());
               }
            }
         }
      }

      if (p_137781_.size() == 1) {
         p_137779_.sendSuccess(
            () -> Component.translatable("commands.give.success.single", p_137782_, itemstack.getDisplayName(), p_137781_.iterator().next().getDisplayName()),
            true
         );
      } else {
         p_137779_.sendSuccess(() -> Component.translatable("commands.give.success.single", p_137782_, itemstack.getDisplayName(), p_137781_.size()), true);
      }

      return p_137781_.size();
   }
}
