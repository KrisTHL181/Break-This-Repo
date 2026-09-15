package net.minecraft.world.level.entity;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;

public interface EntityPersistentStorage<T> extends AutoCloseable {
   CompletableFuture<ChunkEntities<T>> loadEntities(ChunkPos var1);

   void storeEntities(ChunkEntities<T> var1);

   void flush(boolean var1);

   @Override
   default void close() throws IOException {
   }
}
