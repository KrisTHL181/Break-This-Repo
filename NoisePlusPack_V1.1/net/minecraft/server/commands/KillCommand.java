package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class KillCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_137808_) {
      p_137808_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("kill")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  p_137817_ -> kill(
                     (CommandSourceStack)p_137817_.getSource(), ImmutableList.of(((CommandSourceStack)p_137817_.getSource()).getEntityOrException())
                  )
               ))
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .executes(p_137810_ -> kill((CommandSourceStack)p_137810_.getSource(), EntityArgument.getEntities(p_137810_, "targets")))
            )
      );
   }

   private static int kill(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_) {
      for (Entity entity : p_137815_) {
         entity.kill(p_137814_.getLevel());
      }

      if (p_137815_.size() == 1) {
         p_137814_.sendSuccess(() -> Component.translatable("commands.kill.success.single", p_137815_.iterator().next().getDisplayName()), true);
      } else {
         p_137814_.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", p_137815_.size()), true);
      }

      return p_137815_.size();
   }
}
