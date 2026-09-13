package com.mojang.authlib.services;

import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.request.FriendActionRequest;
import com.mojang.authlib.services.request.PresenceRequest;
import com.mojang.authlib.services.request.UpdateType;
import com.mojang.authlib.services.request.UserAttributesRequest;
import com.mojang.authlib.services.response.FriendData;
import com.mojang.authlib.services.response.FriendsListResponse;
import com.mojang.authlib.services.response.PresenceResponse;
import com.mojang.authlib.services.response.PresenceStatus;
import com.mojang.authlib.services.response.UserAttributesResponse;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesFriendsService implements FriendsService {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesFriendsService.class);
   private static final int HTTP_TOO_MANY_REQUESTS = 429;
   private static final long REQUEST_COOLDOWN_SECONDS = 10L;
   private final MinecraftClient minecraftClient;
   private final MinecraftServicesDiscoveryService discoveryService;
   @Nullable
   private String friendsEtag;
   private FriendData friendsCache = FriendData.empty();
   @Nullable
   private String presenceEtag;
   private PresenceResponse presenceCache = PresenceResponse.empty();
   @Nullable
   private volatile Duration friendsPollInterval;
   @Nullable
   private volatile Duration presencePollInterval;
   private final AtomicBoolean requestPending = new AtomicBoolean();
   @Nullable
   private Instant requestCooldown;

   @Override
   public Optional<Duration> getFriendsPollInterval() {
      return Optional.ofNullable(this.friendsPollInterval);
   }

   @Override
   public Optional<Duration> getPresencePollInterval() {
      return Optional.ofNullable(this.presencePollInterval);
   }

   public MinecraftServicesFriendsService(String accessToken, Proxy proxy, MinecraftServicesDiscoveryService discoveryService) {
      this.discoveryService = discoveryService;
      this.minecraftClient = new MinecraftClient(accessToken, proxy);
   }

   @Override
   public FriendsService.ResultCode getFriendData(Consumer<FriendData> friendData) {
      boolean isRequesting = this.requestPending.getAndSet(true);
      if (!isRequesting) {
         FriendsService.ResultCode resultCode;
         if (this.canMakeRequest()) {
            this.requestCooldown = Instant.now().plusSeconds(10L);
            resultCode = this.requestFriendData(friendData);
         } else {
            friendData.accept(this.friendsCache);
            resultCode = FriendsService.ResultCode.SUCCESS;
         }

         this.requestPending.set(false);
         return resultCode;
      } else {
         while (isRequesting) {
            Thread.yield();
            isRequesting = this.requestPending.get();
         }

         friendData.accept(this.friendsCache);
         return FriendsService.ResultCode.SUCCESS;
      }
   }

   private FriendsService.ResultCode requestFriendData(Consumer<FriendData> friendData) {
      try {
         MinecraftClient.ServiceResponse<FriendsListResponse> response = this.minecraftClient
            .getWithEtag(
               HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "getFriends")), FriendsListResponse.class, this.friendsEtag
            );
         Duration retryAfter = response.retryAfter();
         if (retryAfter != null) {
            this.friendsPollInterval = retryAfter;
         }

         FriendsListResponse body = response.body();
         if (body == null) {
            friendData.accept(this.friendsCache);
            return FriendsService.ResultCode.SUCCESS;
         } else {
            this.friendsCache = new FriendData(
               body.friends() != null ? List.copyOf(body.friends()) : List.of(),
               body.incomingRequests() != null ? List.copyOf(body.incomingRequests()) : List.of(),
               body.outgoingRequests() != null ? List.copyOf(body.outgoingRequests()) : List.of()
            );
            this.friendsEtag = response.etag();
            friendData.accept(this.friendsCache);
            return FriendsService.ResultCode.SUCCESS;
         }
      } catch (MinecraftClientHttpException e) {
         FriendsService.ResultCode resultCode = this.handleHttpError(e);
         friendData.accept(this.friendsCache);
         return resultCode;
      } catch (MinecraftClientException e) {
         friendData.accept(this.friendsCache);
         return FriendsService.ResultCode.ERROR;
      }
   }

   private boolean canMakeRequest() {
      return this.requestCooldown == null || Instant.now().isAfter(this.requestCooldown);
   }

   @Override
   public FriendsService.ResultCode removeFriend(UUID playerID) {
      return this.putFriendAction(FriendActionRequest.byId(playerID, UpdateType.REMOVE));
   }

   @Override
   public FriendsService.ResultCode acceptIncomingFriendRequest(UUID id) {
      return this.putFriendAction(FriendActionRequest.byId(id, UpdateType.ADD));
   }

   @Override
   public FriendsService.ResultCode declineIncomingFriendRequest(UUID id) {
      return this.putFriendAction(FriendActionRequest.byId(id, UpdateType.REMOVE));
   }

   @Override
   public FriendsService.ResultCode sendFriendRequest(String name) {
      return this.putFriendAction(FriendActionRequest.byName(name, UpdateType.ADD));
   }

   @Override
   public FriendsService.ResultCode sendFriendRequest(UUID playerID) {
      return this.putFriendAction(FriendActionRequest.byId(playerID, UpdateType.ADD));
   }

   @Override
   public FriendsService.ResultCode revokeOutgoingFriendRequest(UUID id) {
      return this.putFriendAction(FriendActionRequest.byId(id, UpdateType.REMOVE));
   }

   @Override
   public FriendsService.ResultCode updateFriendSettings(boolean enableFriendlist, boolean enableFriendInvites) {
      try {
         UserAttributesRequest.FriendsPreferences friendsPreferences = new UserAttributesRequest.FriendsPreferences(
            enableFriendlist ? ToggleValue.ENABLED : ToggleValue.DISABLED, enableFriendInvites ? ToggleValue.ENABLED : ToggleValue.DISABLED
         );
         UserAttributesRequest request = new UserAttributesRequest(null, friendsPreferences);
         this.minecraftClient
            .post(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "updateAttributes")), request, UserAttributesResponse.class);
      } catch (MinecraftClientHttpException e) {
         return this.handleHttpError(e);
      } catch (MinecraftClientException e) {
         if (e.getType() == MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE) {
            return FriendsService.ResultCode.SERVICE_NOT_AVAILABLE;
         }

         return FriendsService.ResultCode.ERROR;
      }

      return FriendsService.ResultCode.SUCCESS;
   }

   private FriendsService.ResultCode putFriendAction(FriendActionRequest request) {
      try {
         FriendsListResponse response = this.minecraftClient
            .put(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "updateFriends")), request, FriendsListResponse.class);
         if (response != null) {
            this.requestCooldown = Instant.now().plusSeconds(10L);
            this.friendsEtag = null;
            this.friendsCache = new FriendData(
               response.friends() != null ? List.copyOf(response.friends()) : List.of(),
               response.incomingRequests() != null ? List.copyOf(response.incomingRequests()) : List.of(),
               response.outgoingRequests() != null ? List.copyOf(response.outgoingRequests()) : List.of()
            );
         }
      } catch (MinecraftClientHttpException e) {
         return this.handleHttpError(e);
      } catch (MinecraftClientException e) {
         if (e.getType() == MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE) {
            return FriendsService.ResultCode.SERVICE_NOT_AVAILABLE;
         }

         return FriendsService.ResultCode.ERROR;
      }

      return FriendsService.ResultCode.SUCCESS;
   }

   @Override
   public PresenceResponse presence(String status) {
      try {
         MinecraftClient.ServiceResponse<PresenceResponse> response = this.minecraftClient
            .postWithEtag(
               HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "updatePresence")),
               new PresenceRequest(PresenceStatus.valueOf(status)),
               PresenceResponse.class,
               this.presenceEtag
            );
         Duration retryAfter = response.retryAfter();
         if (retryAfter != null) {
            this.presencePollInterval = retryAfter;
         }

         PresenceResponse body = response.body();
         if (body == null) {
            this.presenceEtag = response.etag();
            return this.presenceCache;
         } else {
            PresenceResponse responseToCache = body.presence() == null ? PresenceResponse.empty() : new PresenceResponse(List.copyOf(body.presence()));
            this.presenceEtag = response.etag();
            this.presenceCache = responseToCache;
            return responseToCache;
         }
      } catch (MinecraftClientHttpException e) {
         this.handleHttpError(e);
         return this.presenceCache;
      } catch (MinecraftClientException e) {
         return this.presenceCache;
      }
   }

   private FriendsService.ResultCode handleHttpError(MinecraftClientHttpException e) {
      int status = e.getStatus();
      if (status == 429) {
         LOGGER.warn("Friends service rate-limited (429) — back off before retrying");
         return FriendsService.ResultCode.TOO_MANY_REQUESTS;
      } else if (status >= 500) {
         LOGGER.warn("Friends service unavailable ({}) — retry later", status);
         return FriendsService.ResultCode.SERVICE_NOT_AVAILABLE;
      } else if (status == 403) {
         LOGGER.warn("Friends service forbidden (403) — user may lack an active profile");
         return FriendsService.ResultCode.FORBIDDEN;
      } else if (status == 400) {
         LOGGER.warn("Friends service bad request (400) — Name or profile does not exist");
         return FriendsService.ResultCode.UNKNOWN_PROFILE;
      } else if (status == 401) {
         LOGGER.warn("Friends service unauthorized (401) — Invalid token");
         return FriendsService.ResultCode.UNAUTHORIZED;
      } else {
         LOGGER.debug("Friends service returned HTTP {} — {}", status, e.getMessage());
         return FriendsService.ResultCode.ERROR;
      }
   }
}
