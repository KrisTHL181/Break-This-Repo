package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.protocol.Packet;

public class UnconfiguredPipelineHandler {
   public static <T extends PacketListener> UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundProtocol(final ProtocolInfo<T> protocolInfo) {
      return setupInboundHandler(new PacketDecoder<T>(protocolInfo));
   }

   private static UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundHandler(final ChannelInboundHandler newHandler) {
      return ctx -> {
         ctx.pipeline().replace(ctx.name(), "decoder", newHandler);
         ctx.channel().config().setAutoRead(true);
      };
   }

   public static <T extends PacketListener> UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundProtocol(final ProtocolInfo<T> codecData) {
      return setupOutboundHandler(new PacketEncoder<T>(codecData));
   }

   private static UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundHandler(final ChannelOutboundHandler newHandler) {
      return ctx -> ctx.pipeline().replace(ctx.name(), "encoder", newHandler);
   }

   public static class Inbound extends ChannelDuplexHandler {
      public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
         if (!(msg instanceof ByteBuf) && !(msg instanceof Packet)) {
            ctx.fireChannelRead(msg);
         } else {
            ReferenceCountUtil.release(msg);
            throw new DecoderException("Pipeline has no inbound protocol configured, can't process packet " + msg);
         }
      }

      public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
         if (msg instanceof UnconfiguredPipelineHandler.InboundConfigurationTask configurationTask) {
            try {
               configurationTask.run(ctx);
            } finally {
               ReferenceCountUtil.release(msg);
            }

            promise.setSuccess();
         } else {
            ctx.write(msg, promise);
         }
      }
   }

   @FunctionalInterface
   public interface InboundConfigurationTask {
      void run(ChannelHandlerContext ctx);

      default UnconfiguredPipelineHandler.InboundConfigurationTask andThen(final UnconfiguredPipelineHandler.InboundConfigurationTask otherTask) {
         return ctx -> {
            this.run(ctx);
            otherTask.run(ctx);
         };
      }
   }

   public static class Outbound extends ChannelOutboundHandlerAdapter {
      public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
         if (msg instanceof Packet) {
            ReferenceCountUtil.release(msg);
            throw new EncoderException("Pipeline has no outbound protocol configured, can't process packet " + msg);
         }

         if (msg instanceof UnconfiguredPipelineHandler.OutboundConfigurationTask configurationTask) {
            try {
               configurationTask.run(ctx);
            } finally {
               ReferenceCountUtil.release(msg);
            }

            promise.setSuccess();
         } else {
            ctx.write(msg, promise);
         }
      }
   }

   @FunctionalInterface
   public interface OutboundConfigurationTask {
      void run(ChannelHandlerContext ctx);

      default UnconfiguredPipelineHandler.OutboundConfigurationTask andThen(final UnconfiguredPipelineHandler.OutboundConfigurationTask otherTask) {
         return ctx -> {
            this.run(ctx);
            otherTask.run(ctx);
         };
      }
   }
}
