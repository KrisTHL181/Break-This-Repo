package net.minecraft.world.level.chunk.status;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;

@FunctionalInterface
public interface ChunkStatusTask {
   CompletableFuture<ChunkAccess> doWork(WorldGenContext var1, ChunkStep var2, StaticCache2D<GenerationChunkHolder> var3, ChunkAccess var4);
}
