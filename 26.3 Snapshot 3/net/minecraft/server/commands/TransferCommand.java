package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;

public class TransferCommand {
   private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(
      Component.translatable("commands.transfer.error.no_players")
   );
   private static final CommandResponseTracker.MessagesWithArgs<ServerPlayer, String, Integer> RESPONSE_TRANSFER = CommandResponseTracker.messages(
      ERROR_NO_PLAYERS,
      (player, var1, hostname, port) -> Component.translatable("commands.transfer.success.single", player.getDisplayName(), hostname, port),
      (playerCount, totalValue, hostname, port) -> Component.translatable("commands.transfer.success.multiple", playerCount, hostname, port)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("transfer").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("hostname", StringArgumentType.string())
                     .executes(
                        c -> transfer(
                           (CommandSourceStack)c.getSource(),
                           StringArgumentType.getString(c, "hostname"),
                           25565,
                           List.of(((CommandSourceStack)c.getSource()).getPlayerOrException())
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                           .executes(
                              c -> transfer(
                                 (CommandSourceStack)c.getSource(),
                                 StringArgumentType.getString(c, "hostname"),
                                 IntegerArgumentType.getInteger(c, "port"),
                                 List.of(((CommandSourceStack)c.getSource()).getPlayerOrException())
                              )
                           ))
                        .then(
                           Commands.argument("players", EntityArgument.players())
                              .executes(
                                 c -> transfer(
                                    (CommandSourceStack)c.getSource(),
                                    StringArgumentType.getString(c, "hostname"),
                                    IntegerArgumentType.getInteger(c, "port"),
                                    EntityArgument.getPlayers(c, "players")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int transfer(final CommandSourceStack source, final String hostname, final int port, final Collection<ServerPlayer> players) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         player.connection.send(new ClientboundTransferPacket(hostname, port));
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, RESPONSE_TRANSFER, hostname, port);
   }
}
