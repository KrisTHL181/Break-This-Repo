package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KickCommand {
   private static final SimpleCommandExceptionType ERROR_KICKING_OWNER = new SimpleCommandExceptionType(Component.translatable("commands.kick.owner.failed"));
   private static final SimpleCommandExceptionType ERROR_SINGLEPLAYER = new SimpleCommandExceptionType(
      Component.translatable("commands.kick.singleplayer.failed")
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("kick").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        c -> kickPlayers(
                           (CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), Component.translatable("multiplayer.disconnect.kicked")
                        )
                     ))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           c -> kickPlayers((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), MessageArgument.getMessage(c, "reason"))
                        )
                  )
            )
      );
   }

   private static int kickPlayers(final CommandSourceStack source, final Collection<ServerPlayer> players, final Component reason) throws CommandSyntaxException {
      if (!source.getServer().isPublished()) {
         throw ERROR_SINGLEPLAYER.create();
      }

      int count = 0;

      for (ServerPlayer player : players) {
         if (!source.getServer().isSingleplayerOwner(player.nameAndId())) {
            player.connection.disconnect(reason);
            source.sendSuccess(() -> Component.translatable("commands.kick.success", player.getDisplayName(), reason), true);
            count++;
         }
      }

      if (count == 0) {
         throw ERROR_KICKING_OWNER.create();
      } else {
         return count;
      }
   }
}
