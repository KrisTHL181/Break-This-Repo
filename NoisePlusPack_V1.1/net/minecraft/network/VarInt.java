package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
   public static final int MAX_VARINT_SIZE = 5;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(int p_298763_) {
      for (int i = 1; i < 5; i++) {
         if ((p_298763_ & -1 << i * 7) == 0) {
            return i;
         }
      }

      return 5;
   }

   public static boolean hasContinuationBit(byte p_299197_) {
      return (p_299197_ & 128) == 128;
   }

   public static int read(ByteBuf p_298603_) {
      int i = 0;
      int j = 0;

      byte b0;
      do {
         b0 = p_298603_.readByte();
         i |= (b0 & 127) << j++ * 7;
         if (j > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while (hasContinuationBit(b0));

      return i;
   }

   public static ByteBuf write(ByteBuf p_300403_, int p_297833_) {
      while ((p_297833_ & -128) != 0) {
         p_300403_.writeByte(p_297833_ & 127 | 128);
         p_297833_ >>>= 7;
      }

      p_300403_.writeByte(p_297833_);
      return p_300403_;
   }
}
