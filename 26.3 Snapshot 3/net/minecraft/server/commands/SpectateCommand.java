package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class SpectateCommand {
   private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(Component.translatable("commands.spectate.self"));
   private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType(
      s -> Component.translatableEscape("commands.spectate.not_spectator", s)
   );
   private static final DynamicCommandExceptionType ERROR_CANNOT_SPECTATE = new DynamicCommandExceptionType(
      s -> Component.translatableEscape("commands.spectate.cannot_spectate", s)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spectate")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(c -> spectate((CommandSourceStack)c.getSource(), null, ((CommandSourceStack)c.getSource()).getPlayerOrException())))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .executes(
                        c -> spectate(
                           (CommandSourceStack)c.getSource(), EntityArgument.getEntity(c, "target"), ((CommandSourceStack)c.getSource()).getPlayerOrException()
                        )
                     ))
                  .then(
                     Commands.argument("player", EntityArgument.player())
                        .executes(
                           c -> spectate((CommandSourceStack)c.getSource(), EntityArgument.getEntity(c, "target"), EntityArgument.getPlayer(c, "player"))
                        )
                  )
            )
      );
   }

   private static int spectate(final CommandSourceStack source, final @Nullable Entity target, final ServerPlayer player) throws CommandSyntaxException {
      if (player == target) {
         throw ERROR_SELF.create();
      }

      if (!player.isSpectator()) {
         throw ERROR_NOT_SPECTATOR.create(player.getDisplayName());
      }

      if (target != null && target.getType().clientTrackingRange() == 0) {
         throw ERROR_CANNOT_SPECTATE.create(target.getDisplayName());
      }

      player.setCamera(target);
      if (target != null) {
         source.sendSuccess(() -> Component.translatable("commands.spectate.success.started", target.getDisplayName()), false);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.spectate.success.stopped"), false);
      }

      return 1;
   }
}
