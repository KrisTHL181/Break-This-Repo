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

   public DataLayer(int p_62554_) {
      this.defaultValue = p_62554_;
   }

   public DataLayer(byte[] p_62556_) {
      this.data = p_62556_;
      this.defaultValue = 0;
      if (p_62556_.length != 2048) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + p_62556_.length));
      }
   }

   public int get(int p_62561_, int p_62562_, int p_62563_) {
      return this.get(getIndex(p_62561_, p_62562_, p_62563_));
   }

   public void set(int p_62565_, int p_62566_, int p_62567_, int p_62568_) {
      this.set(getIndex(p_62565_, p_62566_, p_62567_), p_62568_);
   }

   private static int getIndex(int p_62572_, int p_62573_, int p_62574_) {
      return p_62573_ << 8 | p_62574_ << 4 | p_62572_;
   }

   private int get(int p_62571_) {
      if (this.data == null) {
         return this.defaultValue;
      }

      int i = getByteIndex(p_62571_);
      int j = getNibbleIndex(p_62571_);
      return this.data[i] >> 4 * j & 15;
   }

   private void set(int p_62558_, int p_62559_) {
      byte[] abyte = this.getData();
      int i = getByteIndex(p_62558_);
      int j = getNibbleIndex(p_62558_);
      int k = ~(15 << 4 * j);
      int l = (p_62559_ & 15) << 4 * j;
      abyte[i] = (byte)(abyte[i] & k | l);
   }

   private static int getNibbleIndex(int p_182482_) {
      return p_182482_ & 1;
   }

   private static int getByteIndex(int p_62579_) {
      return p_62579_ >> 1;
   }

   public void fill(int p_285142_) {
      this.defaultValue = p_285142_;
      this.data = null;
   }

   private static byte packFilled(int p_282176_) {
      byte b0 = (byte)p_282176_;

      for (int i = 4; i < 8; i += 4) {
         b0 = (byte)(b0 | p_282176_ << i);
      }

      return b0;
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
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < 4096; i++) {
         stringbuilder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            stringbuilder.append("\n");
         }

         if ((i & 0xFF) == 255) {
            stringbuilder.append("\n");
         }
      }

      return stringbuilder.toString();
   }

   @VisibleForDebug
   public String layerToString(int p_156342_) {
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < 256; i++) {
         stringbuilder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            stringbuilder.append("\n");
         }
      }

      return stringbuilder.toString();
   }

   public boolean isDefinitelyHomogenous() {
      return this.data == null;
   }

   public boolean isDefinitelyFilledWith(int p_281763_) {
      return this.data == null && this.defaultValue == p_281763_;
   }

   public boolean isEmpty() {
      return this.data == null && this.defaultValue == 0;
   }
}
