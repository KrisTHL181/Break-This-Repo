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

   public PacketBundlePacker(BundlerInfo p_333768_) {
      this.bundlerInfo = p_333768_;
   }

   protected void decode(ChannelHandlerContext p_265208_, Packet<?> p_265182_, List<Object> p_265368_) throws Exception {
      if (this.currentBundler != null) {
         verifyNonTerminalPacket(p_265182_);
         Packet<?> packet = this.currentBundler.addPacket(p_265182_);
         if (packet != null) {
            this.currentBundler = null;
            p_265368_.add(packet);
         }
      } else {
         BundlerInfo.Bundler bundlerinfo$bundler = this.bundlerInfo.startPacketBundling(p_265182_);
         if (bundlerinfo$bundler != null) {
            verifyNonTerminalPacket(p_265182_);
            this.currentBundler = bundlerinfo$bundler;
         } else {
            p_265368_.add(p_265182_);
            if (p_265182_.isTerminal()) {
               p_265208_.pipeline().remove(p_265208_.name());
            }
         }
      }
   }

   private static void verifyNonTerminalPacket(Packet<?> p_329638_) {
      if (p_329638_.isTerminal()) {
         throw new DecoderException("Terminal message received in bundle");
      }
   }
}
