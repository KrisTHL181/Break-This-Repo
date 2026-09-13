package net.minecraft.util;

import java.util.Arrays;
import java.util.function.IntConsumer;
import org.apache.commons.lang3.Validate;

public class ZeroBitStorage implements BitStorage {
   public static final long[] RAW = new long[0];
   private final int size;

   public ZeroBitStorage(int p_184791_) {
      this.size = p_184791_;
   }

   @Override
   public int getAndSet(int p_184796_, int p_184797_) {
      Validate.inclusiveBetween(0L, this.size - 1, p_184796_);
      Validate.inclusiveBetween(0L, 0L, p_184797_);
      return 0;
   }

   @Override
   public void set(int p_184802_, int p_184803_) {
      Validate.inclusiveBetween(0L, this.size - 1, p_184802_);
      Validate.inclusiveBetween(0L, 0L, p_184803_);
   }

   @Override
   public int get(int p_184794_) {
      Validate.inclusiveBetween(0L, this.size - 1, p_184794_);
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
   public void getAll(IntConsumer p_184799_) {
      for (int i = 0; i < this.size; i++) {
         p_184799_.accept(0);
      }
   }

   @Override
   public void unpack(int[] p_198170_) {
      Arrays.fill(p_198170_, 0, this.size, 0);
   }

   @Override
   public BitStorage copy() {
      return this;
   }
}
