package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class SeedCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_138590_, boolean p_138591_) {
      p_138590_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("seed")
               .requires(Commands.hasPermission(p_138591_ ? Commands.LEVEL_GAMEMASTERS : Commands.LEVEL_ALL)))
            .executes(p_288608_ -> {
               long i = ((CommandSourceStack)p_288608_.getSource()).getLevel().getSeed();
               Component component = ComponentUtils.copyOnClickText(String.valueOf(i));
               ((CommandSourceStack)p_288608_.getSource()).sendSuccess(() -> Component.translatable("commands.seed.success", component), false);
               return (int)i;
            })
      );
   }
}
