package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceLoadCommand {
    private static final int MAX_CHUNK_LIMIT = 256;
    private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType(
    (max, amount) -> Component.translatableEscape("commands.forceload.toobig", max, amount)
    );
    private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType(
    (pos, dimension) -> Component.translatableEscape("commands.forceload.query.failure", pos, dimension)
    );
    private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(Component.translatable("commands.forceload.added.failure"));
    private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(
    Component.translatable("commands.forceload.removed.failure")
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("forceload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                                Commands.literal("add")
                                        .then(
                                                Commands.argument("from", ColumnPosArgument.columnPos())
                                                        .executes(
                                                                c -> changeForceLoad(
                                                                c.getSource(), ColumnPosArgument.getColumnPos(c, "from"), ColumnPosArgument.getColumnPos(c, "from"), true
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument("to", ColumnPosArgument.columnPos())
                                                                        .executes(
                                                                                c -> changeForceLoad(
                                                                                c.getSource(), ColumnPosArgument.getColumnPos(c, "from"), ColumnPosArgument.getColumnPos(c, "to"), true
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("remove")
                                        .then(
                                                Commands.argument("from", ColumnPosArgument.columnPos())
                                                        .executes(
                                                                c -> changeForceLoad(
                                                                c.getSource(), ColumnPosArgument.getColumnPos(c, "from"), ColumnPosArgument.getColumnPos(c, "from"), false
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument("to", ColumnPosArgument.columnPos())
                                                                        .executes(
                                                                                c -> changeForceLoad(
                                                                                c.getSource(), ColumnPosArgument.getColumnPos(c, "from"), ColumnPosArgument.getColumnPos(c, "to"), false
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(Commands.literal("all").executes(c -> removeAll(c.getSource())))
                        )
                        .then(
                                Commands.literal("query")
                                        .executes(c -> listForceLoad(c.getSource()))
                                        .then(
                                                Commands.argument("pos", ColumnPosArgument.columnPos())
                                                        .executes(c -> queryForceLoad(c.getSource(), ColumnPosArgument.getColumnPos(c, "pos")))
                                        )
                        )
        );
    }

    private static int queryForceLoad(final CommandSourceStack source, final ColumnPos pos)
            throws CommandSyntaxException {
        ChunkPos chunkPos = pos.toChunkPos();
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        boolean result = level.getForceLoadedChunks().contains(chunkPos);
        if (result) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.forceload.query.success", Component.translationArg(chunkPos), Component.translationArg(dimension.identifier())
                    ),
                    false
            );
            return 1;
        } else {
            throw ERROR_NOT_TICKING.create(chunkPos, dimension.identifier());
        }
    }

    private static int listForceLoad(final CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        Set<ChunkPos> forcedChunks = level.getForceLoadedChunks();
        int chunkCount = forcedChunks.size();
        if (chunkCount > 0) {
            String chunkList = Joiner.on(", ").join(forcedChunks.stream().map(ChunkPos::toString).iterator());
            if (chunkCount == 1) {
                source.sendSuccess(
                        () -> Component.translatable("commands.forceload.list.single", Component.translationArg(dimension.identifier()), chunkList), false
                );
            } else {
                source.sendSuccess(
                        () -> Component.translatable("commands.forceload.list.multiple", chunkCount, Component.translationArg(dimension.identifier()), chunkList),
                        false
                );
            }
        } else {
            source.sendFailure(Component.translatable("commands.forceload.added.none", Component.translationArg(dimension.identifier())));
        }

        return chunkCount;
    }

    private static int removeAll(final CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        Set<ChunkPos> forcedChunks = level.getForceLoadedChunks();
        forcedChunks.forEach(chunk -> level.setChunkForced((int)chunk.x(), (int)chunk.z(), false));
        source.sendSuccess(() -> Component.translatable("commands.forceload.removed.all", Component.translationArg(dimension.identifier())), true);
        return 0;
    }

    private static int changeForceLoad(final CommandSourceStack source, final ColumnPos from, final ColumnPos to, final boolean add)
            throws CommandSyntaxException {
        int minX = Math.min(from.x(), to.x());
        int minZ = Math.min(from.z(), to.z());
        int maxX = Math.max(from.x(), to.x());
        int maxZ = Math.max(from.z(), to.z());
        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
        long chunkCount = (maxChunkX - minChunkX + 1L) * (maxChunkZ - minChunkZ + 1L);
        if (chunkCount > 256L) {
            throw ERROR_TOO_MANY_CHUNKS.create(256, chunkCount);
        }

        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos firstChanged = null;
        int changedCount = 0;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                boolean changed = level.setChunkForced(x, z, add);
                if (changed) {
                    changedCount++;
                    if (firstChanged == null) {
                        firstChanged = new ChunkPos(x, z);
                    }
                }
            }
        }

        ChunkPos finalFirstChanged = firstChanged;
        int changedChunks = changedCount;
        if (changedChunks == 0) {
            throw (add ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
        }

        if (changedChunks == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.forceload." + (add ? "added" : "removed") + ".single",
                            Component.translationArg(finalFirstChanged),
                            Component.translationArg(dimension.identifier())
                    ),
                    true
            );
        } else {
            ChunkPos min = new ChunkPos(minChunkX, minChunkZ);
            ChunkPos max = new ChunkPos(maxChunkX, maxChunkZ);
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.forceload." + (add ? "added" : "removed") + ".multiple",
                            changedChunks,
                            Component.translationArg(dimension.identifier()),
                            Component.translationArg(min),
                            Component.translationArg(max)
                    ),
                    true
            );
        }

        return changedChunks;
    }
}
