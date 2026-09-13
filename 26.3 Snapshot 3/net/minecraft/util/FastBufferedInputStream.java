package net.minecraft.util;

import java.io.IOException;
import java.io.InputStream;

public class FastBufferedInputStream extends InputStream {
   private static final int DEFAULT_BUFFER_SIZE = 8192;
   private final InputStream in;
   private final byte[] buffer;
   private int limit;
   private int position;

   public FastBufferedInputStream(final InputStream in) {
      this(in, 8192);
   }

   public FastBufferedInputStream(final InputStream in, final int bufferSize) {
      this.in = in;
      this.buffer = new byte[bufferSize];
   }

   @Override
   public int read() throws IOException {
      if (this.position >= this.limit) {
         this.fill();
         if (this.position >= this.limit) {
            return -1;
         }
      }

      return Byte.toUnsignedInt(this.buffer[this.position++]);
   }

   @Override
   public int read(final byte[] output, final int offset, int length) throws IOException {
      int availableInBuffer = this.bytesInBuffer();
      if (availableInBuffer <= 0) {
         if (length >= this.buffer.length) {
            return this.in.read(output, offset, length);
         }

         this.fill();
         availableInBuffer = this.bytesInBuffer();
         if (availableInBuffer <= 0) {
            return -1;
         }
      }

      if (length > availableInBuffer) {
         length = availableInBuffer;
      }

      System.arraycopy(this.buffer, this.position, output, offset, length);
      this.position += length;
      return length;
   }

   @Override
   public long skip(long count) throws IOException {
      if (count <= 0L) {
         return 0L;
      }

      long availableInBuffer = this.bytesInBuffer();
      if (availableInBuffer <= 0L) {
         return this.in.skip(count);
      }

      if (count > availableInBuffer) {
         count = availableInBuffer;
      }

      this.position = (int)(this.position + count);
      return count;
   }

   @Override
   public int available() throws IOException {
      return this.bytesInBuffer() + this.in.available();
   }

   @Override
   public void close() throws IOException {
      this.in.close();
   }

   private int bytesInBuffer() {
      return this.limit - this.position;
   }

   private void fill() throws IOException {
      this.limit = 0;
      this.position = 0;
      int actuallyRead = this.in.read(this.buffer, 0, this.buffer.length);
      if (actuallyRead > 0) {
         this.limit = actuallyRead;
      }
   }
}
