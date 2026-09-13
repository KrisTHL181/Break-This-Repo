package net.minecraft.world.level.chunk.status;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;

@FunctionalInterface
public interface ChunkStatusTask {
   CompletableFuture<ChunkAccess> doWork(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk);
}
