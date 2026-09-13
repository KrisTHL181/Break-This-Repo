package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;

public class Utf8String {
   public static String read(ByteBuf p_300143_, int p_298419_) {
      int i = ByteBufUtil.utf8MaxBytes(p_298419_);
      int j = VarInt.read(p_300143_);
      if (j > i) {
         throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i + ")");
      } else if (j < 0) {
         throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
      } else {
         int k = p_300143_.readableBytes();
         if (j > k) {
            throw new DecoderException("Not enough bytes in buffer, expected " + j + ", but got " + k);
         } else {
            String s = p_300143_.toString(p_300143_.readerIndex(), j, StandardCharsets.UTF_8);
            p_300143_.readerIndex(p_300143_.readerIndex() + j);
            if (s.length() > p_298419_) {
               throw new DecoderException("The received string length is longer than maximum allowed (" + s.length() + " > " + p_298419_ + ")");
            } else {
               return s;
            }
         }
      }
   }

   public static void write(ByteBuf p_299969_, CharSequence p_299580_, int p_298286_) {
      if (p_299580_.length() > p_298286_) {
         throw new EncoderException("String too big (was " + p_299580_.length() + " characters, max " + p_298286_ + ")");
      }

      int i = ByteBufUtil.utf8MaxBytes(p_299580_);
      ByteBuf bytebuf = p_299969_.alloc().buffer(i);

      try {
         int j = ByteBufUtil.writeUtf8(bytebuf, p_299580_);
         int k = ByteBufUtil.utf8MaxBytes(p_298286_);
         if (j > k) {
            throw new EncoderException("String too big (was " + j + " bytes encoded, max " + k + ")");
         }

         VarInt.write(p_299969_, j);
         p_299969_.writeBytes(bytebuf);
      } finally {
         bytebuf.release();
      }
   }
}
