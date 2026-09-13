package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.OutboundPlayer;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsConfigurationDto;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlotUpdateDto;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsClient {
   public static final RealmsClient.Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment"))
      .or(() -> Optional.ofNullable(System.getProperty("realms.environment")))
      .flatMap(RealmsClient.Environment::byName)
      .orElse(RealmsClient.Environment.PRODUCTION);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile @Nullable RealmsClient realmsClientInstance = null;
   private final CompletableFuture<Set<String>> featureFlags;
   private final String sessionId;
   private final String username;
   private final Minecraft minecraft;
   private static final String WORLDS_RESOURCE_PATH = "worlds";
   private static final String INVITES_RESOURCE_PATH = "invites";
   private static final String MCO_RESOURCE_PATH = "mco";
   private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
   private static final String ACTIVITIES_RESOURCE = "activities";
   private static final String OPS_RESOURCE = "ops";
   private static final String REGIONS_RESOURCE = "regions/ping/stat";
   private static final String PREFERRED_REGION_RESOURCE = "regions/preferredRegions";
   private static final String TRIALS_RESOURCE = "trial";
   private static final String NOTIFICATIONS_RESOURCE = "notifications";
   private static final String FEATURE_FLAGS_RESOURCE = "feature/v1";
   private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
   private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
   private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
   private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
   private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
   private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
   private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
   private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
   private static final String PATH_AVAILABLE = "/available";
   private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
   private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
   private static final String PATH_WORLD_GET = "/$ID";
   private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
   private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
   private static final String PATH_PENDING_INVITES = "/pending";
   private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
   private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
   private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
   private static final String PATH_WORLD_CONFIGURE = "/$WORLD_ID/configuration";
   private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
   private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
   private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
   private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
   private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
   private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
   private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
   private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
   private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
   private static final String PATH_TOS_AGREED = "/tos/agreed";
   private static final String PATH_NEWS = "/v1/news";
   private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
   private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
   private static final GuardedSerializer GSON = new GuardedSerializer();

   public static RealmsClient getOrCreate() {
      Minecraft minecraft = Minecraft.getInstance();
      return getOrCreate(minecraft);
   }

   public static RealmsClient getOrCreate(Minecraft p_395111_) {
      String s = p_395111_.getUser().getName();
      String s1 = p_395111_.getUser().getSessionId();
      RealmsClient realmsclient = realmsClientInstance;
      if (realmsclient != null) {
         return realmsclient;
      }

      synchronized (RealmsClient.class) {
         RealmsClient realmsclient1 = realmsClientInstance;
         if (realmsclient1 != null) {
            return realmsclient1;
         }

         realmsclient1 = new RealmsClient(s1, s, p_395111_);
         realmsClientInstance = realmsclient1;
         return realmsclient1;
      }
   }

   private RealmsClient(String p_87166_, String p_87167_, Minecraft p_87168_) {
      this.sessionId = p_87166_;
      this.username = p_87167_;
      this.minecraft = p_87168_;
      RealmsClientConfig.setProxy(p_87168_.getProxy());
      this.featureFlags = CompletableFuture.supplyAsync(this::fetchFeatureFlags, Util.nonCriticalIoPool());
   }

   public Set<String> getFeatureFlags() {
      return this.featureFlags.join();
   }

   private Set<String> fetchFeatureFlags() {
      if (Minecraft.getInstance().isOfflineDeveloperMode()) {
         return Set.of();
      }

      String s = url("feature/v1", null, false);

      try {
         String s1 = this.execute(Request.get(s, 5000, 10000));
         JsonArray jsonarray = LenientJsonParser.parse(s1).getAsJsonArray();
         Set<String> set = jsonarray.asList().stream().<String>map(JsonElement::getAsString).collect(Collectors.toSet());
         LOGGER.debug("Fetched Realms feature flags: {}", set);
         return set;
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Failed to fetch Realms feature flags", realmsserviceexception);
      } catch (Exception exception) {
         LOGGER.error("Could not parse Realms feature flags", exception);
      }

      return Set.of();
   }

   public RealmsServerList listRealms() throws RealmsServiceException {
      String s = this.url("worlds");
      if (RealmsMainScreen.isSnapshot()) {
         s = s + "/listUserWorldsOfType/any";
      }

      String s1 = this.execute(Request.get(s));
      return RealmsServerList.parse(GSON, s1);
   }

   public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
      String s = this.url("worlds/listPrereleaseEligibleWorlds");
      String s1 = this.execute(Request.get(s));
      return RealmsServerList.parse(GSON, s1).servers();
   }

   public RealmsServer createSnapshotRealm(Long p_310421_) throws RealmsServiceException {
      String s = String.valueOf(p_310421_);
      String s1 = this.url("worlds" + "/$PARENT_WORLD_ID/createPrereleaseRealm".replace("$PARENT_WORLD_ID", s));
      return RealmsServer.parse(GSON, this.execute(Request.post(s1, s)));
   }

   public List<RealmsNotification> getNotifications() throws RealmsServiceException {
      String s = this.url("notifications");
      String s1 = this.execute(Request.get(s));
      return RealmsNotification.parseList(s1);
   }

   private static JsonArray uuidListToJsonArray(List<UUID> p_275393_) {
      JsonArray jsonarray = new JsonArray();

      for (UUID uuid : p_275393_) {
         if (uuid != null) {
            jsonarray.add(uuid.toString());
         }
      }

      return jsonarray;
   }

   public void notificationsSeen(List<UUID> p_275212_) throws RealmsServiceException {
      String s = this.url("notifications/seen");
      this.execute(Request.post(s, GSON.toJson(uuidListToJsonArray(p_275212_))));
   }

   public void notificationsDismiss(List<UUID> p_275407_) throws RealmsServiceException {
      String s = this.url("notifications/dismiss");
      this.execute(Request.post(s, GSON.toJson(uuidListToJsonArray(p_275407_))));
   }

   public RealmsServer getOwnRealm(long p_87175_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$ID".replace("$ID", String.valueOf(p_87175_)));
      String s1 = this.execute(Request.get(s));
      return RealmsServer.parse(GSON, s1);
   }

   public PreferredRegionsDto getPreferredRegionSelections() throws RealmsServiceException {
      String s = this.url("regions/preferredRegions");
      String s1 = this.execute(Request.get(s));

      try {
         PreferredRegionsDto preferredregionsdto = GSON.fromJson(s1, PreferredRegionsDto.class);
         if (preferredregionsdto == null) {
            return PreferredRegionsDto.empty();
         }

         Set<RealmsRegion> set = preferredregionsdto.regionData().stream().map(RegionDataDto::region).collect(Collectors.toSet());

         for (RealmsRegion realmsregion : RealmsRegion.values()) {
            if (realmsregion != RealmsRegion.INVALID_REGION && !set.contains(realmsregion)) {
               LOGGER.debug("No realms region matching {} in server response", realmsregion);
            }
         }

         return preferredregionsdto;
      } catch (Exception exception) {
         LOGGER.error("Could not parse PreferredRegionSelections", exception);
         return PreferredRegionsDto.empty();
      }
   }

   public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
      String s = this.url("activities/liveplayerlist");
      String s1 = this.execute(Request.get(s));
      return RealmsServerPlayerLists.parse(s1);
   }

   public RealmsJoinInformation join(long p_87208_) throws RealmsServiceException {
      String s = this.url("worlds" + "/v1/$ID/join/pc".replace("$ID", p_87208_ + ""));
      String s1 = this.execute(Request.get(s, 5000, 30000));
      return RealmsJoinInformation.parse(GSON, s1);
   }

   public void initializeRealm(long p_87192_, String p_87193_, String p_87194_) throws RealmsServiceException {
      RealmsDescriptionDto realmsdescriptiondto = new RealmsDescriptionDto(p_87193_, p_87194_);
      String s = this.url("worlds" + "/$WORLD_ID/initialize".replace("$WORLD_ID", String.valueOf(p_87192_)));
      String s1 = GSON.toJson(realmsdescriptiondto);
      this.execute(Request.post(s, s1, 5000, 10000));
   }

   public boolean hasParentalConsent() throws RealmsServiceException {
      String s = this.url("mco/available");
      String s1 = this.execute(Request.get(s));
      return Boolean.parseBoolean(s1);
   }

   public RealmsClient.CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
      String s = this.url("mco/client/compatible");
      String s1 = this.execute(Request.get(s));

      try {
         return RealmsClient.CompatibleVersionResponse.valueOf(s1);
      } catch (IllegalArgumentException illegalargumentexception) {
         throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse(s1));
      }
   }

   public void uninvite(long p_87184_, UUID p_300114_) throws RealmsServiceException {
      String s = this.url(
         "invites" + "/$WORLD_ID/invite/$UUID".replace("$WORLD_ID", String.valueOf(p_87184_)).replace("$UUID", UndashedUuid.toString(p_300114_))
      );
      this.execute(Request.delete(s));
   }

   public void uninviteMyselfFrom(long p_87223_) throws RealmsServiceException {
      String s = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87223_)));
      this.execute(Request.delete(s));
   }

   public List<PlayerInfo> invite(long p_87213_, String p_87214_) throws RealmsServiceException {
      OutboundPlayer outboundplayer = new OutboundPlayer();
      outboundplayer.name = p_87214_;
      String s = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87213_)));
      String s1 = this.execute(Request.post(s, GSON.toJson(outboundplayer)));
      return RealmsServer.parse(GSON, s1).players;
   }

   public BackupList backupsFor(long p_87231_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(p_87231_)));
      String s1 = this.execute(Request.get(s));
      return BackupList.parse(s1);
   }

   public void updateConfiguration(
      long p_407011_,
      String p_407177_,
      String p_408589_,
      @Nullable RegionSelectionPreferenceDto p_407434_,
      int p_407481_,
      RealmsWorldOptions p_406671_,
      List<RealmsSetting> p_405848_
   ) throws RealmsServiceException {
      RegionSelectionPreferenceDto regionselectionpreferencedto = p_407434_ != null
         ? p_407434_
         : new RegionSelectionPreferenceDto(RegionSelectionPreference.DEFAULT_SELECTION, null);
      RealmsDescriptionDto realmsdescriptiondto = new RealmsDescriptionDto(p_407177_, p_408589_);
      RealmsSlotUpdateDto realmsslotupdatedto = new RealmsSlotUpdateDto(p_407481_, p_406671_, RealmsSetting.isHardcore(p_405848_));
      RealmsConfigurationDto realmsconfigurationdto = new RealmsConfigurationDto(
         realmsslotupdatedto, p_405848_, regionselectionpreferencedto, realmsdescriptiondto
      );
      String s = this.url("worlds" + "/$WORLD_ID/configuration".replace("$WORLD_ID", String.valueOf(p_407011_)));
      this.execute(Request.post(s, GSON.toJson(realmsconfigurationdto)));
   }

   public void updateSlot(long p_87180_, int p_87181_, RealmsWorldOptions p_87182_, List<RealmsSetting> p_409920_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(p_87180_)).replace("$SLOT_ID", String.valueOf(p_87181_)));
      String s1 = GSON.toJson(new RealmsSlotUpdateDto(p_87181_, p_87182_, RealmsSetting.isHardcore(p_409920_)));
      this.execute(Request.post(s, s1));
   }

   public boolean switchSlot(long p_87177_, int p_87178_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(p_87177_)).replace("$SLOT_ID", String.valueOf(p_87178_)));
      String s1 = this.execute(Request.put(s, ""));
      return Boolean.valueOf(s1);
   }

   public void restoreWorld(long p_87225_, String p_87226_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(p_87225_)), "backupId=" + p_87226_);
      this.execute(Request.put(s, "", 40000, 600000));
   }

   public WorldTemplatePaginatedList fetchWorldTemplates(int p_87171_, int p_87172_, RealmsServer.WorldType p_87173_) throws RealmsServiceException {
      String s = this.url(
         "worlds" + "/templates/$WORLD_TYPE".replace("$WORLD_TYPE", p_87173_.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", p_87171_, p_87172_)
      );
      String s1 = this.execute(Request.get(s));
      return WorldTemplatePaginatedList.parse(s1);
   }

   public Boolean putIntoMinigameMode(long p_87233_, String p_87234_) throws RealmsServiceException {
      String s = "/minigames/$MINIGAME_ID/$WORLD_ID".replace("$MINIGAME_ID", p_87234_).replace("$WORLD_ID", String.valueOf(p_87233_));
      String s1 = this.url("worlds" + s);
      return Boolean.valueOf(this.execute(Request.put(s1, "")));
   }

   public Ops op(long p_87239_, UUID p_297634_) throws RealmsServiceException {
      String s = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(p_87239_)).replace("$PROFILE_UUID", UndashedUuid.toString(p_297634_));
      String s1 = this.url("ops" + s);
      return Ops.parse(this.execute(Request.post(s1, "")));
   }

   public Ops deop(long p_87245_, UUID p_298989_) throws RealmsServiceException {
      String s = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(p_87245_)).replace("$PROFILE_UUID", UndashedUuid.toString(p_298989_));
      String s1 = this.url("ops" + s);
      return Ops.parse(this.execute(Request.delete(s1)));
   }

   public Boolean open(long p_87237_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/open".replace("$WORLD_ID", String.valueOf(p_87237_)));
      String s1 = this.execute(Request.put(s, ""));
      return Boolean.valueOf(s1);
   }

   public Boolean close(long p_87243_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/close".replace("$WORLD_ID", String.valueOf(p_87243_)));
      String s1 = this.execute(Request.put(s, ""));
      return Boolean.valueOf(s1);
   }

   public Boolean resetWorldWithTemplate(long p_87251_, String p_87252_) throws RealmsServiceException {
      RealmsWorldResetDto realmsworldresetdto = new RealmsWorldResetDto(null, Long.valueOf(p_87252_), -1, false, Set.of());
      String s = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(p_87251_)));
      String s1 = this.execute(Request.post(s, GSON.toJson(realmsworldresetdto), 30000, 80000));
      return Boolean.valueOf(s1);
   }

   public Subscription subscriptionFor(long p_87249_) throws RealmsServiceException {
      String s = this.url("subscriptions" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87249_)));
      String s1 = this.execute(Request.get(s));
      return Subscription.parse(s1);
   }

   public int pendingInvitesCount() throws RealmsServiceException {
      return this.pendingInvites().pendingInvites().size();
   }

   public PendingInvitesList pendingInvites() throws RealmsServiceException {
      String s = this.url("invites/pending");
      String s1 = this.execute(Request.get(s));
      PendingInvitesList pendinginviteslist = PendingInvitesList.parse(s1);
      pendinginviteslist.pendingInvites().removeIf(this::isBlocked);
      return pendinginviteslist;
   }

   private boolean isBlocked(PendingInvite p_87198_) {
      return this.minecraft.getPlayerSocialManager().isBlocked(p_87198_.realmOwnerUuid());
   }

   public void acceptInvitation(String p_87202_) throws RealmsServiceException {
      String s = this.url("invites" + "/accept/$INVITATION_ID".replace("$INVITATION_ID", p_87202_));
      this.execute(Request.put(s, ""));
   }

   public WorldDownload requestDownloadInfo(long p_87210_, int p_87211_) throws RealmsServiceException {
      String s = this.url(
         "worlds" + "/$WORLD_ID/slot/$SLOT_ID/download".replace("$WORLD_ID", String.valueOf(p_87210_)).replace("$SLOT_ID", String.valueOf(p_87211_))
      );
      String s1 = this.execute(Request.get(s));
      return WorldDownload.parse(s1);
   }

   public @Nullable UploadInfo requestUploadInfo(long p_87257_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID/backups/upload".replace("$WORLD_ID", String.valueOf(p_87257_)));
      String s1 = UploadTokenCache.get(p_87257_);
      UploadInfo uploadinfo = UploadInfo.parse(this.execute(Request.put(s, UploadInfo.createRequest(s1))));
      if (uploadinfo != null) {
         UploadTokenCache.put(p_87257_, uploadinfo.token());
      }

      return uploadinfo;
   }

   public void rejectInvitation(String p_87220_) throws RealmsServiceException {
      String s = this.url("invites" + "/reject/$INVITATION_ID".replace("$INVITATION_ID", p_87220_));
      this.execute(Request.put(s, ""));
   }

   public void agreeToTos() throws RealmsServiceException {
      String s = this.url("mco/tos/agreed");
      this.execute(Request.post(s, ""));
   }

   public RealmsNews getNews() throws RealmsServiceException {
      String s = this.url("mco/v1/news");
      String s1 = this.execute(Request.get(s, 5000, 10000));
      return RealmsNews.parse(s1);
   }

   public void sendPingResults(PingResult p_87200_) throws RealmsServiceException {
      String s = this.url("regions/ping/stat");
      this.execute(Request.post(s, GSON.toJson(p_87200_)));
   }

   public Boolean trialAvailable() throws RealmsServiceException {
      String s = this.url("trial");
      String s1 = this.execute(Request.get(s));
      return Boolean.valueOf(s1);
   }

   public void deleteRealm(long p_87255_) throws RealmsServiceException {
      String s = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87255_)));
      this.execute(Request.delete(s));
   }

   private String url(String p_87228_) throws RealmsServiceException {
      return this.url(p_87228_, null);
   }

   private String url(String p_87204_, @Nullable String p_87205_) {
      return url(p_87204_, p_87205_, this.getFeatureFlags().contains("realms_in_aks"));
   }

   private static String url(String p_392176_, @Nullable String p_391500_, boolean p_393414_) {
      try {
         return new URI(ENVIRONMENT.protocol, p_393414_ ? ENVIRONMENT.alternativeUrl : ENVIRONMENT.baseUrl, "/" + p_392176_, p_391500_, null).toASCIIString();
      } catch (URISyntaxException urisyntaxexception) {
         throw new IllegalArgumentException(p_392176_, urisyntaxexception);
      }
   }

   private String execute(Request<?> p_87196_) throws RealmsServiceException {
      p_87196_.cookie("sid", this.sessionId);
      p_87196_.cookie("user", this.username);
      p_87196_.cookie("version", SharedConstants.getCurrentVersion().name());
      p_87196_.addSnapshotHeader(RealmsMainScreen.isSnapshot());

      try {
         int i = p_87196_.responseCode();
         if (i != 503 && i != 277) {
            String s1 = p_87196_.text();
            if (i >= 200 && i < 300) {
               return s1;
            }

            if (i == 401) {
               String s2 = p_87196_.getHeader("WWW-Authenticate");
               LOGGER.info("Could not authorize you against Realms server: {}", s2);
               throw new RealmsServiceException(new RealmsError.AuthenticationError(s2));
            }

            String s = p_87196_.connection.getContentType();
            if (s != null && s.startsWith("text/html")) {
               throw new RealmsServiceException(RealmsError.CustomError.htmlPayload(i, s1));
            }

            RealmsError realmserror = RealmsError.parse(i, s1);
            throw new RealmsServiceException(realmserror);
         } else {
            int j = p_87196_.getRetryAfterHeader();
            throw new RetryCallException(j, i);
         }
      } catch (RealmsHttpException realmshttpexception) {
         throw new RealmsServiceException(RealmsError.CustomError.connectivityError(realmshttpexception));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum CompatibleVersionResponse {
      COMPATIBLE,
      OUTDATED,
      OTHER;
   }

   @OnlyIn(Dist.CLIENT)
   public enum Environment {
      PRODUCTION("pc.realms.minecraft.net", "java.frontendlegacy.realms.minecraft-services.net", "https"),
      STAGE("pc-stage.realms.minecraft.net", "java.frontendlegacy.stage-c2a40e62.realms.minecraft-services.net", "https"),
      LOCAL("localhost:8080", "localhost:8080", "http");

      public final String baseUrl;
      public final String alternativeUrl;
      public final String protocol;

      Environment(final String p_87286_, final String p_87287_, final String p_392210_) {
         this.baseUrl = p_87286_;
         this.alternativeUrl = p_87287_;
         this.protocol = p_392210_;
      }

      public static Optional<RealmsClient.Environment> byName(String p_289688_) {
         String s = p_289688_.toLowerCase(Locale.ROOT);

         return switch (s) {
            case "production" -> Optional.of(PRODUCTION);
            case "local" -> Optional.of(LOCAL);
            case "stage", "staging" -> Optional.of(STAGE);
            default -> Optional.empty();
         };
      }
   }
}
