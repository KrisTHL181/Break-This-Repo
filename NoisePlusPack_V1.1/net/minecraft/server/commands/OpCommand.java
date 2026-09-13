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

   public static void register(CommandDispatcher<CommandSourceStack> p_138080_) {
      p_138080_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("op").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (p_138084_, p_138085_) -> {
                        PlayerList playerlist = ((CommandSourceStack)p_138084_.getSource()).getServer().getPlayerList();
                        return SharedSuggestionProvider.suggest(
                           playerlist.getPlayers()
                              .stream()
                              .filter(p_448971_ -> !playerlist.isOp(p_448971_.nameAndId()))
                              .map(p_448972_ -> p_448972_.getGameProfile().name()),
                           p_138085_
                        );
                     }
                  )
                  .executes(p_138082_ -> opPlayers((CommandSourceStack)p_138082_.getSource(), GameProfileArgument.getGameProfiles(p_138082_, "targets")))
            )
      );
   }

   private static int opPlayers(CommandSourceStack p_138089_, Collection<NameAndId> p_138090_) throws CommandSyntaxException {
      PlayerList playerlist = p_138089_.getServer().getPlayerList();
      int i = 0;

      for (NameAndId nameandid : p_138090_) {
         if (!playerlist.isOp(nameandid)) {
            playerlist.op(nameandid);
            i++;
            p_138089_.sendSuccess(() -> Component.translatable("commands.op.success", nameandid.name()), true);
         }
      }

      if (i == 0) {
         throw ERROR_ALREADY_OP.create();
      } else {
         return i;
      }
   }
}
