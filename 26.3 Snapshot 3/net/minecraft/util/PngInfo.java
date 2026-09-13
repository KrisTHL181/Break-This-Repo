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

   public static PngInfo fromStream(final InputStream inputStream) throws IOException {
      DataInputStream stream = new DataInputStream(inputStream);
      long magic = stream.readLong();
      if (magic != -8552249625308161526L) {
         throw new IOException("Bad PNG Signature: " + FORMAT.toHexDigits(magic));
      }

      int headerSize = stream.readInt();
      if (headerSize != 13) {
         throw new IOException("Bad length for IHDR chunk: " + headerSize);
      }

      int headerType = stream.readInt();
      if (headerType != 1229472850) {
         throw new IOException("Bad type for IHDR chunk: " + FORMAT.toHexDigits(headerType));
      }

      int width = stream.readInt();
      int height = stream.readInt();
      return new PngInfo(width, height);
   }

   public static PngInfo fromBytes(final byte[] bytes) throws IOException {
      return fromStream(new ByteArrayInputStream(bytes));
   }

   public static void validateHeader(final ByteBuffer buffer) throws IOException {
      ByteOrder order = buffer.order();
      buffer.order(ByteOrder.BIG_ENDIAN);
      if (buffer.limit() < 16) {
         throw new IOException("PNG header missing");
      }

      if (buffer.getLong(0) != -8552249625308161526L) {
         throw new IOException("Bad PNG Signature");
      }

      if (buffer.getInt(8) != 13) {
         throw new IOException("Bad length for IHDR chunk!");
      }

      if (buffer.getInt(12) != 1229472850) {
         throw new IOException("Bad type for IHDR chunk!");
      }

      buffer.order(order);
   }
}
