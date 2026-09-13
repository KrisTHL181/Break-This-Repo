package net.minecraft.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

public record PngInfo(int width, int height) {
   private static final HexFormat FORMAT = HexFormat.of().withUpperCase().withPrefix("0x");
   private static final long PNG_HEADER = -8552249625308161526L;
   private static final int IHDR_TYPE = 1229472850;
   private static final int IHDR_SIZE = 13;

   public static PngInfo fromStream(InputStream p_301756_) throws IOException {
      DataInputStream datainputstream = new DataInputStream(p_301756_);
      long i = datainputstream.readLong();
      if (i != -8552249625308161526L) {
         throw new IOException("Bad PNG Signature: " + FORMAT.toHexDigits(i));
      }

      int j = datainputstream.readInt();
      if (j != 13) {
         throw new IOException("Bad length for IHDR chunk: " + j);
      }

      int k = datainputstream.readInt();
      if (k != 1229472850) {
         throw new IOException("Bad type for IHDR chunk: " + FORMAT.toHexDigits(k));
      }

      int l = datainputstream.readInt();
      int i1 = datainputstream.readInt();
      return new PngInfo(l, i1);
   }

   public static PngInfo fromBytes(byte[] p_301719_) throws IOException {
      return fromStream(new ByteArrayInputStream(p_301719_));
   }

   public static void validateHeader(ByteBuffer p_311156_) throws IOException {
      ByteOrder byteorder = p_311156_.order();
      p_311156_.order(ByteOrder.BIG_ENDIAN);
      if (p_311156_.limit() < 16) {
         throw new IOException("PNG header missing");
      }

      if (p_311156_.getLong(0) != -8552249625308161526L) {
         throw new IOException("Bad PNG Signature");
      }

      if (p_311156_.getInt(8) != 13) {
         throw new IOException("Bad length for IHDR chunk!");
      }

      if (p_311156_.getInt(12) != 1229472850) {
         throw new IOException("Bad type for IHDR chunk!");
      }

      p_311156_.order(byteorder);
   }
}
