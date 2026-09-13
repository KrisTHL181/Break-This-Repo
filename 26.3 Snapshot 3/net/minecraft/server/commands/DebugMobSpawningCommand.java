package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class DebugMobSpawningCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> base = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("debugmobspawning")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

      for (MobCategory mobCategory : MobCategory.values()) {
         base.then(
            Commands.literal(mobCategory.getName())
               .then(
                  Commands.argument("at", BlockPosArgument.blockPos())
                     .executes(c -> spawnMobs((CommandSourceStack)c.getSource(), mobCategory, BlockPosArgument.getLoadedBlockPos(c, "at")))
               )
         );
      }

      dispatcher.register(base);
   }

   private static int spawnMobs(final CommandSourceStack source, final MobCategory mobCategory, final BlockPos at) {
      NaturalSpawner.spawnCategoryForPosition(mobCategory, source.getLevel(), at);
      return 1;
   }
}
