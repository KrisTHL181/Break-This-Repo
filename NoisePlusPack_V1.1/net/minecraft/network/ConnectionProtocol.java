package net.minecraft.network;

public enum ConnectionProtocol {
   HANDSHAKING("handshake"),
   PLAY("play"),
   STATUS("status"),
   LOGIN("login"),
   CONFIGURATION("configuration");

   private final String id;

   ConnectionProtocol(final String p_297624_) {
      this.id = p_297624_;
   }

   public String id() {
      return this.id;
   }
}
