package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
   private static final int MAX_VARLONG_SIZE = 10;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(long p_297916_) {
      for (int i = 1; i < 10; i++) {
         if ((p_297916_ & -1L << i * 7) == 0L) {
            return i;
         }
      }

      return 10;
   }

   public static boolean hasContinuationBit(byte p_298368_) {
      return (p_298368_ & 128) == 128;
   }

   public static long read(ByteBuf p_297482_) {
      long i = 0L;
      int j = 0;

      byte b0;
      do {
         b0 = p_297482_.readByte();
         i |= (long)(b0 & 127) << j++ * 7;
         if (j > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while (hasContinuationBit(b0));

      return i;
   }

   public static ByteBuf write(ByteBuf p_301156_, long p_297622_) {
      while ((p_297622_ & -128L) != 0L) {
         p_301156_.writeByte((int)(p_297622_ & 127L) | 128);
         p_297622_ >>>= 7;
      }

      p_301156_.writeByte((int)p_297622_);
      return p_301156_;
   }
}
