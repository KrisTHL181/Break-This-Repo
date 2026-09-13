package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SaveOffCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_OFF = new SimpleCommandExceptionType(Component.translatable("commands.save.alreadyOff"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-off").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).executes(c -> {
            CommandSourceStack source = (CommandSourceStack)c.getSource();
            boolean success = source.getServer().setAutoSave(false);
            if (!success) {
               throw ERROR_ALREADY_OFF.create();
            }

            source.sendSuccess(() -> Component.translatable("commands.save.disabled"), true);
            return 1;
         })
      );
   }
}
