package com.mojang.realmsclient.client;

import java.net.Proxy;
import org.jspecify.annotations.Nullable;

public class RealmsClientConfig {
   private static @Nullable Proxy proxy;

   public static @Nullable Proxy getProxy() {
      return proxy;
   }

   public static void setProxy(final Proxy proxy) {
      if (RealmsClientConfig.proxy == null) {
         RealmsClientConfig.proxy = proxy;
      }
   }
}
