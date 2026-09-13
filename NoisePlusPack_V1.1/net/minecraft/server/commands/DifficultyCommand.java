package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType(
      p_308648_ -> Component.translatableEscape("commands.difficulty.failure", p_308648_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_136939_) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("difficulty");

      for (Difficulty difficulty : Difficulty.values()) {
         literalargumentbuilder.then(
            Commands.literal(difficulty.getKey()).executes(p_136937_ -> setDifficulty((CommandSourceStack)p_136937_.getSource(), difficulty))
         );
      }

      p_136939_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)literalargumentbuilder.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .executes(
               p_448903_ -> {
                  Difficulty difficulty1 = ((CommandSourceStack)p_448903_.getSource()).getLevel().getDifficulty();
                  ((CommandSourceStack)p_448903_.getSource())
                     .sendSuccess(() -> Component.translatable("commands.difficulty.query", difficulty1.getDisplayName()), false);
                  return difficulty1.getId();
               }
            )
      );
   }

   public static int setDifficulty(CommandSourceStack p_136945_, Difficulty p_136946_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_136945_.getServer();
      if (minecraftserver.getWorldData().getDifficulty() == p_136946_) {
         throw ERROR_ALREADY_DIFFICULT.create(p_136946_.getKey());
      }

      minecraftserver.setDifficulty(p_136946_, true);
      p_136945_.sendSuccess(() -> Component.translatable("commands.difficulty.success", p_136946_.getDisplayName()), true);
      return 0;
   }
}
