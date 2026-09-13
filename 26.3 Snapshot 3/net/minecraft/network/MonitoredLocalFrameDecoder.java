package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MonitoredLocalFrameDecoder extends ChannelInboundHandlerAdapter {
   private final BandwidthDebugMonitor monitor;

   public MonitoredLocalFrameDecoder(final BandwidthDebugMonitor monitor) {
      this.monitor = monitor;
   }

   public void channelRead(final ChannelHandlerContext ctx, Object msg) {
      msg = HiddenByteBuf.unpack(msg);
      if (msg instanceof ByteBuf in) {
         this.monitor.onReceive(in.readableBytes());
      }

      ctx.fireChannelRead(msg);
   }
}
