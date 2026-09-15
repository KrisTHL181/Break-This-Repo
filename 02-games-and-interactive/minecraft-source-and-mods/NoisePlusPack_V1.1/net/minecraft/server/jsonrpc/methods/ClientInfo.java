package net.minecraft.server.jsonrpc.methods;

public record ClientInfo(Integer connectionId) {
   public static ClientInfo of(Integer p_423715_) {
      return new ClientInfo(p_423715_);
   }
}
