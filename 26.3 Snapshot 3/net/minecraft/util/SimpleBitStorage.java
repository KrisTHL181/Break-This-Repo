package net.minecraft.util;

import java.util.function.IntConsumer;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class SimpleBitStorage implements BitStorage {
   private static final int[] MAGIC = new int[]{
      -1,
      -1,
      0,
      Integer.MIN_VALUE,
      0,
      0,
      1431655765,
      1431655765,
      0,
      Integer.MIN_VALUE,
      0,
      1,
      858993459,
      858993459,
      0,
      715827882,
      715827882,
      0,
      613566756,
      613566756,
      0,
      Integer.MIN_VALUE,
      0,
      2,
      477218588,
      477218588,
      0,
      429496729,
      429496729,
      0,
      390451572,
      390451572,
      0,
      357913941,
      357913941,
      0,
      330382099,
      330382099,
      0,
      306783378,
      306783378,
      0,
      286331153,
      286331153,
      0,
      Integer.MIN_VALUE,
      0,
      3,
      252645135,
      252645135,
      0,
      238609294,
      238609294,
      0,
      226050910,
      226050910,
      0,
      214748364,
      214748364,
      0,
      204522252,
      204522252,
      0,
      195225786,
      195225786,
      0,
      186737708,
      186737708,
      0,
      178956970,
      178956970,
      0,
      171798691,
      171798691,
      0,
      165191049,
      165191049,
      0,
      159072862,
      159072862,
      0,
      153391689,
      153391689,
      0,
      148102320,
      148102320,
      0,
      143165576,
      143165576,
      0,
      138547332,
      138547332,
      0,
      Integer.MIN_VALUE,
      0,
      4,
      130150524,
      130150524,
      0,
      126322567,
      126322567,
      0,
      122713351,
      122713351,
      0,
      119304647,
      119304647,
      0,
      116080197,
      116080197,
      0,
      113025455,
      113025455,
      0,
      110127366,
      110127366,
      0,
      107374182,
      107374182,
      0,
      104755299,
      104755299,
      0,
      102261126,
      102261126,
      0,
      99882960,
      99882960,
      0,
      97612893,
      97612893,
      0,
      95443717,
      95443717,
      0,
      93368854,
      93368854,
      0,
      91382282,
      91382282,
      0,
      89478485,
      89478485,
      0,
      87652393,
      87652393,
      0,
      85899345,
      85899345,
      0,
      84215045,
      84215045,
      0,
      82595524,
      82595524,
      0,
      81037118,
      81037118,
      0,
      79536431,
      79536431,
      0,
      78090314,
      78090314,
      0,
      76695844,
      76695844,
      0,
      75350303,
      75350303,
      0,
      74051160,
      74051160,
      0,
      72796055,
      72796055,
      0,
      71582788,
      71582788,
      0,
      70409299,
      70409299,
      0,
      69273666,
      69273666,
      0,
      68174084,
      68174084,
      0,
      Integer.MIN_VALUE,
      0,
      5
   };
   private final long[] data;
   private final int bits;
   private final long mask;
   private final int size;
   private final int valuesPerLong;
   private final int divideMul;
   private final int divideAdd;
   private final int divideShift;

   public SimpleBitStorage(final int bits, final int size, final int[] values) {
      this(bits, size);
      int outputIndex = 0;

      int inputOffset;
      for (inputOffset = 0; inputOffset <= size - this.valuesPerLong; inputOffset += this.valuesPerLong) {
         long packedValue = 0L;

         for (int indexInLong = this.valuesPerLong - 1; indexInLong >= 0; indexInLong--) {
            packedValue <<= bits;
            packedValue |= values[inputOffset + indexInLong] & this.mask;
         }

         this.data[outputIndex++] = packedValue;
      }

      int remainderCount = size - inputOffset;
      if (remainderCount > 0) {
         long lastPackedValue = 0L;

         for (int indexInLong = remainderCount - 1; indexInLong >= 0; indexInLong--) {
            lastPackedValue <<= bits;
            lastPackedValue |= values[inputOffset + indexInLong] & this.mask;
         }

         this.data[outputIndex] = lastPackedValue;
      }
   }

   public SimpleBitStorage(final int bits, final int size) {
      this(bits, size, (long[])null);
   }

   public SimpleBitStorage(final int bits, final int size, final long @Nullable [] data) {
      Validate.inclusiveBetween(1L, 32L, bits);
      this.size = size;
      this.bits = bits;
      this.mask = (1L << bits) - 1L;
      this.valuesPerLong = (char)(64 / bits);
      int row = 3 * (this.valuesPerLong - 1);
      this.divideMul = MAGIC[row + 0];
      this.divideAdd = MAGIC[row + 1];
      this.divideShift = MAGIC[row + 2];
      int requiredLength = (size + this.valuesPerLong - 1) / this.valuesPerLong;
      if (data != null) {
         if (data.length != requiredLength) {
            throw new SimpleBitStorage.InitializationException("Invalid length given for storage, got: " + data.length + " but expected: " + requiredLength);
         }

         this.data = data;
      } else {
         this.data = new long[requiredLength];
      }
   }

   private int cellIndex(final int bitIndex) {
      long mul = Integer.toUnsignedLong(this.divideMul);
      long add = Integer.toUnsignedLong(this.divideAdd);
      return (int)(bitIndex * mul + add >> 32 >> this.divideShift);
   }

   @Override
   public int getAndSet(final int index, final int value) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      Validate.inclusiveBetween(0L, this.mask, value);
      int cellIndex = this.cellIndex(index);
      long cellValue = this.data[cellIndex];
      int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
      int oldValue = (int)(cellValue >> bitIndex & this.mask);
      this.data[cellIndex] = cellValue & ~(this.mask << bitIndex) | (value & this.mask) << bitIndex;
      return oldValue;
   }

   @Override
   public void set(final int index, final int value) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      Validate.inclusiveBetween(0L, this.mask, value);
      int cellIndex = this.cellIndex(index);
      long cellValue = this.data[cellIndex];
      int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
      this.data[cellIndex] = cellValue & ~(this.mask << bitIndex) | (value & this.mask) << bitIndex;
   }

   @Override
   public int get(final int index) {
      Validate.inclusiveBetween(0L, this.size - 1, index);
      int cellIndex = this.cellIndex(index);
      long cellValue = this.data[cellIndex];
      int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
      return (int)(cellValue >> bitIndex & this.mask);
   }

   @Override
   public long[] getRaw() {
      return this.data;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public int getBits() {
      return this.bits;
   }

   @Override
   public void getAll(final IntConsumer output) {
      int count = 0;

      for (long cellValue : this.data) {
         for (int value = 0; value < this.valuesPerLong; value++) {
            output.accept((int)(cellValue & this.mask));
            cellValue >>= this.bits;
            if (++count >= this.size) {
               return;
            }
         }
      }
   }

   @Override
   public void unpack(final int[] output) {
      int dataLength = this.data.length;
      int outputOffset = 0;

      for (int i = 0; i < dataLength - 1; i++) {
         long cellValue = this.data[i];

         for (int indexInLong = 0; indexInLong < this.valuesPerLong; indexInLong++) {
            output[outputOffset + indexInLong] = (int)(cellValue & this.mask);
            cellValue >>= this.bits;
         }

         outputOffset += this.valuesPerLong;
      }

      int remainder = this.size - outputOffset;
      if (remainder > 0) {
         long cellValue = this.data[dataLength - 1];

         for (int indexInLong = 0; indexInLong < remainder; indexInLong++) {
            output[outputOffset + indexInLong] = (int)(cellValue & this.mask);
            cellValue >>= this.bits;
         }
      }
   }

   @Override
   public BitStorage copy() {
      return new SimpleBitStorage(this.bits, this.size, (long[])this.data.clone());
   }

   public static class InitializationException extends RuntimeException {
      private InitializationException(final String message) {
         super(message);
      }
   }
}
