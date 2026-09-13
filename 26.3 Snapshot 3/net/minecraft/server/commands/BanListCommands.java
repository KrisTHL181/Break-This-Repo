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
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("banlist")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .executes(
                     s -> {
                        PlayerList players = ((CommandSourceStack)s.getSource()).getServer().getPlayerList();
                        return showList(
                           (CommandSourceStack)s.getSource(),
                           Lists.newArrayList(Iterables.concat(players.getBans().getEntries(), players.getIpBans().getEntries()))
                        );
                     }
                  ))
               .then(
                  Commands.literal("ips")
                     .executes(
                        s -> showList(
                           (CommandSourceStack)s.getSource(), ((CommandSourceStack)s.getSource()).getServer().getPlayerList().getIpBans().getEntries()
                        )
                     )
               ))
            .then(
               Commands.literal("players")
                  .executes(
                     s -> showList((CommandSourceStack)s.getSource(), ((CommandSourceStack)s.getSource()).getServer().getPlayerList().getBans().getEntries())
                  )
            )
      );
   }

   private static int showList(final CommandSourceStack source, final Collection<? extends BanListEntry<?>> list) {
      if (list.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.banlist.none"), false);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.banlist.list", list.size()), false);

         for (BanListEntry<?> entry : list) {
            source.sendSuccess(
               () -> Component.translatable("commands.banlist.entry", entry.getDisplayName(), entry.getSource(), entry.getReasonMessage()), false
            );
         }
      }

      return list.size();
   }
}
