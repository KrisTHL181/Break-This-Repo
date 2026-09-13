package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.PlayerList;

public class BanListCommands {
   public static void register(CommandDispatcher<CommandSourceStack> p_136544_) {
      p_136544_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("banlist")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .executes(
                     p_421295_ -> {
                        PlayerList playerlist = ((CommandSourceStack)p_421295_.getSource()).getServer().getPlayerList();
                        return showList(
                           (CommandSourceStack)p_421295_.getSource(),
                           Lists.newArrayList(Iterables.concat(playerlist.getBans().getEntries(), playerlist.getIpBans().getEntries()))
                        );
                     }
                  ))
               .then(
                  Commands.literal("ips")
                     .executes(
                        p_421293_ -> showList(
                           (CommandSourceStack)p_421293_.getSource(),
                           ((CommandSourceStack)p_421293_.getSource()).getServer().getPlayerList().getIpBans().getEntries()
                        )
                     )
               ))
            .then(
               Commands.literal("players")
                  .executes(
                     p_421296_ -> showList(
                        (CommandSourceStack)p_421296_.getSource(),
                        ((CommandSourceStack)p_421296_.getSource()).getServer().getPlayerList().getBans().getEntries()
                     )
                  )
            )
      );
   }

   private static int showList(CommandSourceStack p_136550_, Collection<? extends BanListEntry<?>> p_136551_) {
      if (p_136551_.isEmpty()) {
         p_136550_.sendSuccess(() -> Component.translatable("commands.banlist.none"), false);
      } else {
         p_136550_.sendSuccess(() -> Component.translatable("commands.banlist.list", p_136551_.size()), false);

         for (BanListEntry<?> banlistentry : p_136551_) {
            p_136550_.sendSuccess(
               () -> Component.translatable("commands.banlist.entry", banlistentry.getDisplayName(), banlistentry.getSource(), banlistentry.getReasonMessage()),
               false
            );
         }
      }

      return p_136551_.size();
   }
}
