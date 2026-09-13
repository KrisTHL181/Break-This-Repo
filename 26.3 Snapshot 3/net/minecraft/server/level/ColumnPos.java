package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public record ColumnPos(int x, int z) {
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;

   public ChunkPos toChunkPos() {
      return new ChunkPos(SectionPos.blockToSectionCoord(this.x), SectionPos.blockToSectionCoord(this.z));
   }

   public long toLong() {
      return asLong(this.x, this.z);
   }

   public static long asLong(final int x, final int z) {
      return x & 4294967295L | (z & 4294967295L) << 32;
   }

   public static int getX(final long pos) {
      return (int)(pos & 4294967295L);
   }

   public static int getZ(final long pos) {
      return (int)(pos >>> 32 & 4294967295L);
   }

   @Override
   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   @Override
   public int hashCode() {
      return ChunkPos.hash(this.x, this.z);
   }
}
