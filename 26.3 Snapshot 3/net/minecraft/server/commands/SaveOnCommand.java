package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SaveOnCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(Component.translatable("commands.save.alreadyOn"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-on").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).executes(c -> {
            CommandSourceStack source = (CommandSourceStack)c.getSource();
            boolean success = source.getServer().setAutoSave(true);
            if (!success) {
               throw ERROR_ALREADY_ON.create();
            }

            source.sendSuccess(() -> Component.translatable("commands.save.enabled"), true);
            return 1;
         })
      );
   }
}
