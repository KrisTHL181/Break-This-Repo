package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
   private final BitSet used = new BitSet();

   public void force(final int position, final int size) {
      this.used.set(position, position + size);
   }

   public void free(final int position, final int size) {
      this.used.clear(position, position + size);
   }

   public int allocate(final int size) {
      int current = 0;

      while (true) {
         int freeStart = this.used.nextClearBit(current);
         int freeEnd = this.used.nextSetBit(freeStart);
         if (freeEnd == -1 || freeEnd - freeStart >= size) {
            this.force(freeStart, size);
            return freeStart;
         }

         current = freeEnd;
      }
   }

   @VisibleForTesting
   public IntSet getUsed() {
      return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
   }
}
