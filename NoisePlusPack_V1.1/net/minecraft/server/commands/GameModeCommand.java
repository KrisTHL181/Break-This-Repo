package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

public class GameModeCommand {
   public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

   public static void register(CommandDispatcher<CommandSourceStack> p_137730_) {
      p_137730_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("gamemode").requires(Commands.hasPermission(PERMISSION_CHECK)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode())
                     .executes(
                        p_258228_ -> setMode(
                           p_258228_,
                           Collections.singleton(((CommandSourceStack)p_258228_.getSource()).getPlayerOrException()),
                           GameModeArgument.getGameMode(p_258228_, "gamemode")
                        )
                     ))
                  .then(
                     Commands.argument("target", EntityArgument.players())
                        .executes(
                           p_258229_ -> setMode(p_258229_, EntityArgument.getPlayers(p_258229_, "target"), GameModeArgument.getGameMode(p_258229_, "gamemode"))
                        )
                  )
            )
      );
   }

   private static void logGamemodeChange(CommandSourceStack p_137738_, ServerPlayer p_137739_, GameType p_137740_) {
      Component component = Component.translatable("gameMode." + p_137740_.getName());
      if (p_137738_.getEntity() == p_137739_) {
         p_137738_.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", component), true);
      } else {
         if (p_137738_.getLevel().getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK)) {
            p_137739_.sendSystemMessage(Component.translatable("gameMode.changed", component));
         }

         p_137738_.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", p_137739_.getDisplayName(), component), true);
      }
   }

   private static int setMode(CommandContext<CommandSourceStack> p_137732_, Collection<ServerPlayer> p_137733_, GameType p_137734_) {
      int i = 0;

      for (ServerPlayer serverplayer : p_137733_) {
         if (setGameMode((CommandSourceStack)p_137732_.getSource(), serverplayer, p_137734_)) {
            i++;
         }
      }

      return i;
   }

   public static void setGameMode(ServerPlayer p_407866_, GameType p_407588_) {
      setGameMode(p_407866_.createCommandSourceStack(), p_407866_, p_407588_);
   }

   private static boolean setGameMode(CommandSourceStack p_409618_, ServerPlayer p_409736_, GameType p_407947_) {
      if (p_409736_.setGameMode(p_407947_)) {
         logGamemodeChange(p_409618_, p_409736_, p_407947_);
         return true;
      } else {
         return false;
      }
   }
}
