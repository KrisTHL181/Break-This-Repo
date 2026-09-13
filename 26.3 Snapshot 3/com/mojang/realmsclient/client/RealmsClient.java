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
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

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

   public static RealmsClient getOrCreate(final Minecraft minecraft) {
      String username = minecraft.getUser().getName();
      String sessionId = minecraft.getUser().getSessionId();
      RealmsClient realmsClient = realmsClientInstance;
      if (realmsClient != null) {
         return realmsClient;
      }

      synchronized (RealmsClient.class) {
         RealmsClient rc = realmsClientInstance;
         if (rc != null) {
            return rc;
         }

         rc = new RealmsClient(sessionId, username, minecraft);
         realmsClientInstance = rc;
         return rc;
      }
   }

   private RealmsClient(final String sessionId, final String username, final Minecraft minecraft) {
      this.sessionId = sessionId;
      this.username = username;
      this.minecraft = minecraft;
      RealmsClientConfig.setProxy(minecraft.getProxy());
      this.featureFlags = CompletableFuture.supplyAsync(this::fetchFeatureFlags, Util.nonCriticalIoPool());
   }

   public Set<String> getFeatureFlags() {
      return this.featureFlags.join();
   }

   private Set<String> fetchFeatureFlags() {
      if (Minecraft.getInstance().isOfflineDeveloperMode()) {
         return Set.of();
      }

      String asciiUrl = url("feature/v1", null, false);

      try {
         String returnJson = this.execute(Request.get(asciiUrl, 5000, 10000));
         JsonArray object = LenientJsonParser.parse(returnJson).getAsJsonArray();
         Set<String> featureFlags = object.asList().stream().<String>map(JsonElement::getAsString).collect(Collectors.toSet());
         LOGGER.debug("Fetched Realms feature flags: {}", featureFlags);
         return featureFlags;
      } catch (RealmsServiceException e) {
         LOGGER.error("Failed to fetch Realms feature flags", e);
      } catch (Exception e) {
         LOGGER.error("Could not parse Realms feature flags", e);
      }

      return Set.of();
   }

   public RealmsServerList listRealms() throws RealmsServiceException {
      String asciiUrl = this.url("worlds");
      if (RealmsMainScreen.isSnapshot()) {
         asciiUrl = asciiUrl + "/listUserWorldsOfType/any";
      }

      String json = this.execute(Request.get(asciiUrl));
      return RealmsServerList.parse(GSON, json);
   }

   public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
      String asciiUrl = this.url("worlds/listPrereleaseEligibleWorlds");
      String json = this.execute(Request.get(asciiUrl));
      return RealmsServerList.parse(GSON, json).servers();
   }

   public RealmsServer createSnapshotRealm(final Long parentId) throws RealmsServiceException {
      String parentIdString = String.valueOf(parentId);
      String url = this.url("worlds" + "/$PARENT_WORLD_ID/createPrereleaseRealm".replace("$PARENT_WORLD_ID", parentIdString));
      return RealmsServer.parse(GSON, this.execute(Request.post(url, parentIdString)));
   }

   public List<RealmsNotification> getNotifications() throws RealmsServiceException {
      String endpoint = this.url("notifications");
      String responseJson = this.execute(Request.get(endpoint));
      return RealmsNotification.parseList(responseJson);
   }

   private static JsonArray uuidListToJsonArray(final List<UUID> uuids) {
      JsonArray array = new JsonArray();

      for (UUID uuid : uuids) {
         if (uuid != null) {
            array.add(uuid.toString());
         }
      }

      return array;
   }

   public void notificationsSeen(final List<UUID> notificationUuids) throws RealmsServiceException {
      String endpoint = this.url("notifications/seen");
      this.execute(Request.post(endpoint, GSON.toJson(uuidListToJsonArray(notificationUuids))));
   }

   public void notificationsDismiss(final List<UUID> notificationUuids) throws RealmsServiceException {
      String endpoint = this.url("notifications/dismiss");
      this.execute(Request.post(endpoint, GSON.toJson(uuidListToJsonArray(notificationUuids))));
   }

   public RealmsServer getOwnRealm(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$ID".replace("$ID", String.valueOf(realmId)));
      String json = this.execute(Request.get(asciiUrl));
      return RealmsServer.parse(GSON, json);
   }

   public PreferredRegionsDto getPreferredRegionSelections() throws RealmsServiceException {
      String asciiUrl = this.url("regions/preferredRegions");
      String json = this.execute(Request.get(asciiUrl));

      try {
         PreferredRegionsDto preferredRegionsDto = GSON.fromJson(json, PreferredRegionsDto.class);
         if (preferredRegionsDto == null) {
            return PreferredRegionsDto.empty();
         }

         Set<RealmsRegion> regionsInResponse = preferredRegionsDto.regionData().stream().map(RegionDataDto::region).collect(Collectors.toSet());

         for (RealmsRegion region : RealmsRegion.values()) {
            if (region != RealmsRegion.INVALID_REGION && !regionsInResponse.contains(region)) {
               LOGGER.debug("No realms region matching {} in server response", region);
            }
         }

         return preferredRegionsDto;
      } catch (Exception e) {
         LOGGER.error("Could not parse PreferredRegionSelections", e);
         return PreferredRegionsDto.empty();
      }
   }

   public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
      String asciiUrl = this.url("activities/liveplayerlist");
      String json = this.execute(Request.get(asciiUrl));
      return RealmsServerPlayerLists.parse(json);
   }

   public RealmsJoinInformation join(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/v1/$ID/join/pc".replace("$ID", realmId + ""));
      String json = this.execute(Request.get(asciiUrl, 5000, 30000));
      return RealmsJoinInformation.parse(GSON, json);
   }

   public void initializeRealm(final long realmId, final String name, final String motd) throws RealmsServiceException {
      RealmsDescriptionDto realmsDescription = new RealmsDescriptionDto(name, motd);
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/initialize".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = GSON.toJson(realmsDescription);
      this.execute(Request.post(asciiUrl, json, 5000, 10000));
   }

   public boolean hasParentalConsent() throws RealmsServiceException {
      String asciiUrl = this.url("mco/available");
      String json = this.execute(Request.get(asciiUrl));
      return Boolean.parseBoolean(json);
   }

   public RealmsClient.CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
      String asciiUrl = this.url("mco/client/compatible");
      String response = this.execute(Request.get(asciiUrl));

      try {
         return RealmsClient.CompatibleVersionResponse.valueOf(response);
      } catch (IllegalArgumentException ignored) {
         throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse(response));
      }
   }

   public void uninvite(final long realmId, final UUID profileId) throws RealmsServiceException {
      String asciiUrl = this.url(
         "invites" + "/$WORLD_ID/invite/$UUID".replace("$WORLD_ID", String.valueOf(realmId)).replace("$UUID", UndashedUuid.toString(profileId))
      );
      this.execute(Request.delete(asciiUrl));
   }

   public void uninviteMyselfFrom(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
      this.execute(Request.delete(asciiUrl));
   }

   public List<PlayerInfo> invite(final long realmId, final String profileName) throws RealmsServiceException {
      OutboundPlayer playerInfo = new OutboundPlayer();
      playerInfo.name = profileName;
      String asciiUrl = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.post(asciiUrl, GSON.toJson(playerInfo)));
      return RealmsServer.parse(GSON, json).players;
   }

   public BackupList backupsFor(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.get(asciiUrl));
      return BackupList.parse(json);
   }

   public void updateConfiguration(
      final long realmId,
      final String name,
      final String description,
      final @Nullable RegionSelectionPreferenceDto regionSelectionPreference,
      final int slotId,
      final RealmsWorldOptions options,
      final List<RealmsSetting> settings
   ) throws RealmsServiceException {
      RegionSelectionPreferenceDto preferenceDto = regionSelectionPreference != null
         ? regionSelectionPreference
         : new RegionSelectionPreferenceDto(RegionSelectionPreference.DEFAULT_SELECTION, null);
      RealmsDescriptionDto realmsDescription = new RealmsDescriptionDto(name, description);
      RealmsSlotUpdateDto slotUpdateDto = new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings));
      RealmsConfigurationDto realmsConfiguration = new RealmsConfigurationDto(slotUpdateDto, settings, preferenceDto, realmsDescription);
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/configuration".replace("$WORLD_ID", String.valueOf(realmId)));
      this.execute(Request.post(asciiUrl, GSON.toJson(realmsConfiguration)));
   }

   public void updateSlot(final long realmId, final int slotId, final RealmsWorldOptions options, final List<RealmsSetting> settings) throws RealmsServiceException {
      String asciiUrl = this.url(
         "worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slotId))
      );
      String json = GSON.toJson(new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings)));
      this.execute(Request.post(asciiUrl, json));
   }

   public boolean switchSlot(final long realmId, final int slot) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slot)));
      String json = this.execute(Request.put(asciiUrl, ""));
      return Boolean.valueOf(json);
   }

   public void restoreWorld(final long realmId, final String backupId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(realmId)), "backupId=" + backupId);
      this.execute(Request.put(asciiUrl, "", 40000, 600000));
   }

   public WorldTemplatePaginatedList fetchWorldTemplates(final int page, final int pageSize, final RealmsServer.WorldType type) throws RealmsServiceException {
      String asciiUrl = this.url(
         "worlds" + "/templates/$WORLD_TYPE".replace("$WORLD_TYPE", type.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", page, pageSize)
      );
      String json = this.execute(Request.get(asciiUrl));
      return WorldTemplatePaginatedList.parse(json);
   }

   public Boolean putIntoMinigameMode(final long realmId, final String minigameId) throws RealmsServiceException {
      String path = "/minigames/$MINIGAME_ID/$WORLD_ID".replace("$MINIGAME_ID", minigameId).replace("$WORLD_ID", String.valueOf(realmId));
      String asciiUrl = this.url("worlds" + path);
      return Boolean.valueOf(this.execute(Request.put(asciiUrl, "")));
   }

   public Ops op(final long realmId, final UUID profileId) throws RealmsServiceException {
      String path = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(realmId)).replace("$PROFILE_UUID", UndashedUuid.toString(profileId));
      String asciiUrl = this.url("ops" + path);
      return Ops.parse(this.execute(Request.post(asciiUrl, "")));
   }

   public Ops deop(final long realmId, final UUID profileId) throws RealmsServiceException {
      String path = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(realmId)).replace("$PROFILE_UUID", UndashedUuid.toString(profileId));
      String asciiUrl = this.url("ops" + path);
      return Ops.parse(this.execute(Request.delete(asciiUrl)));
   }

   public Boolean open(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/open".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.put(asciiUrl, ""));
      return Boolean.valueOf(json);
   }

   public Boolean close(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/close".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.put(asciiUrl, ""));
      return Boolean.valueOf(json);
   }

   public Boolean resetWorldWithTemplate(final long realmId, final String worldTemplateId) throws RealmsServiceException {
      RealmsWorldResetDto worldReset = new RealmsWorldResetDto(null, Long.valueOf(worldTemplateId), -1, false, Set.of());
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.post(asciiUrl, GSON.toJson(worldReset), 30000, 80000));
      return Boolean.valueOf(json);
   }

   public Subscription subscriptionFor(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("subscriptions" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
      String json = this.execute(Request.get(asciiUrl));
      return Subscription.parse(json);
   }

   public int pendingInvitesCount() throws RealmsServiceException {
      return this.pendingInvites().pendingInvites().size();
   }

   public PendingInvitesList pendingInvites() throws RealmsServiceException {
      String asciiUrl = this.url("invites/pending");
      String json = this.execute(Request.get(asciiUrl));
      PendingInvitesList list = PendingInvitesList.parse(json);
      list.pendingInvites().removeIf(this::isBlocked);
      return list;
   }

   private boolean isBlocked(final PendingInvite invite) {
      return this.minecraft.getPlayerSocialManager().isBlocked(invite.realmOwnerUuid());
   }

   public void acceptInvitation(final String invitationId) throws RealmsServiceException {
      String asciiUrl = this.url("invites" + "/accept/$INVITATION_ID".replace("$INVITATION_ID", invitationId));
      this.execute(Request.put(asciiUrl, ""));
   }

   public WorldDownload requestDownloadInfo(final long realmId, final int slotId) throws RealmsServiceException {
      String asciiUrl = this.url(
         "worlds" + "/$WORLD_ID/slot/$SLOT_ID/download".replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slotId))
      );
      String json = this.execute(Request.get(asciiUrl));
      return WorldDownload.parse(json);
   }

   public @Nullable UploadInfo requestUploadInfo(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID/backups/upload".replace("$WORLD_ID", String.valueOf(realmId)));
      String uploadToken = UploadTokenCache.get(realmId);
      UploadInfo uploadInfo = UploadInfo.parse(this.execute(Request.put(asciiUrl, UploadInfo.createRequest(uploadToken))));
      if (uploadInfo != null) {
         UploadTokenCache.put(realmId, uploadInfo.token());
      }

      return uploadInfo;
   }

   public void rejectInvitation(final String invitationId) throws RealmsServiceException {
      String asciiUrl = this.url("invites" + "/reject/$INVITATION_ID".replace("$INVITATION_ID", invitationId));
      this.execute(Request.put(asciiUrl, ""));
   }

   public void agreeToTos() throws RealmsServiceException {
      String asciiUrl = this.url("mco/tos/agreed");
      this.execute(Request.post(asciiUrl, ""));
   }

   public RealmsNews getNews() throws RealmsServiceException {
      String asciiUrl = this.url("mco/v1/news");
      String returnJson = this.execute(Request.get(asciiUrl, 5000, 10000));
      return RealmsNews.parse(returnJson);
   }

   public void sendPingResults(final PingResult pingResult) throws RealmsServiceException {
      String asciiUrl = this.url("regions/ping/stat");
      this.execute(Request.post(asciiUrl, GSON.toJson(pingResult)));
   }

   public Boolean trialAvailable() throws RealmsServiceException {
      String asciiUrl = this.url("trial");
      String json = this.execute(Request.get(asciiUrl));
      return Boolean.valueOf(json);
   }

   public void deleteRealm(final long realmId) throws RealmsServiceException {
      String asciiUrl = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
      this.execute(Request.delete(asciiUrl));
   }

   private String url(final String path) throws RealmsServiceException {
      return this.url(path, null);
   }

   private String url(final String path, final @Nullable String queryString) {
      return url(path, queryString, this.getFeatureFlags().contains("realms_in_aks"));
   }

   private static String url(final String path, final @Nullable String queryString, final boolean useAlternativeURL) {
      try {
         return new URI(ENVIRONMENT.protocol, useAlternativeURL ? ENVIRONMENT.alternativeUrl : ENVIRONMENT.baseUrl, "/" + path, queryString, null)
            .toASCIIString();
      } catch (URISyntaxException e) {
         throw new IllegalArgumentException(path, e);
      }
   }

   private String execute(final Request<?> request) throws RealmsServiceException {
      request.cookie("sid", this.sessionId);
      request.cookie("user", this.username);
      WorldVersion version = SharedConstants.getCurrentVersion();
      request.cookie("version", version.name());
      request.addVersionHeaders(version.protocolVersion(), RealmsMainScreen.isSnapshot());

      try {
         int responseCode = request.responseCode();
         if (responseCode != 503 && responseCode != 277) {
            String responseText = request.text();
            if (responseCode >= 200 && responseCode < 300) {
               return responseText;
            }

            if (responseCode == 401) {
               String authenticationHeader = request.getHeader("WWW-Authenticate");
               LOGGER.info("Could not authorize you against Realms server: {}", authenticationHeader);
               throw new RealmsServiceException(new RealmsError.AuthenticationError(authenticationHeader));
            }

            String contentType = request.connection.getContentType();
            if (contentType != null && contentType.startsWith("text/html")) {
               throw new RealmsServiceException(RealmsError.CustomError.htmlPayload(responseCode, responseText));
            }

            RealmsError error = RealmsError.parse(responseCode, responseText);
            throw new RealmsServiceException(error);
         } else {
            int pauseTime = request.getRetryAfterHeader();
            throw new RetryCallException(pauseTime, responseCode);
         }
      } catch (RealmsHttpException e) {
         throw new RealmsServiceException(RealmsError.CustomError.connectivityError(e));
      }
   }

   public enum CompatibleVersionResponse {
      COMPATIBLE,
      OUTDATED,
      OTHER;
   }

   public enum Environment {
      PRODUCTION("pc.realms.minecraft.net", "java.frontendlegacy.realms.minecraft-services.net", "https"),
      STAGE("pc-stage.realms.minecraft.net", "java.frontendlegacy.stage-c2a40e62.realms.minecraft-services.net", "https"),
      LOCAL("localhost:8080", "localhost:8080", "http");

      public final String baseUrl;
      public final String alternativeUrl;
      public final String protocol;

      Environment(final String baseUrl, final String alternativeUrl, final String protocol) {
         this.baseUrl = baseUrl;
         this.alternativeUrl = alternativeUrl;
         this.protocol = protocol;
      }

      public static Optional<RealmsClient.Environment> byName(final String name) {
         return switch (name.toLowerCase(Locale.ROOT)) {
            case "production" -> Optional.of(PRODUCTION);
            case "local" -> Optional.of(LOCAL);
            case "stage", "staging" -> Optional.of(STAGE);
            default -> Optional.empty();
         };
      }
   }
}
