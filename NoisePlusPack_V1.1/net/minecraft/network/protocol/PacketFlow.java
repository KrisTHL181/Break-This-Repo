package net.minecraft.network.protocol;

public enum PacketFlow {
   SERVERBOUND("serverbound"),
   CLIENTBOUND("clientbound");

   private final String id;

   PacketFlow(final String p_330878_) {
      this.id = p_330878_;
   }

   public PacketFlow getOpposite() {
      return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
   }

   public String id() {
      return this.id;
   }
}
