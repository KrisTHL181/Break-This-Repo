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

   public Varint21FrameDecoder(@Nullable BandwidthDebugMonitor p_297525_) {
      this.monitor = p_297525_;
   }

   protected void handlerRemoved0(ChannelHandlerContext p_299287_) {
      this.helperBuf.release();
   }

   private static boolean copyVarint(ByteBuf p_299967_, ByteBuf p_298224_) {
      for (int i = 0; i < 3; i++) {
         if (!p_299967_.isReadable()) {
            return false;
         }

         byte b0 = p_299967_.readByte();
         p_298224_.writeByte(b0);
         if (!VarInt.hasContinuationBit(b0)) {
            return true;
         }
      }

      throw new CorruptedFrameException("length wider than 21-bit");
   }

   protected void decode(ChannelHandlerContext p_130566_, ByteBuf p_130567_, List<Object> p_130568_) {
      p_130567_.markReaderIndex();
      this.helperBuf.clear();
      if (!copyVarint(p_130567_, this.helperBuf)) {
         p_130567_.resetReaderIndex();
      } else {
         int i = VarInt.read(this.helperBuf);
         if (i == 0) {
            throw new CorruptedFrameException("Frame length cannot be zero");
         }

         if (p_130567_.readableBytes() < i) {
            p_130567_.resetReaderIndex();
         } else {
            if (this.monitor != null) {
               this.monitor.onReceive(i + VarInt.getByteSize(i));
            }

            p_130568_.add(p_130567_.readBytes(i));
         }
      }
   }
}
