package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record UploadInfo(boolean worldClosed, @Nullable String token, URI uploadEndpoint) {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_SCHEMA = "http://";
   private static final int DEFAULT_PORT = 8080;
   private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");

   public static @Nullable UploadInfo parse(final String json) {
      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         String endpointStr = JsonUtils.getStringOr("uploadEndpoint", jsonObject, null);
         if (endpointStr != null) {
            int endpointPort = JsonUtils.getIntOr("port", jsonObject, -1);
            URI uploadEndpoint = assembleUri(endpointStr, endpointPort);
            if (uploadEndpoint != null) {
               boolean worldClosed = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
               String token = JsonUtils.getStringOr("token", jsonObject, null);
               return new UploadInfo(worldClosed, token, uploadEndpoint);
            }
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse UploadInfo", e);
      }

      return null;
   }

   @VisibleForTesting
   public static @Nullable URI assembleUri(final String endpoint, final int portOverride) {
      Matcher matcher = URI_SCHEMA_PATTERN.matcher(endpoint);
      String endpointWithSchema = ensureEndpointSchema(endpoint, matcher);

      try {
         URI result = new URI(endpointWithSchema);
         int selectedPort = selectPortOrDefault(portOverride, result.getPort());
         return selectedPort != result.getPort()
            ? new URI(result.getScheme(), result.getUserInfo(), result.getHost(), selectedPort, result.getPath(), result.getQuery(), result.getFragment())
            : result;
      } catch (URISyntaxException e) {
         LOGGER.warn("Failed to parse URI {}", endpointWithSchema, e);
         return null;
      }
   }

   private static int selectPortOrDefault(final int portOverride, final int parsedPort) {
      if (portOverride != -1) {
         return portOverride;
      } else {
         return parsedPort != -1 ? parsedPort : 8080;
      }
   }

   private static String ensureEndpointSchema(final String endpoint, final Matcher matcher) {
      return matcher.find() ? endpoint : "http://" + endpoint;
   }

   public static String createRequest(final @Nullable String uploadToken) {
      JsonObject request = new JsonObject();
      if (uploadToken != null) {
         request.addProperty("token", uploadToken);
      }

      return request.toString();
   }
}
