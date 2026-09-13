package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class SaveAllCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.save.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-all")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .executes(c -> saveAll((CommandSourceStack)c.getSource(), false)))
            .then(Commands.literal("flush").executes(c -> saveAll((CommandSourceStack)c.getSource(), true)))
      );
   }

   private static int saveAll(final CommandSourceStack source, final boolean flush) throws CommandSyntaxException {
      source.sendSuccess(() -> Component.translatable("commands.save.saving"), false);
      MinecraftServer server = source.getServer();
      boolean success = server.saveEverything(true, flush, true);
      if (!success) {
         throw ERROR_FAILED.create();
      }

      source.sendSuccess(() -> Component.translatable("commands.save.success"), true);
      return 1;
   }
}
