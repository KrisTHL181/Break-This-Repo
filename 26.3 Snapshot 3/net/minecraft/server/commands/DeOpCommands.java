package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

public class DeOpCommands {
   private static final SimpleCommandExceptionType ERROR_NOT_OP = new SimpleCommandExceptionType(Component.translatable("commands.deop.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("deop").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests((c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getPlayerList().getOpNames(), p))
                  .executes(c -> deopPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles(c, "targets")))
            )
      );
   }

   private static int deopPlayers(final CommandSourceStack source, final Collection<NameAndId> players) throws CommandSyntaxException {
      PlayerList list = source.getServer().getPlayerList();
      int count = 0;

      for (NameAndId player : players) {
         if (list.isOp(player)) {
            list.deop(player);
            count++;
            source.sendSuccess(() -> Component.translatable("commands.deop.success", player.name()), true);
         }
      }

      if (count == 0) {
         throw ERROR_NOT_OP.create();
      }

      source.getServer().kickUnlistedPlayers();
      return count;
   }
}
