package com.mojang.realmsclient.client;

import java.net.Proxy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsClientConfig {
   private static @Nullable Proxy proxy;

   public static @Nullable Proxy getProxy() {
      return proxy;
   }

   public static void setProxy(Proxy p_87294_) {
      if (proxy == null) {
         proxy = p_87294_;
      }
   }
}
