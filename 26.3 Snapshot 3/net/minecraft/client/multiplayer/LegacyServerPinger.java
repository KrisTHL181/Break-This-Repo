package net.minecraft.client.multiplayer;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.LegacyProtocolUtils;
import net.minecraft.util.Mth;

public class LegacyServerPinger extends SimpleChannelInboundHandler<ByteBuf> {
   private static final Splitter SPLITTER = Splitter.on('\u0000').limit(6);
   private final ServerAddress address;
   private final LegacyServerPinger.Output output;

   public LegacyServerPinger(final ServerAddress address, final LegacyServerPinger.Output output) {
      this.address = address;
      this.output = output;
   }

   public void channelActive(final ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      ByteBuf toSend = ctx.alloc().buffer();

      try {
         toSend.writeByte(254);
         toSend.writeByte(1);
         toSend.writeByte(250);
         LegacyProtocolUtils.writeLegacyString(toSend, "MC|PingHost");
         int sizeIndex = toSend.writerIndex();
         toSend.writeShort(0);
         int payloadStart = toSend.writerIndex();
         toSend.writeByte(127);
         LegacyProtocolUtils.writeLegacyString(toSend, this.address.getHost());
         toSend.writeInt(this.address.getPort());
         int payloadSize = toSend.writerIndex() - payloadStart;
         toSend.setShort(sizeIndex, payloadSize);
         ctx.channel().writeAndFlush(toSend).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      } catch (Exception e) {
         toSend.release();
         throw e;
      }
   }

   protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
      short firstByte = msg.readUnsignedByte();
      if (firstByte == 255) {
         String str = LegacyProtocolUtils.readLegacyString(msg);
         List<String> split = SPLITTER.splitToList(str);
         if ("§1".equals(split.get(0))) {
            int protocolVersion = Mth.getInt(split.get(1), 0);
            String version = split.get(2);
            String motd = split.get(3);
            int curPlayers = Mth.getInt(split.get(4), -1);
            int maxPlayers = Mth.getInt(split.get(5), -1);
            this.output.handleResponse(protocolVersion, version, motd, curPlayers, maxPlayers);
         }
      }

      ctx.close();
   }

   public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      ctx.close();
   }

   @FunctionalInterface
   public interface Output {
      void handleResponse(int protocolVersion, String gameVersion, String motd, int players, int maxPlayers);
   }
}
