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
   private static final DynamicCommandExceptionType ERROR_ALREADY_SAME_DIFFICULTY = new DynamicCommandExceptionType(
      difficulty -> Component.translatableEscape("commands.difficulty.failure", difficulty)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("difficulty");

      for (Difficulty difficulty : Difficulty.values()) {
         command.then(Commands.literal(difficulty.getSerializedName()).executes(c -> setDifficulty((CommandSourceStack)c.getSource(), difficulty)));
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)command.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> {
            Difficulty difficultyx = ((CommandSourceStack)c.getSource()).getLevel().getDifficulty();
            ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.translatable("commands.difficulty.query", difficultyx.getDisplayName()), false);
            return difficultyx.getId();
         })
      );
   }

   public static int setDifficulty(final CommandSourceStack source, final Difficulty difficulty) throws CommandSyntaxException {
      MinecraftServer server = source.getServer();
      if (server.getWorldData().getDifficulty() == difficulty) {
         throw ERROR_ALREADY_SAME_DIFFICULTY.create(difficulty.getDisplayName());
      }

      server.setDifficulty(difficulty, true);
      source.sendSuccess(() -> Component.translatable("commands.difficulty.success", difficulty.getDisplayName()), true);
      return 0;
   }
}
