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
import net.minecraft.server.players.UserBanList;

public class PardonCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardon.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("pardon").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getPlayerList().getBans().getUserList(), p)
                  )
                  .executes(c -> pardonPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles(c, "targets")))
            )
      );
   }

   private static int pardonPlayers(final CommandSourceStack source, final Collection<NameAndId> players) throws CommandSyntaxException {
      UserBanList list = source.getServer().getPlayerList().getBans();
      int count = 0;

      for (NameAndId player : players) {
         if (list.isBanned(player)) {
            list.remove(player);
            count++;
            source.sendSuccess(() -> Component.translatable("commands.pardon.success", Component.literal(player.name())), true);
         }
      }

      if (count == 0) {
         throw ERROR_NOT_BANNED.create();
      } else {
         return count;
      }
   }
}
