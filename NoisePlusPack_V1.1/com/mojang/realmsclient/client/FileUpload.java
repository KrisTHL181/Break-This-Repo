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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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

   public FileUpload(File p_87071_, long p_87072_, int p_87073_, UploadInfo p_87074_, User p_87075_, String p_87076_, String p_335512_, UploadStatus p_87077_) {
      this.file = p_87071_;
      this.realmId = p_87072_;
      this.slotId = p_87073_;
      this.uploadInfo = p_87074_;
      this.sessionId = p_87075_.getSessionId();
      this.username = p_87075_.getName();
      this.clientVersion = p_87076_;
      this.worldVersion = p_335512_;
      this.uploadStatus = p_87077_;
      this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
   }

   @Override
   public void close() {
      this.client.close();
   }

   public CompletableFuture<UploadResult> startUpload() {
      long i = this.file.length();
      this.uploadStatus.setTotalBytes(i);
      return this.requestUpload(0, i);
   }

   private CompletableFuture<UploadResult> requestUpload(int p_452039_, long p_457801_) {
      BodyPublisher bodypublisher = inputStreamPublisherWithSize(() -> {
         try {
            return new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to open file {}", this.file, ioexception);
            return null;
         }
      }, p_457801_);
      HttpRequest httprequest = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId))
         .timeout(Duration.ofMinutes(10L))
         .setHeader("Cookie", this.uploadCookie())
         .setHeader("Content-Type", "application/octet-stream")
         .POST(bodypublisher)
         .build();
      return this.client.sendAsync(httprequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(p_447737_ -> {
         long i = this.getRetryDelaySeconds((HttpResponse<?>)p_447737_);
         if (this.shouldRetry(i, p_452039_)) {
            this.uploadStatus.restart();

            try {
               Thread.sleep(Duration.ofSeconds(i));
            } catch (InterruptedException var8) {
            }

            return this.requestUpload(p_452039_ + 1, p_457801_);
         } else {
            return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)p_447737_));
         }
      });
   }

   private static BodyPublisher inputStreamPublisherWithSize(Supplier<@Nullable InputStream> p_459948_, long p_458257_) {
      return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(p_459948_), p_458257_);
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

   private UploadResult handleResponse(HttpResponse<String> p_460441_) {
      int i = p_460441_.statusCode();
      if (i == 401) {
         LOGGER.debug("Realms server returned 401: {}", p_460441_.headers().firstValue("WWW-Authenticate"));
      }

      String s = null;
      String s1 = p_460441_.body();
      if (s1 != null && !s1.isBlank()) {
         try {
            JsonElement jsonelement = LenientJsonParser.parse(s1).getAsJsonObject().get("errorMsg");
            if (jsonelement != null) {
               s = jsonelement.getAsString();
            }
         } catch (Exception exception) {
            LOGGER.warn("Failed to parse response {}", s1, exception);
         }
      }

      return new UploadResult(i, s);
   }

   private boolean shouldRetry(long p_87082_, int p_87083_) {
      return p_87082_ > 0L && p_87083_ + 1 < 5;
   }

   private long getRetryDelaySeconds(HttpResponse<?> p_452721_) {
      return p_452721_.headers().firstValueAsLong("Retry-After").orElse(0L);
   }

   @OnlyIn(Dist.CLIENT)
   static class UploadCountingInputStream extends CountingInputStream {
      private final UploadStatus uploadStatus;

      UploadCountingInputStream(InputStream p_454213_, UploadStatus p_454371_) {
         super(p_454213_);
         this.uploadStatus = p_454371_;
      }

      protected void afterRead(int p_454000_) throws IOException {
         super.afterRead(p_454000_);
         this.uploadStatus.onWrite(this.getByteCount());
      }
   }
}
