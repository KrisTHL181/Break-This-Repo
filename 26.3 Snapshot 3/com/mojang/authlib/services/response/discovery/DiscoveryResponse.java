package com.mojang.authlib.services.response.discovery;

import java.beans.Transient;

public record DiscoveryResponse(String environment, String product, Discovery discovery) {
   private static final String OFFLINE = "offline";

   @Transient
   public boolean isOffline() {
      return "offline".equals(this.environment);
   }

   public static DiscoveryResponse offline() {
      return new DiscoveryResponse("offline", "minecraft", Discovery.offline());
   }
}
