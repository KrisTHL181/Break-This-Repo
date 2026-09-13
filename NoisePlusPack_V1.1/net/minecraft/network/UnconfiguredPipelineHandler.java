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
   public static <T extends PacketListener> UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundProtocol(ProtocolInfo<T> p_335707_) {
      return setupInboundHandler(new PacketDecoder<T>(p_335707_));
   }

   private static UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundHandler(ChannelInboundHandler p_333903_) {
      return p_331657_ -> {
         p_331657_.pipeline().replace(p_331657_.name(), "decoder", p_333903_);
         p_331657_.channel().config().setAutoRead(true);
      };
   }

   public static <T extends PacketListener> UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundProtocol(ProtocolInfo<T> p_332375_) {
      return setupOutboundHandler(new PacketEncoder<T>(p_332375_));
   }

   private static UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundHandler(ChannelOutboundHandler p_327845_) {
      return p_329768_ -> p_329768_.pipeline().replace(p_329768_.name(), "encoder", p_327845_);
   }

   public static class Inbound extends ChannelDuplexHandler {
      public void channelRead(ChannelHandlerContext p_333162_, Object p_330291_) {
         if (!(p_330291_ instanceof ByteBuf) && !(p_330291_ instanceof Packet)) {
            p_333162_.fireChannelRead(p_330291_);
         } else {
            ReferenceCountUtil.release(p_330291_);
            throw new DecoderException("Pipeline has no inbound protocol configured, can't process packet " + p_330291_);
         }
      }

      public void write(ChannelHandlerContext p_335998_, Object p_335040_, ChannelPromise p_328870_) throws Exception {
         if (p_335040_ instanceof UnconfiguredPipelineHandler.InboundConfigurationTask unconfiguredpipelinehandler$inboundconfigurationtask) {
            try {
               unconfiguredpipelinehandler$inboundconfigurationtask.run(p_335998_);
            } finally {
               ReferenceCountUtil.release(p_335040_);
            }

            p_328870_.setSuccess();
         } else {
            p_335998_.write(p_335040_, p_328870_);
         }
      }
   }

   @FunctionalInterface
   public interface InboundConfigurationTask {
      void run(ChannelHandlerContext var1);

      default UnconfiguredPipelineHandler.InboundConfigurationTask andThen(UnconfiguredPipelineHandler.InboundConfigurationTask p_332325_) {
         return p_334974_ -> {
            this.run(p_334974_);
            p_332325_.run(p_334974_);
         };
      }
   }

   public static class Outbound extends ChannelOutboundHandlerAdapter {
      public void write(ChannelHandlerContext p_331750_, Object p_329073_, ChannelPromise p_329104_) throws Exception {
         if (p_329073_ instanceof Packet) {
            ReferenceCountUtil.release(p_329073_);
            throw new EncoderException("Pipeline has no outbound protocol configured, can't process packet " + p_329073_);
         }

         if (p_329073_ instanceof UnconfiguredPipelineHandler.OutboundConfigurationTask unconfiguredpipelinehandler$outboundconfigurationtask) {
            try {
               unconfiguredpipelinehandler$outboundconfigurationtask.run(p_331750_);
            } finally {
               ReferenceCountUtil.release(p_329073_);
            }

            p_329104_.setSuccess();
         } else {
            p_331750_.write(p_329073_, p_329104_);
         }
      }
   }

   @FunctionalInterface
   public interface OutboundConfigurationTask {
      void run(ChannelHandlerContext var1);

      default UnconfiguredPipelineHandler.OutboundConfigurationTask andThen(UnconfiguredPipelineHandler.OutboundConfigurationTask p_334721_) {
         return p_334875_ -> {
            this.run(p_334875_);
            p_334721_.run(p_334875_);
         };
      }
   }
}
