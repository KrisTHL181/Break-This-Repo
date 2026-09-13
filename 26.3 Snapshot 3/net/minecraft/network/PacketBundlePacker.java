package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;

public class PacketBundlePacker extends MessageToMessageDecoder<Packet<?>> {
   private final BundlerInfo bundlerInfo;
   private BundlerInfo.@Nullable Bundler currentBundler;

   public PacketBundlePacker(final BundlerInfo bundlerInfo) {
      this.bundlerInfo = bundlerInfo;
   }

   protected void decode(final ChannelHandlerContext ctx, final Packet<?> msg, final List<Object> out) throws Exception {
      if (this.currentBundler != null) {
         verifyNonTerminalPacket(msg);
         Packet<?> bundlePacket = this.currentBundler.addPacket(msg);
         if (bundlePacket != null) {
            this.currentBundler = null;
            out.add(bundlePacket);
         }
      } else {
         BundlerInfo.Bundler bundler = this.bundlerInfo.startPacketBundling(msg);
         if (bundler != null) {
            verifyNonTerminalPacket(msg);
            this.currentBundler = bundler;
         } else {
            out.add(msg);
            if (msg.isTerminal()) {
               ctx.pipeline().remove(ctx.name());
            }
         }
      }
   }

   private static void verifyNonTerminalPacket(final Packet<?> msg) {
      if (msg.isTerminal()) {
         throw new DecoderException("Terminal message received in bundle");
      }
   }
}
