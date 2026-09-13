package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
   public static final int MAX_VARINT21_BYTES = 3;

   protected void encode(ChannelHandlerContext p_130571_, ByteBuf p_130572_, ByteBuf p_130573_) {
      int i = p_130572_.readableBytes();
      int j = VarInt.getByteSize(i);
      if (j > 3) {
         throw new EncoderException("Packet too large: size " + i + " is over 8");
      }

      p_130573_.ensureWritable(j + i);
      VarInt.write(p_130573_, i);
      p_130573_.writeBytes(p_130572_, p_130572_.readerIndex(), i);
   }
}
