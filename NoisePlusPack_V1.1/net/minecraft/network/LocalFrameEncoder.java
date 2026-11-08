package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class LocalFrameEncoder extends ChannelOutboundHandlerAdapter {
   public void write(ChannelHandlerContext p_376273_, Object p_376176_, ChannelPromise p_376593_) {
      p_376273_.write(HiddenByteBuf.pack(p_376176_), p_376593_);
   }
}
