package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class DebugPathCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType(Component.literal("Source is not a mob"));
   private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType(Component.literal("Path not found"));
   private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.literal("Target not reached"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugpath").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("to", BlockPosArgument.blockPos())
                  .executes(c -> fillBlocks((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "to")))
            )
      );
   }

   private static int fillBlocks(final CommandSourceStack source, final BlockPos target) throws CommandSyntaxException {
      if (source.getEntity() instanceof Mob mob) {
         PathNavigation pathNavigation = new GroundPathNavigation(mob, source.getLevel());
         Path path = pathNavigation.createPath(target, 0);
         if (path == null) {
            throw ERROR_NO_PATH.create();
         }

         if (!path.canReach()) {
            throw ERROR_NOT_COMPLETE.create();
         }

         source.sendSuccess(() -> Component.literal("Made path"), true);
         return 1;
      } else {
         throw ERROR_NOT_MOB.create();
      }
   }
}
