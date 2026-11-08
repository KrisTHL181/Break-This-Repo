package net.minecraft.server.jsonrpc.methods;

public class InvalidParameterJsonRpcException extends RuntimeException {
   public InvalidParameterJsonRpcException(final String message) {
      super(message);
   }
}
