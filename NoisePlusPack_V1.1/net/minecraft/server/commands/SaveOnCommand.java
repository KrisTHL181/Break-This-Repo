package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SaveOnCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(Component.translatable("commands.save.alreadyOn"));

   public static void register(CommandDispatcher<CommandSourceStack> p_138293_) {
      p_138293_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-on").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
            .executes(p_421347_ -> {
               CommandSourceStack commandsourcestack = (CommandSourceStack)p_421347_.getSource();
               boolean flag = commandsourcestack.getServer().setAutoSave(true);
               if (!flag) {
                  throw ERROR_ALREADY_ON.create();
               }

               commandsourcestack.sendSuccess(() -> Component.translatable("commands.save.enabled"), true);
               return 1;
            })
      );
   }
}
