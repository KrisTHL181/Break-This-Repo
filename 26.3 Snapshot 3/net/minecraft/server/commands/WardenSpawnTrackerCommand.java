package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WardenSpawnTrackerCommand {
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SET_LEVEL = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.warden_spawn_tracker.set.success.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.warden_spawn_tracker.set.success.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_RESET = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.warden_spawn_tracker.clear.success.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.warden_spawn_tracker.clear.success.multiple", playerCount)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("warden_spawn_tracker")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("clear")
                     .executes(
                        c -> resetTracker((CommandSourceStack)c.getSource(), ImmutableList.of(((CommandSourceStack)c.getSource()).getPlayerOrException()))
                     )
               ))
            .then(
               Commands.literal("set")
                  .then(
                     Commands.argument("warning_level", IntegerArgumentType.integer(0, 4))
                        .executes(
                           c -> setWarningLevel(
                              (CommandSourceStack)c.getSource(),
                              ImmutableList.of(((CommandSourceStack)c.getSource()).getPlayerOrException()),
                              IntegerArgumentType.getInteger(c, "warning_level")
                           )
                        )
                  )
            )
      );
   }

   private static int setWarningLevel(final CommandSourceStack source, final Collection<ServerPlayer> players, final int warningLevel) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         player.getWardenSpawnTracker().setWarningLevel(warningLevel);
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, RESPONSE_SET_LEVEL);
   }

   private static int resetTracker(final CommandSourceStack source, final Collection<ServerPlayer> players) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         player.getWardenSpawnTracker().reset();
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, RESPONSE_RESET);
   }
}
