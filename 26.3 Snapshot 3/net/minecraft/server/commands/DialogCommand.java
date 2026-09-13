package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SHOW = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.dialog.show.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.dialog.show.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_CLEAR = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.dialog.clear.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.dialog.clear.multiple", playerCount)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("dialog")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("show")
                     .then(
                        Commands.argument("targets", EntityArgument.players())
                           .then(
                              Commands.argument("dialog", ResourceOrIdArgument.dialog(context))
                                 .executes(
                                    c -> showDialog(
                                       (CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), ResourceOrIdArgument.getDialog(c, "dialog")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .executes(c -> clearDialog((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                  )
            )
      );
   }

   private static int showDialog(final CommandSourceStack sender, final Collection<ServerPlayer> targets, final Holder<Dialog> dialog) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer target : targets) {
         target.openDialog(dialog);
         tracker.track(target);
      }

      return tracker.sendFeedback(sender, true, RESPONSE_SHOW);
   }

   private static int clearDialog(final CommandSourceStack sender, final Collection<ServerPlayer> targets) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer target : targets) {
         target.connection.send(ClientboundClearDialogPacket.INSTANCE);
         tracker.track(target);
      }

      return tracker.sendFeedback(sender, true, RESPONSE_CLEAR);
   }
}
