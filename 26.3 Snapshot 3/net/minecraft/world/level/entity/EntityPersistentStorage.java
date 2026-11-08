package net.minecraft.world.level.entity;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;

public interface EntityPersistentStorage<T> extends AutoCloseable {
   CompletableFuture<ChunkEntities<T>> loadEntities(ChunkPos pos);

   void storeEntities(final ChunkEntities<T> chunk);

   void flush(boolean flushStorage);

   @Override
   default void close() throws IOException {
   }
}
