package net.minecraft.network;

public enum ConnectionProtocol {
   HANDSHAKING("handshake"),
   PLAY("play"),
   STATUS("status"),
   LOGIN("login"),
   CONFIGURATION("configuration");

   private final String id;

   ConnectionProtocol(final String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }
}
