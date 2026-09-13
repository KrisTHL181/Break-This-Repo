package net.minecraft.util.debug;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface DebugValueAccess {
   <T> void forEachChunk(DebugSubscription<T> var1, BiConsumer<ChunkPos, T> var2);

   <T> @Nullable T getChunkValue(DebugSubscription<T> var1, ChunkPos var2);

   <T> void forEachBlock(DebugSubscription<T> var1, BiConsumer<BlockPos, T> var2);

   <T> @Nullable T getBlockValue(DebugSubscription<T> var1, BlockPos var2);

   <T> void forEachEntity(DebugSubscription<T> var1, BiConsumer<Entity, T> var2);

   <T> @Nullable T getEntityValue(DebugSubscription<T> var1, Entity var2);

   <T> void forEachEvent(DebugSubscription<T> var1, DebugValueAccess.EventVisitor<T> var2);

   @FunctionalInterface
   interface EventVisitor<T> {
      void accept(T var1, int var2, int var3);
   }
}
