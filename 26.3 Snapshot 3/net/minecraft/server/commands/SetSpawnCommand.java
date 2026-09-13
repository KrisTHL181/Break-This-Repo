package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetSpawnCommand {
   private static final CommandResponseTracker.MessagesWithArg<ServerPlayer, LevelData.RespawnData> RESPONSE_SET = CommandResponseTracker.messages(
      (player, var1, respawnData) -> Component.translatable(
         "commands.spawnpoint.success.single",
         respawnData.pos().getX(),
         respawnData.pos().getY(),
         respawnData.pos().getZ(),
         respawnData.yaw(),
         respawnData.pitch(),
         Component.translationArg(respawnData.dimension().identifier()),
         player.getDisplayName()
      ),
      (playerCount, var1, respawnData) -> Component.translatable(
         "commands.spawnpoint.success.multiple",
         respawnData.pos().getX(),
         respawnData.pos().getY(),
         respawnData.pos().getZ(),
         respawnData.yaw(),
         respawnData.pitch(),
         Component.translationArg(respawnData.dimension().identifier()),
         playerCount
      )
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawnpoint")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  c -> setSpawn(
                     (CommandSourceStack)c.getSource(),
                     Collections.singleton(((CommandSourceStack)c.getSource()).getPlayerOrException()),
                     BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition()),
                     WorldCoordinates.ZERO_ROTATION
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        c -> setSpawn(
                           (CommandSourceStack)c.getSource(),
                           EntityArgument.getPlayers(c, "targets"),
                           BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition()),
                           WorldCoordinates.ZERO_ROTATION
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(
                              c -> setSpawn(
                                 (CommandSourceStack)c.getSource(),
                                 EntityArgument.getPlayers(c, "targets"),
                                 BlockPosArgument.getSpawnablePos(c, "pos"),
                                 WorldCoordinates.ZERO_ROTATION
                              )
                           ))
                        .then(
                           Commands.argument("rotation", RotationArgument.rotation())
                              .executes(
                                 c -> setSpawn(
                                    (CommandSourceStack)c.getSource(),
                                    EntityArgument.getPlayers(c, "targets"),
                                    BlockPosArgument.getSpawnablePos(c, "pos"),
                                    RotationArgument.getRotation(c, "rotation")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(final CommandSourceStack source, final Collection<ServerPlayer> targets, final BlockPos pos, final Coordinates rotation) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();
      ResourceKey<Level> dimension = source.getLevel().dimension();
      Vec2 rotationVector = rotation.getRotation(source);
      float yaw = Mth.wrapDegrees(rotationVector.y);
      float pitch = Mth.clamp(rotationVector.x, -90.0F, 90.0F);
      LevelData.RespawnData respawnData = LevelData.RespawnData.of(dimension, pos, yaw, pitch);
      ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(respawnData, true);

      for (ServerPlayer target : targets) {
         target.setRespawnPosition(respawnConfig, false);
         tracker.track(target);
      }

      return tracker.sendFeedback(source, true, RESPONSE_SET, respawnData);
   }
}
