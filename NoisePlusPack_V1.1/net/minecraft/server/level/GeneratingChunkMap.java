package net.minecraft.server.level;

import java.util.concurrent.CompletableFuture;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;

public interface GeneratingChunkMap {
   GenerationChunkHolder acquireGeneration(long var1);

   void releaseGeneration(GenerationChunkHolder var1);

   CompletableFuture<ChunkAccess> applyStep(GenerationChunkHolder var1, ChunkStep var2, StaticCache2D<GenerationChunkHolder> var3);

   ChunkGenerationTask scheduleGenerationTask(ChunkStatus var1, ChunkPos var2);

   void runGenerationTasks();
}
