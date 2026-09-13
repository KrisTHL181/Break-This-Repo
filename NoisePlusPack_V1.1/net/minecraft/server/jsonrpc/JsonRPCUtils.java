package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class JsonRPCUtils {
   public static final String JSON_RPC_VERSION = "2.0";
   public static final String OPEN_RPC_VERSION = "1.3.2";

   public static JsonObject createSuccessResult(JsonElement p_422388_, JsonElement p_430381_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      jsonobject.add("id", p_422388_);
      jsonobject.add("result", p_430381_);
      return jsonobject;
   }

   public static JsonObject createRequest(@Nullable Integer p_424199_, Identifier p_451799_, List<JsonElement> p_429260_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      if (p_424199_ != null) {
         jsonobject.addProperty("id", p_424199_);
      }

      jsonobject.addProperty("method", p_451799_.toString());
      if (!p_429260_.isEmpty()) {
         JsonArray jsonarray = new JsonArray(p_429260_.size());

         for (JsonElement jsonelement : p_429260_) {
            jsonarray.add(jsonelement);
         }

         jsonobject.add("params", jsonarray);
      }

      return jsonobject;
   }

   public static JsonObject createError(JsonElement p_423951_, String p_428093_, int p_430423_, @Nullable String p_427250_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      jsonobject.add("id", p_423951_);
      JsonObject jsonobject1 = new JsonObject();
      jsonobject1.addProperty("code", p_430423_);
      jsonobject1.addProperty("message", p_428093_);
      if (p_427250_ != null && !p_427250_.isBlank()) {
         jsonobject1.addProperty("data", p_427250_);
      }

      jsonobject.add("error", jsonobject1);
      return jsonobject;
   }

   public static @Nullable JsonElement getRequestId(JsonObject p_427698_) {
      return p_427698_.get("id");
   }

   public static @Nullable String getMethodName(JsonObject p_424573_) {
      return GsonHelper.getAsString(p_424573_, "method", null);
   }

   public static @Nullable JsonElement getParams(JsonObject p_423352_) {
      return p_423352_.get("params");
   }

   public static @Nullable JsonElement getResult(JsonObject p_422775_) {
      return p_422775_.get("result");
   }

   public static @Nullable JsonObject getError(JsonObject p_425008_) {
      return GsonHelper.getAsJsonObject(p_425008_, "error", null);
   }
}
