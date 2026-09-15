package net.minecraft.server.jsonrpc.methods;

public class InvalidRequestJsonRpcException extends RuntimeException {
   public InvalidRequestJsonRpcException(final String message) {
      super(message);
   }
}
