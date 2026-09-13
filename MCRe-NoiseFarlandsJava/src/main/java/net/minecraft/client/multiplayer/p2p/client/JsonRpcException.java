package net.minecraft.client.multiplayer.p2p.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public final class JsonRpcException extends RuntimeException {
   private final int code;
   private final String serverMessage;
   private final @Nullable JsonElement data;

   public JsonRpcException(final int code, final String message, final @Nullable JsonElement data) {
      super("JSON-RPC error " + code + ": " + message);
      this.code = code;
      this.serverMessage = message;
      this.data = data;
   }

   public int code() {
      return this.code;
   }

   public @Nullable JsonElement data() {
      return this.data;
   }

   public String serverMessage() {
      return this.serverMessage;
   }

   public @Nullable String dataCode() {
      if (this.data != null && this.data.isJsonObject()) {
         JsonObject obj = this.data.getAsJsonObject();
         JsonElement c = obj.get("Code");
         return c != null && c.isJsonPrimitive() ? c.getAsString() : null;
      } else {
         return null;
      }
   }
}
