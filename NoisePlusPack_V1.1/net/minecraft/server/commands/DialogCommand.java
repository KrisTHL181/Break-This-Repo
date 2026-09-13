package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_410179_, CommandBuildContext p_408009_) {
      p_410179_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("dialog")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("show")
                     .then(
                        Commands.argument("targets", EntityArgument.players())
                           .then(
                              Commands.argument("dialog", ResourceOrIdArgument.dialog(p_408009_))
                                 .executes(
                                    p_409348_ -> showDialog(
                                       (CommandSourceStack)p_409348_.getSource(),
                                       EntityArgument.getPlayers(p_409348_, "targets"),
                                       ResourceOrIdArgument.getDialog(p_409348_, "dialog")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .executes(p_408723_ -> clearDialog((CommandSourceStack)p_408723_.getSource(), EntityArgument.getPlayers(p_408723_, "targets")))
                  )
            )
      );
   }

   private static int showDialog(CommandSourceStack p_408221_, Collection<ServerPlayer> p_410671_, Holder<Dialog> p_408690_) {
      for (ServerPlayer serverplayer : p_410671_) {
         serverplayer.openDialog(p_408690_);
      }

      if (p_410671_.size() == 1) {
         p_408221_.sendSuccess(() -> Component.translatable("commands.dialog.show.single", p_410671_.iterator().next().getDisplayName()), true);
      } else {
         p_408221_.sendSuccess(() -> Component.translatable("commands.dialog.show.multiple", p_410671_.size()), true);
      }

      return p_410671_.size();
   }

   private static int clearDialog(CommandSourceStack p_410567_, Collection<ServerPlayer> p_406347_) {
      for (ServerPlayer serverplayer : p_406347_) {
         serverplayer.connection.send(ClientboundClearDialogPacket.INSTANCE);
      }

      if (p_406347_.size() == 1) {
         p_410567_.sendSuccess(() -> Component.translatable("commands.dialog.clear.single", p_406347_.iterator().next().getDisplayName()), true);
      } else {
         p_410567_.sendSuccess(() -> Component.translatable("commands.dialog.clear.multiple", p_406347_.size()), true);
      }

      return p_406347_.size();
   }
}
