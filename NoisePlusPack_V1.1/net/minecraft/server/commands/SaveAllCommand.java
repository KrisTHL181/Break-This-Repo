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

   public static void register(CommandDispatcher<CommandSourceStack> p_138272_) {
      p_138272_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-all")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .executes(p_138281_ -> saveAll((CommandSourceStack)p_138281_.getSource(), false)))
            .then(Commands.literal("flush").executes(p_138274_ -> saveAll((CommandSourceStack)p_138274_.getSource(), true)))
      );
   }

   private static int saveAll(CommandSourceStack p_138278_, boolean p_138279_) throws CommandSyntaxException {
      p_138278_.sendSuccess(() -> Component.translatable("commands.save.saving"), false);
      MinecraftServer minecraftserver = p_138278_.getServer();
      boolean flag = minecraftserver.saveEverything(true, p_138279_, true);
      if (!flag) {
         throw ERROR_FAILED.create();
      }

      p_138278_.sendSuccess(() -> Component.translatable("commands.save.success"), true);
      return 1;
   }
}
