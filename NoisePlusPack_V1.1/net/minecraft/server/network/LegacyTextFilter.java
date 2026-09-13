package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class LegacyTextFilter extends ServerTextFilter {
   private static final String ENDPOINT = "v1/chat";
   final URL joinEndpoint;
   final LegacyTextFilter.JoinOrLeaveEncoder joinEncoder;
   final URL leaveEndpoint;
   final LegacyTextFilter.JoinOrLeaveEncoder leaveEncoder;
   private final String authKey;

   private LegacyTextFilter(
      URL p_366922_,
      ServerTextFilter.MessageEncoder p_362103_,
      URL p_369617_,
      LegacyTextFilter.JoinOrLeaveEncoder p_364840_,
      URL p_367665_,
      LegacyTextFilter.JoinOrLeaveEncoder p_365794_,
      String p_363705_,
      ServerTextFilter.IgnoreStrategy p_368934_,
      ExecutorService p_369275_
   ) {
      super(p_366922_, p_362103_, p_368934_, p_369275_);
      this.joinEndpoint = p_369617_;
      this.joinEncoder = p_364840_;
      this.leaveEndpoint = p_367665_;
      this.leaveEncoder = p_365794_;
      this.authKey = p_363705_;
   }

   public static @Nullable ServerTextFilter createTextFilterFromConfig(String p_364392_) {
      try {
         JsonObject jsonobject = GsonHelper.parse(p_364392_);
         URI uri = new URI(GsonHelper.getAsString(jsonobject, "apiServer"));
         String s = GsonHelper.getAsString(jsonobject, "apiKey");
         if (s.isEmpty()) {
            throw new IllegalArgumentException("Missing API key");
         }

         int i = GsonHelper.getAsInt(jsonobject, "ruleId", 1);
         String s1 = GsonHelper.getAsString(jsonobject, "serverId", "");
         String s2 = GsonHelper.getAsString(jsonobject, "roomId", "Java:Chat");
         int j = GsonHelper.getAsInt(jsonobject, "hashesToDrop", -1);
         int k = GsonHelper.getAsInt(jsonobject, "maxConcurrentRequests", 7);
         JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "endpoints", null);
         String s3 = getEndpointFromConfig(jsonobject1, "chat", "v1/chat");
         boolean flag = s3.equals("v1/chat");
         URL url = uri.resolve("/" + s3).toURL();
         URL url1 = getEndpoint(uri, jsonobject1, "join", "v1/join");
         URL url2 = getEndpoint(uri, jsonobject1, "leave", "v1/leave");
         LegacyTextFilter.JoinOrLeaveEncoder legacytextfilter$joinorleaveencoder = p_421490_ -> {
            JsonObject jsonobject2 = new JsonObject();
            jsonobject2.addProperty("server", s1);
            jsonobject2.addProperty("room", s2);
            jsonobject2.addProperty("user_id", p_421490_.id().toString());
            jsonobject2.addProperty("user_display_name", p_421490_.name());
            return jsonobject2;
         };
         ServerTextFilter.MessageEncoder servertextfilter$messageencoder;
         if (flag) {
            servertextfilter$messageencoder = (p_421481_, p_421482_) -> {
               JsonObject jsonobject2 = new JsonObject();
               jsonobject2.addProperty("rule", i);
               jsonobject2.addProperty("server", s1);
               jsonobject2.addProperty("room", s2);
               jsonobject2.addProperty("player", p_421481_.id().toString());
               jsonobject2.addProperty("player_display_name", p_421481_.name());
               jsonobject2.addProperty("text", p_421482_);
               jsonobject2.addProperty("language", "*");
               return jsonobject2;
            };
         } else {
            String s4 = String.valueOf(i);
            servertextfilter$messageencoder = (p_421486_, p_421487_) -> {
               JsonObject jsonobject2 = new JsonObject();
               jsonobject2.addProperty("rule_id", s4);
               jsonobject2.addProperty("category", s1);
               jsonobject2.addProperty("subcategory", s2);
               jsonobject2.addProperty("user_id", p_421486_.id().toString());
               jsonobject2.addProperty("user_display_name", p_421486_.name());
               jsonobject2.addProperty("text", p_421487_);
               jsonobject2.addProperty("language", "*");
               return jsonobject2;
            };
         }

         ServerTextFilter.IgnoreStrategy servertextfilter$ignorestrategy = ServerTextFilter.IgnoreStrategy.select(j);
         ExecutorService executorservice = createWorkerPool(k);
         String s5 = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.US_ASCII));
         return new LegacyTextFilter(
            url,
            servertextfilter$messageencoder,
            url1,
            legacytextfilter$joinorleaveencoder,
            url2,
            legacytextfilter$joinorleaveencoder,
            s5,
            servertextfilter$ignorestrategy,
            executorservice
         );
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse chat filter config {}", p_364392_, exception);
         return null;
      }
   }

   @Override
   public TextFilter createContext(GameProfile p_361590_) {
      return new ServerTextFilter.PlayerContext(p_361590_) {
         @Override
         public void join() {
            LegacyTextFilter.this.processJoinOrLeave(this.profile, LegacyTextFilter.this.joinEndpoint, LegacyTextFilter.this.joinEncoder, this.streamExecutor);
         }

         @Override
         public void leave() {
            LegacyTextFilter.this.processJoinOrLeave(this.profile, LegacyTextFilter.this.leaveEndpoint, LegacyTextFilter.this.leaveEncoder, this.streamExecutor);
         }
      };
   }

   void processJoinOrLeave(GameProfile p_365389_, URL p_361745_, LegacyTextFilter.JoinOrLeaveEncoder p_361630_, Executor p_362353_) {
      p_362353_.execute(() -> {
         JsonObject jsonobject = p_361630_.encode(p_365389_);

         try {
            this.processRequest(jsonobject, p_361745_);
         } catch (Exception exception) {
            LOGGER.warn("Failed to send join/leave packet to {} for player {}", new Object[]{p_361745_, p_365389_, exception});
         }
      });
   }

   private void processRequest(JsonObject p_365130_, URL p_369729_) throws IOException {
      HttpURLConnection httpurlconnection = this.makeRequest(p_365130_, p_369729_);

      try (InputStream inputstream = httpurlconnection.getInputStream()) {
         this.drainStream(inputstream);
      }
   }

   @Override
   protected void setAuthorizationProperty(HttpURLConnection p_362434_) {
      p_362434_.setRequestProperty("Authorization", "Basic " + this.authKey);
   }

   @Override
   protected FilteredText filterText(String p_364027_, ServerTextFilter.IgnoreStrategy p_361528_, JsonObject p_365914_) {
      boolean flag = GsonHelper.getAsBoolean(p_365914_, "response", false);
      if (flag) {
         return FilteredText.passThrough(p_364027_);
      }

      String s = GsonHelper.getAsString(p_365914_, "hashed", null);
      if (s == null) {
         return FilteredText.fullyFiltered(p_364027_);
      }

      JsonArray jsonarray = GsonHelper.getAsJsonArray(p_365914_, "hashes");
      FilterMask filtermask = this.parseMask(p_364027_, jsonarray, p_361528_);
      return new FilteredText(p_364027_, filtermask);
   }

   @FunctionalInterface
   interface JoinOrLeaveEncoder {
      JsonObject encode(GameProfile var1);
   }
}
