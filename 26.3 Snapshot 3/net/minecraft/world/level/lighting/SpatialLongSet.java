package net.minecraft.world.level.lighting;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.NoSuchElementException;
import net.minecraft.util.Mth;

public class SpatialLongSet extends LongLinkedOpenHashSet {
   private final SpatialLongSet.InternalMap map;

   public SpatialLongSet(final int expected, final float f) {
      super(expected, f);
      this.map = new SpatialLongSet.InternalMap(expected / 64, f);
   }

   public boolean add(final long k) {
      return this.map.addBit(k);
   }

   public boolean rem(final long k) {
      return this.map.removeBit(k);
   }

   public long removeFirstLong() {
      return this.map.removeFirstBit();
   }

   public int size() {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   @VisibleForTesting
   static class InternalMap extends Long2LongLinkedOpenHashMap {
      private static final int X_BITS = Mth.log2(60000000);
      private static final int Z_BITS = Mth.log2(60000000);
      private static final int Y_BITS = 64 - X_BITS - Z_BITS;
      private static final int Y_OFFSET = 0;
      private static final int Z_OFFSET = Y_BITS;
      private static final int X_OFFSET = Y_BITS + Z_BITS;
      private static final long OUTER_MASK = 3L << X_OFFSET | 3L | 3L << Z_OFFSET;
      private int lastPos = -1;
      private long lastOuterKey;
      private final int minSize;

      public InternalMap(final int expected, final float f) {
         super(expected, f);
         this.minSize = expected;
      }

      @VisibleForTesting
      static long getOuterKey(final long key) {
         return key & ~OUTER_MASK;
      }

      @VisibleForTesting
      static int getInnerKey(final long key) {
         int innerX = (int)(key >>> X_OFFSET & 3L);
         int innerY = (int)(key >>> 0 & 3L);
         int innerZ = (int)(key >>> Z_OFFSET & 3L);
         return innerX << 4 | innerZ << 2 | innerY;
      }

      @VisibleForTesting
      static long getFullKey(long outerKey, final int innerKey) {
         outerKey |= (long)(innerKey >>> 4 & 3) << X_OFFSET;
         outerKey |= (long)(innerKey >>> 2 & 3) << Z_OFFSET;
         return outerKey | (long)(innerKey >>> 0 & 3) << 0;
      }

      public boolean addBit(final long key) {
         long outerKey = getOuterKey(key);
         int innerKey = getInnerKey(key);
         long bitMask = 1L << innerKey;
         int pos;
         if (outerKey == 0L) {
            if (this.containsNullKey) {
               return this.replaceBit(this.n, bitMask);
            }

            this.containsNullKey = true;
            pos = this.n;
         } else {
            if (this.lastPos != -1 && outerKey == this.lastOuterKey) {
               return this.replaceBit(this.lastPos, bitMask);
            }

            long[] keys = this.key;
            pos = (int)HashCommon.mix(outerKey) & this.mask;

            for (long curr = keys[pos]; curr != 0L; curr = keys[pos]) {
               if (curr == outerKey) {
                  this.lastPos = pos;
                  this.lastOuterKey = outerKey;
                  return this.replaceBit(pos, bitMask);
               }

               pos = pos + 1 & this.mask;
            }
         }

         this.key[pos] = outerKey;
         this.value[pos] = bitMask;
         if (this.size == 0) {
            this.first = this.last = pos;
            this.link[pos] = -1L;
         } else {
            this.link[this.last] = this.link[this.last] ^ (this.link[this.last] ^ pos & 4294967295L) & 4294967295L;
            this.link[pos] = (this.last & 4294967295L) << 32 | 4294967295L;
            this.last = pos;
         }

         if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
         }

         return false;
      }

      private boolean replaceBit(final int pos, final long bitMask) {
         boolean oldValue = (this.value[pos] & bitMask) != 0L;
         this.value[pos] = this.value[pos] | bitMask;
         return oldValue;
      }

      public boolean removeBit(final long key) {
         long outerKey = getOuterKey(key);
         int innerKey = getInnerKey(key);
         long bitMask = 1L << innerKey;
         if (outerKey == 0L) {
            return this.containsNullKey ? this.removeFromNullEntry(bitMask) : false;
         }

         if (this.lastPos != -1 && outerKey == this.lastOuterKey) {
            return this.removeFromEntry(this.lastPos, bitMask);
         }

         long[] keys = this.key;
         int pos = (int)HashCommon.mix(outerKey) & this.mask;

         for (long curr = keys[pos]; curr != 0L; curr = keys[pos]) {
            if (outerKey == curr) {
               this.lastPos = pos;
               this.lastOuterKey = outerKey;
               return this.removeFromEntry(pos, bitMask);
            }

            pos = pos + 1 & this.mask;
         }

         return false;
      }

      private boolean removeFromNullEntry(final long bitMask) {
         if ((this.value[this.n] & bitMask) == 0L) {
            return false;
         }

         this.value[this.n] = this.value[this.n] & ~bitMask;
         if (this.value[this.n] != 0L) {
            return true;
         }

         this.containsNullKey = false;
         this.size--;
         this.fixPointers(this.n);
         if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
         }

         return true;
      }

      private boolean removeFromEntry(final int pos, final long bitMask) {
         if ((this.value[pos] & bitMask) == 0L) {
            return false;
         }

         this.value[pos] = this.value[pos] & ~bitMask;
         if (this.value[pos] != 0L) {
            return true;
         }

         this.lastPos = -1;
         this.size--;
         this.fixPointers(pos);
         this.shiftKeys(pos);
         if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
         }

         return true;
      }

      public long removeFirstBit() {
         if (this.size == 0) {
            throw new NoSuchElementException();
         }

         int pos = this.first;
         long outerKey = this.key[pos];
         int innerKey = Long.numberOfTrailingZeros(this.value[pos]);
         this.value[pos] = this.value[pos] & ~(1L << innerKey);
         if (this.value[pos] == 0L) {
            this.removeFirstLong();
            this.lastPos = -1;
         }

         return getFullKey(outerKey, innerKey);
      }

      protected void rehash(final int newN) {
         if (newN > this.minSize) {
            super.rehash(newN);
         }
      }
   }
}
