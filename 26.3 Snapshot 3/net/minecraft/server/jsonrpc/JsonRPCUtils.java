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

   public static JsonObject createSuccessResult(final JsonElement id, final JsonElement result) {
      JsonObject response = new JsonObject();
      response.addProperty("jsonrpc", "2.0");
      response.add("id", id);
      response.add("result", result);
      return response;
   }

   public static JsonObject createRequest(final @Nullable Integer id, final Identifier method, final List<JsonElement> params) {
      return createRequest(id, method.toString(), params);
   }

   public static JsonObject createRequest(final @Nullable Integer id, final String method, final List<JsonElement> params) {
      JsonObject request = new JsonObject();
      request.addProperty("jsonrpc", "2.0");
      if (id != null) {
         request.addProperty("id", id);
      }

      request.addProperty("method", method);
      if (!params.isEmpty()) {
         JsonArray jsonArray = new JsonArray(params.size());

         for (JsonElement param : params) {
            jsonArray.add(param);
         }

         request.add("params", jsonArray);
      }

      return request;
   }

   public static JsonObject createError(final JsonElement id, final String message, final int errorCode, final @Nullable String data) {
      JsonObject errorResponse = new JsonObject();
      errorResponse.addProperty("jsonrpc", "2.0");
      errorResponse.add("id", id);
      JsonObject error = new JsonObject();
      error.addProperty("code", errorCode);
      error.addProperty("message", message);
      if (data != null && !data.isBlank()) {
         error.addProperty("data", data);
      }

      errorResponse.add("error", error);
      return errorResponse;
   }

   public static @Nullable JsonElement getRequestId(final JsonObject jsonObject) {
      return jsonObject.get("id");
   }

   public static @Nullable String getMethodName(final JsonObject jsonObject) {
      return GsonHelper.getAsString(jsonObject, "method", null);
   }

   public static @Nullable JsonElement getParams(final JsonObject jsonObject) {
      return jsonObject.get("params");
   }

   public static @Nullable JsonElement getResult(final JsonObject jsonObject) {
      return jsonObject.get("result");
   }

   public static @Nullable JsonObject getError(final JsonObject jsonObject) {
      return GsonHelper.getAsJsonObject(jsonObject, "error", null);
   }
}
