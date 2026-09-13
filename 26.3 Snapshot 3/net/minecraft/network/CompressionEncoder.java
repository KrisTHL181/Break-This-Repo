package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;

public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
   private final byte[] encodeBuf = new byte[8192];
   private final Deflater deflater;
   private int threshold;

   public CompressionEncoder(final int threshold) {
      this.threshold = threshold;
      this.deflater = new Deflater();
   }

   protected void encode(final ChannelHandlerContext ctx, final ByteBuf uncompressed, final ByteBuf out) {
      int uncompressedLength = uncompressed.readableBytes();
      if (uncompressedLength > 8388608) {
         throw new IllegalArgumentException("Packet too big (is " + uncompressedLength + ", should be less than 8388608)");
      }

      if (uncompressedLength < this.threshold) {
         VarInt.write(out, 0);
         out.writeBytes(uncompressed);
      } else {
         byte[] input = new byte[uncompressedLength];
         uncompressed.readBytes(input);
         VarInt.write(out, input.length);
         this.deflater.setInput(input, 0, uncompressedLength);
         this.deflater.finish();

         while (!this.deflater.finished()) {
            int written = this.deflater.deflate(this.encodeBuf);
            out.writeBytes(this.encodeBuf, 0, written);
         }

         this.deflater.reset();
      }
   }

   public int getThreshold() {
      return this.threshold;
   }

   public void setThreshold(final int threshold) {
      this.threshold = threshold;
   }
}
