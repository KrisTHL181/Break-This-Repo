package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public enum JsonRPCErrors {
   PARSE_ERROR(-32700, "Parse error"),
   INVALID_REQUEST(-32600, "Invalid Request"),
   METHOD_NOT_FOUND(-32601, "Method not found"),
   INVALID_PARAMS(-32602, "Invalid params"),
   INTERNAL_ERROR(-32603, "Internal error");

   private final int errorCode;
   private final String message;

   JsonRPCErrors(final int p_424602_, final String p_422906_) {
      this.errorCode = p_424602_;
      this.message = p_422906_;
   }

   public JsonObject createWithUnknownId(@Nullable String p_430166_) {
      return JsonRPCUtils.createError(JsonNull.INSTANCE, this.message, this.errorCode, p_430166_);
   }

   public JsonObject createWithoutData(JsonElement p_424440_) {
      return JsonRPCUtils.createError(p_424440_, this.message, this.errorCode, null);
   }

   public JsonObject create(JsonElement p_427984_, String p_430106_) {
      return JsonRPCUtils.createError(p_427984_, this.message, this.errorCode, p_430106_);
   }
}
