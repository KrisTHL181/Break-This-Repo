package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
   static void handleInboundTerminalPacket(final ChannelHandlerContext ctx, final Packet<?> packet) {
      if (packet.isTerminal()) {
         ctx.channel().config().setAutoRead(false);
         ctx.pipeline().addBefore(ctx.name(), "inbound_config", new UnconfiguredPipelineHandler.Inbound());
         ctx.pipeline().remove(ctx.name());
      }
   }

   static void handleOutboundTerminalPacket(final ChannelHandlerContext ctx, final Packet<?> packet) {
      if (packet.isTerminal()) {
         ctx.pipeline().addAfter(ctx.name(), "outbound_config", new UnconfiguredPipelineHandler.Outbound());
         ctx.pipeline().remove(ctx.name());
      }
   }
}
