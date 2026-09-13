package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("defaultgamemode").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("gamemode", GameModeArgument.gameMode())
                  .executes(c -> setMode((CommandSourceStack)c.getSource(), GameModeArgument.getGameMode(c, "gamemode")))
            )
      );
   }

   private static int setMode(final CommandSourceStack source, final GameType type) {
      MinecraftServer server = source.getServer();
      server.setDefaultGameType(type);
      int count = server.enforceGameTypeForPlayers(server.getForcedGameType());
      source.sendSuccess(() -> Component.translatable("commands.defaultgamemode.success", type.getLongDisplayName()), true);
      return count;
   }
}
