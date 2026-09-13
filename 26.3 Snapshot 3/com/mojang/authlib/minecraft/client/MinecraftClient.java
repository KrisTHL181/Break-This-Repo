package com.mojang.authlib.minecraft.client;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.services.response.ErrorResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftClient {
   public static final int CONNECT_TIMEOUT_MS = 5000;
   public static final int READ_TIMEOUT_MS = 5000;
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftClient.class);
   private static final int HTTP_NOT_MODIFIED = 304;
   @Nullable
   private final String accessToken;
   private final Proxy proxy;
   private final ObjectMapper objectMapper = ObjectMapper.create();

   public MinecraftClient(@Nullable String accessToken, Proxy proxy) {
      this.accessToken = accessToken;
      this.proxy = (Proxy)Validate.notNull(proxy);
   }

   public static MinecraftClient unauthenticated(Proxy proxy) {
      return new MinecraftClient(null, proxy);
   }

   @Nullable
   private static Duration parseRetryAfter(@Nullable String headerValue) {
      if (headerValue == null) {
         return null;
      }

      try {
         long seconds = Long.parseLong(headerValue.trim());
         return seconds > 0L ? Duration.ofSeconds(seconds) : null;
      } catch (NumberFormatException e) {
         LOGGER.debug("Ignoring malformed {} header: {}", "Retry-After", headerValue);
         return null;
      }
   }

   @Nullable
   public <T> T get(URL url, Class<T> responseClass) {
      return this.getWithEtag(url, responseClass, null).body();
   }

   public <T> MinecraftClient.ServiceResponse<T> getWithEtag(URL url, Class<T> responseClass, @Nullable String cachedEtag) {
      Validate.notNull(url);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.prepareRequest(url, cachedEtag);
      return this.readServiceResponse(url, responseClass, connection, cachedEtag);
   }

   @Nullable
   public <T> T post(URL url, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.withBody(this.prepareRequest(url, null), "POST", new byte[0]);
      return this.readServiceResponse(url, responseClass, connection, null).body();
   }

   @Nullable
   public <T> T post(URL url, Object body, Class<T> responseClass) {
      return this.postWithEtag(url, body, responseClass, null).body();
   }

   public <T> MinecraftClient.ServiceResponse<T> postWithEtag(URL url, Object body, Class<T> responseClass, @Nullable String cachedEtag) {
      Validate.notNull(url);
      Validate.notNull(body);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.withBody(this.prepareRequest(url, cachedEtag), "POST", this.serialize(body));
      return this.readServiceResponse(url, responseClass, connection, cachedEtag);
   }

   @Nullable
   public <T> T put(URL url, Object body, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(body);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.withBody(this.prepareRequest(url, null), "PUT", this.serialize(body));
      return this.readServiceResponse(url, responseClass, connection, null).body();
   }

   @Nullable
   public <T> T delete(URL url, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.prepareRequest(url, null);

      try {
         connection.setRequestMethod("DELETE");
      } catch (IOException io) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed to DELETE " + url, io);
      }

      return this.readServiceResponse(url, responseClass, connection, null).body();
   }

   private <T> MinecraftClient.ServiceResponse<T> readServiceResponse(URL url, Class<T> clazz, HttpURLConnection connection, @Nullable String cachedEtag) {
      InputStream inputStream = null;

      try {
         int status = connection.getResponseCode();
         Duration retryAfter = parseRetryAfter(connection.getHeaderField("Retry-After"));
         if (status == 304) {
            return new MinecraftClient.ServiceResponse<>(null, cachedEtag, retryAfter);
         } else {
            String responseEtag = connection.getHeaderField("ETag");
            if (status < 400) {
               inputStream = connection.getInputStream();
               String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
               return !result.isEmpty() && clazz != Void.class
                  ? new MinecraftClient.ServiceResponse<>(this.objectMapper.readValue(result, clazz), responseEtag, retryAfter)
                  : new MinecraftClient.ServiceResponse<>(null, responseEtag, retryAfter);
            } else {
               throw this.buildHttpException(url, connection, status, retryAfter);
            }
         }
      } catch (IOException e) {
         throw new MinecraftClientException(
            MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed to read from " + url + " due to " + e.getMessage(), e
         );
      } finally {
         IOUtils.closeQuietly(inputStream);
      }
   }

   private MinecraftClientHttpException buildHttpException(URL url, HttpURLConnection connection, int status, @Nullable Duration retryAfter) throws IOException {
      InputStream errorStream = connection.getErrorStream();
      if (errorStream == null) {
         return new MinecraftClientHttpException(status, null, retryAfter);
      }

      try {
         String contentType = connection.getContentType();
         String result = IOUtils.toString(errorStream, StandardCharsets.UTF_8);
         if (contentType != null && contentType.startsWith("text/html")) {
            LOGGER.error("Got an error with a html body connecting to {}: {}", url, result);
            return new MinecraftClientHttpException(status, null, retryAfter);
         } else {
            ErrorResponse errorResponse = this.objectMapper.readValue(result, ErrorResponse.class);
            return new MinecraftClientHttpException(status, errorResponse, retryAfter);
         }
      } finally {
         IOUtils.closeQuietly(errorStream);
      }
   }

   private HttpURLConnection prepareRequest(URL url, @Nullable String cachedEtag) {
      HttpURLConnection connection = this.createUrlConnection(url);
      if (this.accessToken != null) {
         connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
      }

      if (cachedEtag != null) {
         connection.setRequestProperty("If-None-Match", cachedEtag);
      }

      return connection;
   }

   private HttpURLConnection withBody(HttpURLConnection connection, String method, byte[] body) {
      OutputStream outputStream = null;

      try {
         connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
         connection.setRequestProperty("Content-Length", Integer.toString(body.length));
         connection.setRequestMethod(method);
         connection.setDoOutput(true);
         outputStream = connection.getOutputStream();
         IOUtils.write(body, outputStream);
      } catch (IOException io) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed to " + method + " " + connection.getURL(), io);
      } finally {
         IOUtils.closeQuietly(outputStream);
      }

      return connection;
   }

   private byte[] serialize(Object body) {
      return this.objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
   }

   private HttpURLConnection createUrlConnection(URL url) {
      try {
         LOGGER.debug("Connecting to {}", url);
         HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(5000);
         connection.setUseCaches(false);
         return connection;
      } catch (IOException io) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed connecting to " + url, io);
      }
   }

   public record ServiceResponse<T>(@Nullable T body, @Nullable String etag, @Nullable Duration retryAfter) {
   }
}
