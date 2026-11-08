package net.minecraft.world.level.chunk.storage;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

@FunctionalInterface
public interface LegacyTagFixer {
   Supplier<LegacyTagFixer> EMPTY = () -> p_456738_ -> p_456738_;

   CompoundTag applyFix(CompoundTag var1);

   default void markChunkDone(ChunkPos p_456889_) {
   }

   default int targetDataVersion() {
      return -1;
   }
}
