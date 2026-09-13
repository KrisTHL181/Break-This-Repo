package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
   public static final int MAX_VARINT_SIZE = 5;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(final int value) {
      for (int i = 1; i < 5; i++) {
         if ((value & -1 << i * 7) == 0) {
            return i;
         }
      }

      return 5;
   }

   public static boolean hasContinuationBit(final byte in) {
      return (in & 128) == 128;
   }

   public static int read(final ByteBuf input) {
      int out = 0;
      int bytes = 0;

      byte in;
      do {
         in = input.readByte();
         out |= (in & 127) << bytes++ * 7;
         if (bytes > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while (hasContinuationBit(in));

      return out;
   }

   public static ByteBuf write(final ByteBuf output, int value) {
      while ((value & -128) != 0) {
         output.writeByte(value & 127 | 128);
         value >>>= 7;
      }

      output.writeByte(value);
      return output;
   }
}
