package net.minecraft.world.level.chunk;

import java.util.Arrays;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;

public class DataLayer {
   public static final int LAYER_COUNT = 16;
   public static final int LAYER_SIZE = 128;
   public static final int SIZE = 2048;
   private static final int NIBBLE_SIZE = 4;
   protected byte @Nullable [] data;
   private int defaultValue;

   public DataLayer() {
      this(0);
   }

   public DataLayer(final int defaultValue) {
      this.defaultValue = defaultValue;
   }

   public DataLayer(final byte[] data) {
      this.data = data;
      this.defaultValue = 0;
      if (data.length != 2048) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + data.length));
      }
   }

   public int get(final int x, final int y, final int z) {
      return this.get(getIndex(x, y, z));
   }

   public void set(final int x, final int y, final int z, final int val) {
      this.set(getIndex(x, y, z), val);
   }

   private static int getIndex(final int x, final int y, final int z) {
      return y << 8 | z << 4 | x;
   }

   private int get(final int index) {
      if (this.data == null) {
         return this.defaultValue;
      }

      int position = getByteIndex(index);
      int nibble = getNibbleIndex(index);
      return this.data[position] >> 4 * nibble & 15;
   }

   private void set(final int index, final int val) {
      byte[] data = this.getData();
      int position = getByteIndex(index);
      int nibble = getNibbleIndex(index);
      int mask = ~(15 << 4 * nibble);
      int valueToSet = (val & 15) << 4 * nibble;
      data[position] = (byte)(data[position] & mask | valueToSet);
   }

   private static int getNibbleIndex(final int index) {
      return index & 1;
   }

   private static int getByteIndex(final int position) {
      return position >> 1;
   }

   public void fill(final int value) {
      this.defaultValue = value;
      this.data = null;
   }

   private static byte packFilled(final int value) {
      byte packed = (byte)value;

      for (int i = 4; i < 8; i += 4) {
         packed = (byte)(packed | value << i);
      }

      return packed;
   }

   public byte[] getData() {
      if (this.data == null) {
         this.data = new byte[2048];
         if (this.defaultValue != 0) {
            Arrays.fill(this.data, packFilled(this.defaultValue));
         }
      }

      return this.data;
   }

   public DataLayer copy() {
      return this.data == null ? new DataLayer(this.defaultValue) : new DataLayer((byte[])this.data.clone());
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < 4096; i++) {
         builder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            builder.append("\n");
         }

         if ((i & 0xFF) == 255) {
            builder.append("\n");
         }
      }

      return builder.toString();
   }

   @VisibleForDebug
   public String layerToString(final int layer) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < 256; i++) {
         builder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            builder.append("\n");
         }
      }

      return builder.toString();
   }

   public boolean isDefinitelyHomogenous() {
      return this.data == null;
   }

   public boolean isDefinitelyFilledWith(final int value) {
      return this.data == null && this.defaultValue == value;
   }

   public boolean isEmpty() {
      return this.data == null && this.defaultValue == 0;
   }
}
