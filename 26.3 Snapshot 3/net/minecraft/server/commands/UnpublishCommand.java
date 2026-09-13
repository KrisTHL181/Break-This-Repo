package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnpublishCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_PUBLISHED = new SimpleCommandExceptionType(
      Component.translatable("commands.unpublish.notPublished")
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("unpublish").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
            .executes(c -> unpublish((CommandSourceStack)c.getSource()))
      );
   }

   private static int unpublish(final CommandSourceStack source) throws CommandSyntaxException {
      if (!source.getServer().unpublishServer()) {
         throw ERROR_NOT_PUBLISHED.create();
      }

      source.sendSuccess(() -> Component.translatable("commands.unpublish.success"), true);
      return 1;
   }
}
