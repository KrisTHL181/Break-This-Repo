package net.minecraft.client.server;

import net.minecraft.util.Util;

public class LanServer {
   private final String motd;
   private final String address;
   private long pingTime;

   public LanServer(final String motd, final String address) {
      this.motd = motd;
      this.address = address;
      this.pingTime = Util.getMillis();
   }

   public String getMotd() {
      return this.motd;
   }

   public String getAddress() {
      return this.address;
   }

   public void updatePingTime() {
      this.pingTime = Util.getMillis();
   }
}
