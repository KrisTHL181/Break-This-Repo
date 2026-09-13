package net.minecraft.util;

import java.util.Arrays;
import java.util.function.IntConsumer;
import org.apache.commons.lang3.Validate;

public class ZeroBitStorage implements BitStorage {
   public static final long[] RAW = new long[0];
   private final int size;

   public ZeroBitStorage(final int size) {
      this.size = size;
   }

   @Override
   public int getAndSet(final int index, final int value) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      Validate.inclusiveBetween(0L, 0L, value);
      return 0;
   }

   @Override
   public void set(final int index, final int value) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      Validate.inclusiveBetween(0L, 0L, value);
   }

   @Override
   public int get(final int index) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      return 0;
   }

   @Override
   public long[] getRaw() {
      return RAW;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public int getBits() {
      return 0;
   }

   @Override
   public void getAll(final IntConsumer output) {
      for (int i = 0; i < this.size; i++) {
         output.accept(0);
      }
   }

   @Override
   public void unpack(final int[] output) {
      Arrays.fill(output, 0, this.size, 0);
   }

   @Override
   public BitStorage copy() {
      return this;
   }
}
