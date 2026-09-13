package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileUpload implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_RETRIES = 5;
   private static final String UPLOAD_PATH = "/upload";
   private final File file;
   private final long realmId;
   private final int slotId;
   private final UploadInfo uploadInfo;
   private final String sessionId;
   private final String username;
   private final String clientVersion;
   private final String worldVersion;
   private final UploadStatus uploadStatus;
   private final HttpClient client;

   public FileUpload(
      final File file,
      final long realmId,
      final int slotId,
      final UploadInfo uploadInfo,
      final User user,
      final String clientVersion,
      final String worldVersion,
      final UploadStatus uploadStatus
   ) {
      this.file = file;
      this.realmId = realmId;
      this.slotId = slotId;
      this.uploadInfo = uploadInfo;
      this.sessionId = user.getSessionId();
      this.username = user.getName();
      this.clientVersion = clientVersion;
      this.worldVersion = worldVersion;
      this.uploadStatus = uploadStatus;
      this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
   }

   @Override
   public void close() {
      this.client.close();
   }

   public CompletableFuture<UploadResult> startUpload() {
      long fileSize = this.file.length();
      this.uploadStatus.setTotalBytes(fileSize);
      return this.requestUpload(0, fileSize);
   }

   private CompletableFuture<UploadResult> requestUpload(final int currentAttempt, final long fileSize) {
      BodyPublisher publisher = inputStreamPublisherWithSize(() -> {
         try {
            return new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
         } catch (IOException e) {
            LOGGER.warn("Failed to open file {}", this.file, e);
            return null;
         }
      }, fileSize);
      HttpRequest request = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId))
         .timeout(Duration.ofMinutes(10L))
         .setHeader("Cookie", this.uploadCookie())
         .setHeader("Content-Type", "application/octet-stream")
         .POST(publisher)
         .build();
      return this.client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(response -> {
         long retryDelaySeconds = this.getRetryDelaySeconds((HttpResponse<?>)response);
         if (this.shouldRetry(retryDelaySeconds, currentAttempt)) {
            this.uploadStatus.restart();

            try {
               Thread.sleep(Duration.ofSeconds(retryDelaySeconds));
            } catch (InterruptedException var8) {
            }

            return this.requestUpload(currentAttempt + 1, fileSize);
         } else {
            return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)response));
         }
      });
   }

   private static BodyPublisher inputStreamPublisherWithSize(final Supplier<@Nullable InputStream> inputStreamSupplier, final long fileSize) {
      return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(inputStreamSupplier), fileSize);
   }

   private String uploadCookie() {
      return "sid="
         + this.sessionId
         + ";token="
         + this.uploadInfo.token()
         + ";user="
         + this.username
         + ";version="
         + this.clientVersion
         + ";worldVersion="
         + this.worldVersion;
   }

   private UploadResult handleResponse(final HttpResponse<String> response) {
      int statusCode = response.statusCode();
      if (statusCode == 401) {
         LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
      }

      String errorMessage = null;
      String body = response.body();
      if (body != null && !body.isBlank()) {
         try {
            JsonElement errorMsgElement = LenientJsonParser.parse(body).getAsJsonObject().get("errorMsg");
            if (errorMsgElement != null) {
               errorMessage = errorMsgElement.getAsString();
            }
         } catch (Exception e) {
            LOGGER.warn("Failed to parse response {}", body, e);
         }
      }

      return new UploadResult(statusCode, errorMessage);
   }

   private boolean shouldRetry(final long retryDelaySeconds, final int currentAttempt) {
      return retryDelaySeconds > 0L && currentAttempt + 1 < 5;
   }

   private long getRetryDelaySeconds(final HttpResponse<?> response) {
      return response.headers().firstValueAsLong("Retry-After").orElse(0L);
   }

   private static class UploadCountingInputStream extends CountingInputStream {
      private final UploadStatus uploadStatus;

      private UploadCountingInputStream(final InputStream proxy, final UploadStatus uploadStatus) {
         super(proxy);
         this.uploadStatus = uploadStatus;
      }

      protected void afterRead(final int n) throws IOException {
         super.afterRead(n);
         this.uploadStatus.onWrite(this.getByteCount());
      }
   }
}
