package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressionDecoder extends ByteToMessageDecoder {
   public static final int MAXIMUM_COMPRESSED_LENGTH = 2097152;
   public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;
   private final Inflater inflater;
   private int threshold;
   private boolean validateDecompressed;

   public CompressionDecoder(final int threshold, final boolean validateDecompressed) {
      this.threshold = threshold;
      this.validateDecompressed = validateDecompressed;
      this.inflater = new Inflater();
   }

   protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
      int uncompressedLength = VarInt.read(in);
      if (uncompressedLength == 0) {
         out.add(in.readBytes(in.readableBytes()));
      } else {
         if (this.validateDecompressed) {
            if (uncompressedLength < this.threshold) {
               throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is below server threshold of " + this.threshold);
            }

            if (uncompressedLength > 8388608) {
               throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is larger than protocol maximum of 8388608");
            }
         }

         this.setupInflaterInput(in);
         ByteBuf output = this.inflate(ctx, uncompressedLength);
         this.inflater.reset();
         out.add(output);
      }
   }

   private void setupInflaterInput(final ByteBuf in) {
      ByteBuffer input;
      if (in.nioBufferCount() > 0) {
         input = in.nioBuffer();
         in.skipBytes(in.readableBytes());
      } else {
         input = ByteBuffer.allocateDirect(in.readableBytes());
         in.readBytes(input);
         input.flip();
      }

      this.inflater.setInput(input);
   }

   private ByteBuf inflate(final ChannelHandlerContext ctx, final int uncompressedLength) throws DataFormatException {
      ByteBuf output = ctx.alloc().directBuffer(uncompressedLength);

      try {
         ByteBuffer nioBuffer = output.internalNioBuffer(0, uncompressedLength);
         int pos = nioBuffer.position();
         this.inflater.inflate(nioBuffer);
         int actualUncompressedLength = nioBuffer.position() - pos;
         if (actualUncompressedLength != uncompressedLength) {
            throw new DecoderException(
               "Badly compressed packet - actual length of uncompressed payload "
                  + actualUncompressedLength
                  + " is does not match declared size "
                  + uncompressedLength
            );
         }

         output.writerIndex(output.writerIndex() + actualUncompressedLength);
         return output;
      } catch (Exception e) {
         output.release();
         throw e;
      }
   }

   public void setThreshold(final int threshold, final boolean validateDecompressed) {
      this.threshold = threshold;
      this.validateDecompressed = validateDecompressed;
   }
}
