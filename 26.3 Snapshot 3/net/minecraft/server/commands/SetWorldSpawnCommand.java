package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetWorldSpawnCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setworldspawn")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  c -> setSpawn(
                     (CommandSourceStack)c.getSource(), BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition()), WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                     .executes(c -> setSpawn((CommandSourceStack)c.getSource(), BlockPosArgument.getSpawnablePos(c, "pos"), WorldCoordinates.ZERO_ROTATION)))
                  .then(
                     Commands.argument("rotation", RotationArgument.rotation())
                        .executes(
                           c -> setSpawn(
                              (CommandSourceStack)c.getSource(), BlockPosArgument.getSpawnablePos(c, "pos"), RotationArgument.getRotation(c, "rotation")
                           )
                        )
                  )
            )
      );
   }

   private static int setSpawn(final CommandSourceStack source, final BlockPos pos, final Coordinates rotation) {
      ServerLevel level = source.getLevel();
      Vec2 rotationVector = rotation.getRotation(source);
      float yaw = rotationVector.y;
      float pitch = rotationVector.x;
      LevelData.RespawnData respawnData = LevelData.RespawnData.of(level.dimension(), pos, yaw, pitch);
      level.setRespawnData(respawnData);
      source.sendSuccess(
         () -> Component.translatable(
            "commands.setworldspawn.success",
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            respawnData.yaw(),
            respawnData.pitch(),
            level.dimension().identifier().toString()
         ),
         true
      );
      return 1;
   }
}
