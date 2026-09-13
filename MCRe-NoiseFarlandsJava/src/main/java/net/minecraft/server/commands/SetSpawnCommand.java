package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
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
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spawnpoint")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(
                    c -> setSpawn(
                        c.getSource(),
                        Collections.singleton(c.getSource().getPlayerOrException()),
                        BlockPos.containing(c.getSource().getPosition()),
                        WorldCoordinates.ZERO_ROTATION
                    )
                )
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .executes(
                            c -> setSpawn(
                                c.getSource(),
                                EntityArgument.getPlayers(c, "targets"),
                                BlockPos.containing(c.getSource().getPosition()),
                                WorldCoordinates.ZERO_ROTATION
                            )
                        )
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(
                                    c -> setSpawn(
                                        c.getSource(),
                                        EntityArgument.getPlayers(c, "targets"),
                                        BlockPosArgument.getSpawnablePos(c, "pos"),
                                        WorldCoordinates.ZERO_ROTATION
                                    )
                                )
                                .then(
                                    Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(
                                            c -> setSpawn(
                                                c.getSource(),
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

    private static int setSpawn(final CommandSourceStack source, final Collection<ServerPlayer> targets, final BlockPos pos, final Coordinates rotation) {
        ResourceKey<Level> dimension = source.getLevel().dimension();
        Vec2 rotationVector = rotation.getRotation(source);
        float yaw = Mth.wrapDegrees(rotationVector.y);
        float pitch = Mth.clamp(rotationVector.x, -90.0F, 90.0F);

        for (ServerPlayer target : targets) {
            target.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(dimension, pos, yaw, pitch), true), false);
        }

        String dimensionName = dimension.identifier().toString();
        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.spawnpoint.success.single",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    yaw,
                    pitch,
                    dimensionName,
                    targets.iterator().next().getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, dimensionName, targets.size()
                ),
                true
            );
        }

        return targets.size();
    }
}