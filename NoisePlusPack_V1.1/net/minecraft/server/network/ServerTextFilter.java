package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ServerTextFilter implements AutoCloseable {
   protected static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
   private static final ThreadFactory THREAD_FACTORY = p_363933_ -> {
      Thread thread = new Thread(p_363933_);
      thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
      return thread;
   };
   private final URL chatEndpoint;
   private final ServerTextFilter.MessageEncoder chatEncoder;
   final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy;
   final ExecutorService workerPool;

   protected static ExecutorService createWorkerPool(int p_360924_) {
      return Executors.newFixedThreadPool(p_360924_, THREAD_FACTORY);
   }

   protected ServerTextFilter(URL p_364797_, ServerTextFilter.MessageEncoder p_361949_, ServerTextFilter.IgnoreStrategy p_370136_, ExecutorService p_369238_) {
      this.chatIgnoreStrategy = p_370136_;
      this.workerPool = p_369238_;
      this.chatEndpoint = p_364797_;
      this.chatEncoder = p_361949_;
   }

   protected static URL getEndpoint(URI p_362612_, @Nullable JsonObject p_362365_, String p_369987_, String p_360990_) throws MalformedURLException {
      String s = getEndpointFromConfig(p_362365_, p_369987_, p_360990_);
      return p_362612_.resolve("/" + s).toURL();
   }

   protected static String getEndpointFromConfig(@Nullable JsonObject p_364044_, String p_360878_, String p_365862_) {
      return p_364044_ != null ? GsonHelper.getAsString(p_364044_, p_360878_, p_365862_) : p_365862_;
   }

   public static @Nullable ServerTextFilter createFromConfig(DedicatedServerProperties p_365465_) {
      String s = p_365465_.textFilteringConfig;
      if (StringUtil.isBlank(s)) {
         return null;
      }

      return switch (p_365465_.textFilteringVersion) {
         case 0 -> LegacyTextFilter.createTextFilterFromConfig(s);
         case 1 -> PlayerSafetyServiceTextFilter.createTextFilterFromConfig(s);
         default -> {
            LOGGER.warn("Could not create text filter - unsupported text filtering version used");
            yield null;
         }
      };
   }

   protected CompletableFuture<FilteredText> requestMessageProcessing(
      GameProfile p_370178_, String p_362939_, ServerTextFilter.IgnoreStrategy p_364336_, Executor p_367327_
   ) {
      return p_362939_.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
         JsonObject jsonobject = this.chatEncoder.encode(p_370178_, p_362939_);

         try {
            JsonObject jsonobject1 = this.processRequestResponse(jsonobject, this.chatEndpoint);
            return this.filterText(p_362939_, p_364336_, jsonobject1);
         } catch (Exception exception) {
            LOGGER.warn("Failed to validate message '{}'", p_362939_, exception);
            return FilteredText.fullyFiltered(p_362939_);
         }
      }, p_367327_);
   }

   protected abstract FilteredText filterText(String var1, ServerTextFilter.IgnoreStrategy var2, JsonObject var3);

   protected FilterMask parseMask(String p_365656_, JsonArray p_365004_, ServerTextFilter.IgnoreStrategy p_366306_) {
      if (p_365004_.isEmpty()) {
         return FilterMask.PASS_THROUGH;
      }

      if (p_366306_.shouldIgnore(p_365656_, p_365004_.size())) {
         return FilterMask.FULLY_FILTERED;
      }

      FilterMask filtermask = new FilterMask(p_365656_.length());

      for (int i = 0; i < p_365004_.size(); i++) {
         filtermask.setFiltered(p_365004_.get(i).getAsInt());
      }

      return filtermask;
   }

   @Override
   public void close() {
      this.workerPool.shutdownNow();
   }

   protected void drainStream(InputStream p_362205_) throws IOException {
      byte[] abyte = new byte[1024];

      while (p_362205_.read(abyte) != -1) {
      }
   }

   private JsonObject processRequestResponse(JsonObject p_369256_, URL p_367030_) throws IOException {
      HttpURLConnection httpurlconnection = this.makeRequest(p_369256_, p_367030_);

      try (InputStream inputstream = httpurlconnection.getInputStream()) {
         if (httpurlconnection.getResponseCode() == 204) {
            return new JsonObject();
         }

         try {
            return LenientJsonParser.parse(new InputStreamReader(inputstream, StandardCharsets.UTF_8)).getAsJsonObject();
         } finally {
            this.drainStream(inputstream);
         }
      }
   }

   protected HttpURLConnection makeRequest(JsonObject p_365844_, URL p_361087_) throws IOException {
      HttpURLConnection httpurlconnection = this.getURLConnection(p_361087_);
      this.setAuthorizationProperty(httpurlconnection);
      OutputStreamWriter outputstreamwriter = new OutputStreamWriter(httpurlconnection.getOutputStream(), StandardCharsets.UTF_8);

      try {
         JsonWriter jsonwriter = new JsonWriter(outputstreamwriter);

         try {
            Streams.write(p_365844_, jsonwriter);
         } catch (Throwable var10) {
            try {
               jsonwriter.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         jsonwriter.close();
      } catch (Throwable throwable1) {
         try {
            outputstreamwriter.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      outputstreamwriter.close();
      int i = httpurlconnection.getResponseCode();
      if (i >= 200 && i < 300) {
         return httpurlconnection;
      } else {
         throw new ServerTextFilter.RequestFailedException(i + " " + httpurlconnection.getResponseMessage());
      }
   }

   protected abstract void setAuthorizationProperty(HttpURLConnection var1);

   protected int connectionReadTimeout() {
      return 2000;
   }

   protected HttpURLConnection getURLConnection(URL p_365457_) throws IOException {
      HttpURLConnection httpurlconnection = (HttpURLConnection)p_365457_.openConnection();
      httpurlconnection.setConnectTimeout(15000);
      httpurlconnection.setReadTimeout(this.connectionReadTimeout());
      httpurlconnection.setUseCaches(false);
      httpurlconnection.setDoOutput(true);
      httpurlconnection.setDoInput(true);
      httpurlconnection.setRequestMethod("POST");
      httpurlconnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      httpurlconnection.setRequestProperty("Accept", "application/json");
      httpurlconnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().name());
      return httpurlconnection;
   }

   public TextFilter createContext(GameProfile p_364200_) {
      return new ServerTextFilter.PlayerContext(p_364200_);
   }

   @FunctionalInterface
   public interface IgnoreStrategy {
      ServerTextFilter.IgnoreStrategy NEVER_IGNORE = (p_367292_, p_369668_) -> false;
      ServerTextFilter.IgnoreStrategy IGNORE_FULLY_FILTERED = (p_365373_, p_369783_) -> p_365373_.length() == p_369783_;

      static ServerTextFilter.IgnoreStrategy ignoreOverThreshold(int p_364607_) {
         return (p_360722_, p_360874_) -> p_360874_ >= p_364607_;
      }

      static ServerTextFilter.IgnoreStrategy select(int p_363204_) {
         return switch (p_363204_) {
            case -1 -> NEVER_IGNORE;
            case 0 -> IGNORE_FULLY_FILTERED;
            default -> ignoreOverThreshold(p_363204_);
         };
      }

      boolean shouldIgnore(String var1, int var2);
   }

   @FunctionalInterface
   protected interface MessageEncoder {
      JsonObject encode(GameProfile var1, String var2);
   }

   protected class PlayerContext implements TextFilter {
      protected final GameProfile profile;
      protected final Executor streamExecutor;

      protected PlayerContext(final GameProfile p_367136_) {
         this.profile = p_367136_;
         ConsecutiveExecutor consecutiveexecutor = new ConsecutiveExecutor(ServerTextFilter.this.workerPool, "chat stream for " + p_367136_.name());
         this.streamExecutor = consecutiveexecutor::schedule;
      }

      @Override
      public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> p_369024_) {
         List<CompletableFuture<FilteredText>> list = p_369024_.stream()
            .map(
               p_369716_ -> ServerTextFilter.this.requestMessageProcessing(
                  this.profile, p_369716_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor
               )
            )
            .collect(ImmutableList.toImmutableList());
         return Util.sequenceFailFast(list).exceptionally(p_362043_ -> ImmutableList.of());
      }

      @Override
      public CompletableFuture<FilteredText> processStreamMessage(String p_366352_) {
         return ServerTextFilter.this.requestMessageProcessing(this.profile, p_366352_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor);
      }
   }

   protected static class RequestFailedException extends RuntimeException {
      protected RequestFailedException(String p_369268_) {
         super(p_369268_);
      }
   }
}
