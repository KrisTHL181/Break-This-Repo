package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SetPlayerIdleTimeoutCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_138635_) {
      p_138635_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setidletimeout").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("minutes", IntegerArgumentType.integer(0))
                  .executes(p_138637_ -> setIdleTimeout((CommandSourceStack)p_138637_.getSource(), IntegerArgumentType.getInteger(p_138637_, "minutes")))
            )
      );
   }

   private static int setIdleTimeout(CommandSourceStack p_138641_, int p_138642_) {
      p_138641_.getServer().setPlayerIdleTimeout(p_138642_);
      if (p_138642_ > 0) {
         p_138641_.sendSuccess(() -> Component.translatable("commands.setidletimeout.success", p_138642_), true);
      } else {
         p_138641_.sendSuccess(() -> Component.translatable("commands.setidletimeout.success.disabled"), true);
      }

      return p_138642_;
   }
}
