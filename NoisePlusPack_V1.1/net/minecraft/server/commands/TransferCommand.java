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

   public static void register(CommandDispatcher<CommandSourceStack> p_331355_) {
      p_331355_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("transfer").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("hostname", StringArgumentType.string())
                     .executes(
                        p_328093_ -> transfer(
                           (CommandSourceStack)p_328093_.getSource(),
                           StringArgumentType.getString(p_328093_, "hostname"),
                           25565,
                           List.of(((CommandSourceStack)p_328093_.getSource()).getPlayerOrException())
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                           .executes(
                              p_331985_ -> transfer(
                                 (CommandSourceStack)p_331985_.getSource(),
                                 StringArgumentType.getString(p_331985_, "hostname"),
                                 IntegerArgumentType.getInteger(p_331985_, "port"),
                                 List.of(((CommandSourceStack)p_331985_.getSource()).getPlayerOrException())
                              )
                           ))
                        .then(
                           Commands.argument("players", EntityArgument.players())
                              .executes(
                                 p_327688_ -> transfer(
                                    (CommandSourceStack)p_327688_.getSource(),
                                    StringArgumentType.getString(p_327688_, "hostname"),
                                    IntegerArgumentType.getInteger(p_327688_, "port"),
                                    EntityArgument.getPlayers(p_327688_, "players")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int transfer(CommandSourceStack p_328615_, String p_328133_, int p_328113_, Collection<ServerPlayer> p_331356_) throws CommandSyntaxException {
      if (p_331356_.isEmpty()) {
         throw ERROR_NO_PLAYERS.create();
      }

      for (ServerPlayer serverplayer : p_331356_) {
         serverplayer.connection.send(new ClientboundTransferPacket(p_328133_, p_328113_));
      }

      if (p_331356_.size() == 1) {
         p_328615_.sendSuccess(
            () -> Component.translatable("commands.transfer.success.single", p_331356_.iterator().next().getDisplayName(), p_328133_, p_328113_), true
         );
      } else {
         p_328615_.sendSuccess(() -> Component.translatable("commands.transfer.success.multiple", p_331356_.size(), p_328133_, p_328113_), true);
      }

      return p_331356_.size();
   }
}
