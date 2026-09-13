package net.minecraft.server.jsonrpc.methods;

public class MethodNotFoundJsonRpcException extends RuntimeException {
   public MethodNotFoundJsonRpcException(final String message) {
      super(message);
   }
}
