package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketDecoder<T extends PacketListener> extends ByteToMessageDecoder implements ProtocolSwapHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ProtocolInfo<T> protocolInfo;

   public PacketDecoder(ProtocolInfo<T> p_336253_) {
      this.protocolInfo = p_336253_;
   }

   protected void decode(ChannelHandlerContext p_130535_, ByteBuf p_130536_, List<Object> p_130537_) throws Exception {
      int i = p_130536_.readableBytes();

      Packet<? super T> packet;
      try {
         packet = this.protocolInfo.codec().decode(p_130536_);
      } catch (Exception exception) {
         if (exception instanceof SkipPacketException) {
            p_130536_.skipBytes(p_130536_.readableBytes());
         }

         throw exception;
      }

      PacketType<? extends Packet<? super T>> packettype = packet.type();
      JvmProfiler.INSTANCE.onPacketReceived(this.protocolInfo.id(), packettype, p_130535_.channel().remoteAddress(), i);
      if (p_130536_.readableBytes() > 0) {
         throw new IOException(
            "Packet "
               + this.protocolInfo.id().id()
               + "/"
               + packettype
               + " ("
               + packet.getClass().getSimpleName()
               + ") was larger than I expected, found "
               + p_130536_.readableBytes()
               + " bytes extra whilst reading packet "
               + packettype
         );
      }

      p_130537_.add(packet);
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug(
            Connection.PACKET_RECEIVED_MARKER,
            " IN: [{}:{}] {} -> {} bytes",
            new Object[]{this.protocolInfo.id().id(), packettype, packet.getClass().getName(), i}
         );
      }

      ProtocolSwapHandler.handleInboundTerminalPacket(p_130535_, packet);
   }
}
