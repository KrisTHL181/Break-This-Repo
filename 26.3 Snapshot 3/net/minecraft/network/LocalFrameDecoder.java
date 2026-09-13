package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LocalFrameDecoder extends ChannelInboundHandlerAdapter {
   public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      ctx.fireChannelRead(HiddenByteBuf.unpack(msg));
   }
}
