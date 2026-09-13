package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class Varint21FrameDecoder extends ByteToMessageDecoder {
   private static final int MAX_VARINT21_BYTES = 3;
   private final ByteBuf helperBuf = Unpooled.directBuffer(3);
   private final @Nullable BandwidthDebugMonitor monitor;

   public Varint21FrameDecoder(final @Nullable BandwidthDebugMonitor monitor) {
      this.monitor = monitor;
   }

   protected void handlerRemoved0(final ChannelHandlerContext ctx) {
      this.helperBuf.release();
   }

   private static boolean copyVarint(final ByteBuf in, final ByteBuf out) {
      for (int i = 0; i < 3; i++) {
         if (!in.isReadable()) {
            return false;
         }

         byte b = in.readByte();
         out.writeByte(b);
         if (!VarInt.hasContinuationBit(b)) {
            return true;
         }
      }

      throw new CorruptedFrameException("length wider than 21-bit");
   }

   protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
      in.markReaderIndex();
      this.helperBuf.clear();
      if (!copyVarint(in, this.helperBuf)) {
         in.resetReaderIndex();
      } else {
         int length = VarInt.read(this.helperBuf);
         if (length == 0) {
            throw new CorruptedFrameException("Frame length cannot be zero");
         }

         if (in.readableBytes() < length) {
            in.resetReaderIndex();
         } else {
            if (this.monitor != null) {
               this.monitor.onReceive(length + VarInt.getByteSize(length));
            }

            out.add(in.readBytes(length));
         }
      }
   }
}
