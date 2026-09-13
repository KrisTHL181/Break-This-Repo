package com.mojang.authlib.services;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.services.request.AbuseReportRequest;
import com.mojang.authlib.services.response.BlockListResponse;
import com.mojang.authlib.services.response.KeyPairResponse;
import com.mojang.authlib.services.response.UserAttributesResponse;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.jspecify.annotations.Nullable;

public class MinecraftServicesUserApiService implements UserApiService {
   private static final long REQUEST_COOLDOWN_SECONDS = 120L;
   private static final UUID ZERO_UUID = new UUID(0L, 0L);
   private final MinecraftServicesDiscoveryService discoveryService;
   private final MinecraftClient minecraftClient;
   @Nullable
   private Instant nextAcceptableRequest;
   @Nullable
   private Set<UUID> blockList;

   public MinecraftServicesUserApiService(String accessToken, MinecraftServicesDiscoveryService discoveryService, Proxy proxy) {
      this.discoveryService = discoveryService;
      this.minecraftClient = new MinecraftClient(accessToken, proxy);
   }

   @Override
   public TelemetrySession newTelemetrySession(Executor executor) {
      return new MinecraftTelemetrySession(this.minecraftClient, this.discoveryService, executor);
   }

   @Override
   public KeyPairResponse getKeyPair() {
      try {
         return this.minecraftClient
            .post(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "getCertificates")), KeyPairResponse.class);
      } catch (MinecraftClientException e) {
         return null;
      }
   }

   @Override
   public boolean isBlockedPlayer(UUID playerID) {
      if (playerID.equals(ZERO_UUID)) {
         return false;
      }

      if (this.blockList == null) {
         this.blockList = this.fetchBlockList();
         if (this.blockList == null) {
            return false;
         }
      }

      return this.blockList.contains(playerID);
   }

   @Override
   public void refreshBlockList() {
      if (this.blockList == null || this.canMakeRequest()) {
         this.blockList = this.forceFetchBlockList();
      }
   }

   @Nullable
   private Set<UUID> fetchBlockList() {
      return !this.canMakeRequest() ? null : this.forceFetchBlockList();
   }

   private boolean canMakeRequest() {
      return this.nextAcceptableRequest == null || Instant.now().isAfter(this.nextAcceptableRequest);
   }

   private Set<UUID> forceFetchBlockList() {
      this.nextAcceptableRequest = Instant.now().plusSeconds(120L);

      try {
         BlockListResponse response = this.minecraftClient
            .get(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "getBlocklist")), BlockListResponse.class);
         return response == null ? Set.of() : response.blockedProfiles();
      } catch (MinecraftClientHttpException e) {
         return null;
      } catch (MinecraftClientException e) {
         return null;
      }
   }

   @Override
   public UserApiService.UserProperties fetchProperties() throws AuthenticationException {
      try {
         String url = this.discoveryService.getUrl(Service.PLAYER, "getAttributes");
         UserAttributesResponse response = this.minecraftClient.get(HttpDiscoveryService.constantURL(url), UserAttributesResponse.class);
         Builder<UserApiService.UserFlag> flags = ImmutableSet.builder();
         com.google.common.collect.ImmutableMap.Builder<String, BanDetails> bannedScopes = ImmutableMap.builder();
         if (response != null) {
            UserAttributesResponse.Privileges privileges = response.privileges();
            if (privileges != null) {
               addFlagIfUserHasPrivilege(privileges.getOnlineChat(), UserApiService.UserFlag.CHAT_ALLOWED, flags);
               addFlagIfUserHasPrivilege(privileges.getMultiplayerServer(), UserApiService.UserFlag.SERVERS_ALLOWED, flags);
               addFlagIfUserHasPrivilege(privileges.getMultiplayerRealms(), UserApiService.UserFlag.REALMS_ALLOWED, flags);
               addFlagIfUserHasPrivilege(privileges.getTelemetry(), UserApiService.UserFlag.TELEMETRY_ENABLED, flags);
               addFlagIfUserHasPrivilege(privileges.getOptionalTelemetry(), UserApiService.UserFlag.OPTIONAL_TELEMETRY_AVAILABLE, flags);
            }

            UserAttributesResponse.ProfanityFilterPreferences profanityFilterPreferences = response.profanityFilterPreferences();
            if (profanityFilterPreferences != null && profanityFilterPreferences.enabled()) {
               flags.add(UserApiService.UserFlag.PROFANITY_FILTER_ENABLED);
            }

            UserAttributesResponse.FriendsPreferences friendsPreferences = response.friendsPreferences();
            if (friendsPreferences != null) {
               if (friendsPreferences.friends().isEnabled()) {
                  flags.add(UserApiService.UserFlag.FRIENDS_ENABLED);
               }

               if (friendsPreferences.acceptInvites().isEnabled()) {
                  flags.add(UserApiService.UserFlag.ACCEPT_FRIEND_INVITES);
               }
            }

            UserAttributesResponse.ChatPreferences chatPreferences = response.chatPreferences();
            if (chatPreferences != null) {
               if (chatPreferences.textCommunication().isEnabled()) {
                  flags.add(UserApiService.UserFlag.CHAT_ALLOWED);
               } else if (chatPreferences.textCommunication().isFriendsOnly()) {
                  flags.add(UserApiService.UserFlag.CHAT_ALLOWED);
                  flags.add(UserApiService.UserFlag.CHAT_FRIENDS_ONLY);
               }
            }

            if (response.banStatus() != null) {
               response.banStatus()
                  .bannedScopes()
                  .forEach(
                     (scopeType, scope) -> bannedScopes.put(scopeType, new BanDetails(scope.banId(), scope.expires(), scope.reason(), scope.reasonMessage()))
                  );
            }
         }

         return new UserApiService.UserProperties(flags.build(), bannedScopes.build());
      } catch (MinecraftClientHttpException e) {
         throw e.toAuthenticationException();
      } catch (MinecraftClientException e) {
         throw e.toAuthenticationException();
      }
   }

   private static void addFlagIfUserHasPrivilege(boolean privilege, UserApiService.UserFlag value, Builder<UserApiService.UserFlag> output) {
      if (privilege) {
         output.add(value);
      }
   }

   @Override
   public void reportAbuse(AbuseReportRequest request) {
      this.minecraftClient.post(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PLAYER, "sendReport")), request, Void.class);
   }

   @Override
   public boolean canSendReports() {
      return true;
   }

   @Override
   public AbuseReportLimits getAbuseReportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }
}
