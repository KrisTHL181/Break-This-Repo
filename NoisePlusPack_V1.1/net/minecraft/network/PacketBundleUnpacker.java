package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundleUnpacker extends MessageToMessageEncoder<Packet<?>> {
   private final BundlerInfo bundlerInfo;

   public PacketBundleUnpacker(BundlerInfo p_335271_) {
      this.bundlerInfo = p_335271_;
   }

   protected void encode(ChannelHandlerContext p_265691_, Packet<?> p_265038_, List<Object> p_265735_) throws Exception {
      this.bundlerInfo.unbundlePacket(p_265038_, p_265735_::add);
      if (p_265038_.isTerminal()) {
         p_265691_.pipeline().remove(p_265691_.name());
      }
   }
}
