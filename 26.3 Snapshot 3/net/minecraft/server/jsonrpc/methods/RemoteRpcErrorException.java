package net.minecraft.server.jsonrpc.methods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteRpcErrorException extends RuntimeException {
   private final JsonElement id;
   private final JsonObject error;

   public RemoteRpcErrorException(final JsonElement id, final JsonObject error) {
      this.id = id;
      this.error = error;
   }

   private JsonObject getError() {
      return this.error;
   }

   private JsonElement getId() {
      return this.id;
   }
}
