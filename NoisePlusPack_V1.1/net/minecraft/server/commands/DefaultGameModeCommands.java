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
   public static void register(CommandDispatcher<CommandSourceStack> p_136927_) {
      p_136927_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("defaultgamemode").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("gamemode", GameModeArgument.gameMode())
                  .executes(p_258227_ -> setMode((CommandSourceStack)p_258227_.getSource(), GameModeArgument.getGameMode(p_258227_, "gamemode")))
            )
      );
   }

   private static int setMode(CommandSourceStack p_136931_, GameType p_136932_) {
      MinecraftServer minecraftserver = p_136931_.getServer();
      minecraftserver.setDefaultGameType(p_136932_);
      int i = minecraftserver.enforceGameTypeForPlayers(minecraftserver.getForcedGameType());
      p_136931_.sendSuccess(() -> Component.translatable("commands.defaultgamemode.success", p_136932_.getLongDisplayName()), true);
      return i;
   }
}
