package net.minecraft.client.multiplayer.resolver;

import java.net.InetSocketAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvedServerAddress {
   String getHostName();

   String getHostIp();

   int getPort();

   InetSocketAddress asInetSocketAddress();

   static ResolvedServerAddress from(final InetSocketAddress p_171846_) {
      return new ResolvedServerAddress() {
         @Override
         public String getHostName() {
            return p_171846_.getAddress().getHostName();
         }

         @Override
         public String getHostIp() {
            return p_171846_.getAddress().getHostAddress();
         }

         @Override
         public int getPort() {
            return p_171846_.getPort();
         }

         @Override
         public InetSocketAddress asInetSocketAddress() {
            return p_171846_;
         }
      };
   }
}
