package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MonitoredLocalFrameDecoder extends ChannelInboundHandlerAdapter {
   private final BandwidthDebugMonitor monitor;

   public MonitoredLocalFrameDecoder(BandwidthDebugMonitor p_377665_) {
      this.monitor = p_377665_;
   }

   public void channelRead(ChannelHandlerContext p_377429_, Object p_375529_) {
      p_375529_ = HiddenByteBuf.unpack(p_375529_);
      if (p_375529_ instanceof ByteBuf bytebuf) {
         this.monitor.onReceive(bytebuf.readableBytes());
      }

      p_377429_.fireChannelRead(p_375529_);
   }
}
