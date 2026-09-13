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

   public static void register(CommandDispatcher<CommandSourceStack> p_136889_) {
      p_136889_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("deop").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (p_136893_, p_136894_) -> SharedSuggestionProvider.suggest(
                        ((CommandSourceStack)p_136893_.getSource()).getServer().getPlayerList().getOpNames(), p_136894_
                     )
                  )
                  .executes(p_136891_ -> deopPlayers((CommandSourceStack)p_136891_.getSource(), GameProfileArgument.getGameProfiles(p_136891_, "targets")))
            )
      );
   }

   private static int deopPlayers(CommandSourceStack p_136898_, Collection<NameAndId> p_136899_) throws CommandSyntaxException {
      PlayerList playerlist = p_136898_.getServer().getPlayerList();
      int i = 0;

      for (NameAndId nameandid : p_136899_) {
         if (playerlist.isOp(nameandid)) {
            playerlist.deop(nameandid);
            i++;
            p_136898_.sendSuccess(() -> Component.translatable("commands.deop.success", p_136899_.iterator().next().name()), true);
         }
      }

      if (i == 0) {
         throw ERROR_NOT_OP.create();
      }

      p_136898_.getServer().kickUnlistedPlayers();
      return i;
   }
}
