package net.minecraft.util.datafix;

import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;

public class PackedBitStorage {
   private static final int BIT_TO_LONG_SHIFT = 6;
   private final long[] data;
   private final int bits;
   private final long mask;
   private final int size;

   public PackedBitStorage(final int bits, final int size) {
      this(bits, size, new long[Mth.roundToward(size * bits, 64) / 64]);
   }

   public PackedBitStorage(final int bits, final int size, final long[] data) {
      Validate.inclusiveBetween(1L, 32L, bits);
      this.size = size;
      this.bits = bits;
      this.data = data;
      this.mask = (1L << bits) - 1L;
      int requiredLength = Mth.roundToward(size * bits, 64) / 64;
      if (data.length != requiredLength) {
         throw new IllegalArgumentException("Invalid length given for storage, got: " + data.length + " but expected: " + requiredLength);
      }
   }

   public void set(final int index, final int value) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      Validate.inclusiveBetween(0L, this.mask, value);
      int position = index * this.bits;
      int startData = position >> 6;
      int endData = (index + 1) * this.bits - 1 >> 6;
      int startBit = position ^ startData << 6;
      this.data[startData] = this.data[startData] & ~(this.mask << startBit) | (value & this.mask) << startBit;
      if (startData != endData) {
         int shiftBits = 64 - startBit;
         int wantedBits = this.bits - shiftBits;
         this.data[endData] = this.data[endData] >>> wantedBits << wantedBits | (value & this.mask) >> shiftBits;
      }
   }

   public int get(final int index) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      int position = index * this.bits;
      int startData = position >> 6;
      int endData = (index + 1) * this.bits - 1 >> 6;
      int startBit = position ^ startData << 6;
      if (startData == endData) {
         return (int)(this.data[startData] >>> startBit & this.mask);
      }

      int shiftBits = 64 - startBit;
      return (int)((this.data[startData] >>> startBit | this.data[endData] << shiftBits) & this.mask);
   }

   public long[] getRaw() {
      return this.data;
   }

   public int getBits() {
      return this.bits;
   }
}
