package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
   private static final int MAX_VARLONG_SIZE = 10;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(final long value) {
      for (int i = 1; i < 10; i++) {
         if ((value & -1L << i * 7) == 0L) {
            return i;
         }
      }

      return 10;
   }

   public static boolean hasContinuationBit(final byte in) {
      return (in & 128) == 128;
   }

   public static long read(final ByteBuf input) {
      long out = 0L;
      int bytes = 0;

      byte in;
      do {
         in = input.readByte();
         out |= (long)(in & 127) << bytes++ * 7;
         if (bytes > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while (hasContinuationBit(in));

      return out;
   }

   public static ByteBuf write(final ByteBuf output, long value) {
      while ((value & -128L) != 0L) {
         output.writeByte((int)(value & 127L) | 128);
         value >>>= 7;
      }

      output.writeByte((int)value);
      return output;
   }
}
