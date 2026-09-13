package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list").executes(c -> listPlayers((CommandSourceStack)c.getSource())))
            .then(Commands.literal("uuids").executes(c -> listPlayersWithUuids((CommandSourceStack)c.getSource())))
      );
   }

   private static int listPlayers(final CommandSourceStack source) {
      return format(source, Player::getDisplayName);
   }

   private static int listPlayersWithUuids(final CommandSourceStack source) {
      return format(
         source, player -> Component.translatable("commands.list.nameAndId", player.getName(), Component.translationArg(player.getGameProfile().id()))
      );
   }

   private static int format(final CommandSourceStack source, final Function<ServerPlayer, Component> formatter) {
      PlayerList playerList = source.getServer().getPlayerList();
      List<ServerPlayer> players = playerList.getPlayers();
      Component listComponent = ComponentUtils.formatList(players, formatter);
      source.sendSuccess(() -> Component.translatable("commands.list.players", players.size(), playerList.getMaxPlayers(), listComponent), false);
      return players.size();
   }
}
