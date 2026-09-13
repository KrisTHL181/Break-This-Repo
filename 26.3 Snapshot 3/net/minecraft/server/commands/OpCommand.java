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

public class OpCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("op").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (c, p) -> {
                        PlayerList list = ((CommandSourceStack)c.getSource()).getServer().getPlayerList();
                        return SharedSuggestionProvider.suggest(
                           list.getPlayers().stream().filter(player -> !list.isOp(player.nameAndId())).map(pl -> pl.getGameProfile().name()), p
                        );
                     }
                  )
                  .executes(c -> opPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles(c, "targets")))
            )
      );
   }

   private static int opPlayers(final CommandSourceStack source, final Collection<NameAndId> players) throws CommandSyntaxException {
      PlayerList list = source.getServer().getPlayerList();
      int count = 0;

      for (NameAndId player : players) {
         if (!list.isOp(player)) {
            list.op(player);
            count++;
            source.sendSuccess(() -> Component.translatable("commands.op.success", player.name()), true);
         }
      }

      if (count == 0) {
         throw ERROR_ALREADY_OP.create();
      } else {
         return count;
      }
   }
}
