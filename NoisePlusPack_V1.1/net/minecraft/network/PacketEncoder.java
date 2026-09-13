package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketEncoder<T extends PacketListener> extends MessageToByteEncoder<Packet<T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ProtocolInfo<T> protocolInfo;

   public PacketEncoder(ProtocolInfo<T> p_327768_) {
      this.protocolInfo = p_327768_;
   }

   protected void encode(ChannelHandlerContext p_130545_, Packet<T> p_130546_, ByteBuf p_130547_) throws Exception {
      PacketType<? extends Packet<? super T>> packettype = p_130546_.type();

      try {
         this.protocolInfo.codec().encode(p_130547_, p_130546_);
         int i = p_130547_.readableBytes();
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
               Connection.PACKET_SENT_MARKER,
               "OUT: [{}:{}] {} -> {} bytes",
               new Object[]{this.protocolInfo.id().id(), packettype, p_130546_.getClass().getName(), i}
            );
         }

         JvmProfiler.INSTANCE.onPacketSent(this.protocolInfo.id(), packettype, p_130545_.channel().remoteAddress(), i);
      } catch (Throwable throwable) {
         LOGGER.error("Error sending packet {}", packettype, throwable);
         if (p_130546_.isSkippable()) {
            throw new SkipPacketEncoderException(throwable);
         }

         throw throwable;
      } finally {
         ProtocolSwapHandler.handleOutboundTerminalPacket(p_130545_, p_130546_);
      }
   }
}
