package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LocalFrameDecoder extends ChannelInboundHandlerAdapter {
   public void channelRead(ChannelHandlerContext p_376807_, Object p_376448_) {
      p_376807_.fireChannelRead(HiddenByteBuf.unpack(p_376448_));
   }
}
