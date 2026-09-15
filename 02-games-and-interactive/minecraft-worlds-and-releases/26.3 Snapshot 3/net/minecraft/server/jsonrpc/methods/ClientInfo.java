package net.minecraft.server.jsonrpc.methods;

public record ClientInfo(Integer connectionId) {
   public static ClientInfo of(final Integer connectionId) {
      return new ClientInfo(connectionId);
   }
}
