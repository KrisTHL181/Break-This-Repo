package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerInfo server;

   public LegacyQueryHandler(final ServerInfo server) {
      this.server = server;
   }

   public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      ByteBuf in = (ByteBuf)msg;
      in.markReaderIndex();
      boolean connectNormally = true;

      try {
         try {
            if (in.readUnsignedByte() != 254) {
               return;
            }

            SocketAddress socket = ctx.channel().remoteAddress();
            int length = in.readableBytes();
            if (length == 0) {
               LOGGER.debug("Ping: (<1.3.x) from {}", socket);
               String body = createVersion0Response(this.server);
               sendFlushAndClose(ctx, createLegacyDisconnectPacket(ctx.alloc(), body));
            } else {
               if (in.readUnsignedByte() != 1) {
                  return;
               }

               if (in.isReadable()) {
                  if (!readCustomPayloadPacket(in)) {
                     return;
                  }

                  LOGGER.debug("Ping: (1.6) from {}", socket);
               } else {
                  LOGGER.debug("Ping: (1.4-1.5.x) from {}", socket);
               }

               String body = createVersion1Response(this.server);
               sendFlushAndClose(ctx, createLegacyDisconnectPacket(ctx.alloc(), body));
            }

            in.release();
            connectNormally = false;
         } catch (RuntimeException var11) {
         }
      } finally {
         if (connectNormally) {
            in.resetReaderIndex();
            ctx.channel().pipeline().remove(this);
            ctx.fireChannelRead(msg);
         }
      }
   }

   private static boolean readCustomPayloadPacket(final ByteBuf in) {
      short packetId = in.readUnsignedByte();
      if (packetId != 250) {
         return false;
      }

      String channelId = LegacyProtocolUtils.readLegacyString(in);
      if (!"MC|PingHost".equals(channelId)) {
         return false;
      }

      int payloadSize = in.readUnsignedShort();
      if (in.readableBytes() != payloadSize) {
         return false;
      }

      short protocolVersion = in.readUnsignedByte();
      if (protocolVersion < 73) {
         return false;
      }

      String host = LegacyProtocolUtils.readLegacyString(in);
      int port = in.readInt();
      return port <= 65535;
   }

   private static String createVersion0Response(final ServerInfo server) {
      return String.format(Locale.ROOT, "%s§%d§%d", server.getMotd(), server.getPlayerCount(), server.getMaxPlayers());
   }

   private static String createVersion1Response(final ServerInfo server) {
      return String.format(
         Locale.ROOT,
         "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
         127,
         server.getServerVersion(),
         server.getMotd(),
         server.getPlayerCount(),
         server.getMaxPlayers()
      );
   }

   private static void sendFlushAndClose(final ChannelHandlerContext ctx, final ByteBuf out) {
      ctx.pipeline().firstContext().writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);
   }

   private static ByteBuf createLegacyDisconnectPacket(final ByteBufAllocator alloc, final String reason) {
      ByteBuf out = alloc.buffer();
      out.writeByte(255);
      LegacyProtocolUtils.writeLegacyString(out, reason);
      return out;
   }
}
