package net.minecraft.server.jsonrpc.methods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteRpcErrorException extends RuntimeException {
   private final JsonElement id;
   private final JsonObject error;

   public RemoteRpcErrorException(JsonElement p_430547_, JsonObject p_429173_) {
      this.id = p_430547_;
      this.error = p_429173_;
   }

   private JsonObject getError() {
      return this.error;
   }

   private JsonElement getId() {
      return this.id;
   }
}
