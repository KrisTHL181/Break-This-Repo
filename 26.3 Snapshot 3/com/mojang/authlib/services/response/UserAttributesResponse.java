package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.services.ToggleValue;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserAttributesResponse(
   @Nullable @SerializedName("privileges") UserAttributesResponse.Privileges privileges,
   @Nullable @SerializedName("profanityFilterPreferences") UserAttributesResponse.ProfanityFilterPreferences profanityFilterPreferences,
   @Nullable @SerializedName("friendsPreferences") UserAttributesResponse.FriendsPreferences friendsPreferences,
   @Nullable @SerializedName("chatPreferences") UserAttributesResponse.ChatPreferences chatPreferences,
   @Nullable @SerializedName("banStatus") UserAttributesResponse.BanStatus banStatus
) {
   public record BanStatus(@SerializedName("bannedScopes") Map<String, UserAttributesResponse.BanStatus.BannedScope> bannedScopes) {
      public record BannedScope(
         @SerializedName("banId") UUID banId,
         @Nullable @SerializedName("expires") Instant expires,
         @SerializedName("reason") String reason,
         @Nullable @SerializedName("reasonMessage") String reasonMessage
      ) {
      }
   }

   public record ChatPreferences(@SerializedName("textCommunication") ChatToggleValue textCommunication) {
   }

   public record FriendsPreferences(@SerializedName("friends") ToggleValue friends, @SerializedName("acceptInvites") ToggleValue acceptInvites) {
   }

   public record Privileges(
      @Nullable @SerializedName("onlineChat") UserAttributesResponse.Privileges.Privilege onlineChat,
      @Nullable @SerializedName("multiplayerServer") UserAttributesResponse.Privileges.Privilege multiplayerServer,
      @Nullable @SerializedName("multiplayerRealms") UserAttributesResponse.Privileges.Privilege multiplayerRealms,
      @Nullable @SerializedName("telemetry") UserAttributesResponse.Privileges.Privilege telemetry,
      @Nullable @SerializedName("optionalTelemetry") UserAttributesResponse.Privileges.Privilege optionalTelemetry
   ) {
      @Deprecated
      public boolean getOnlineChat() {
         return this.onlineChat != null && this.onlineChat.enabled;
      }

      public boolean getMultiplayerServer() {
         return this.multiplayerServer != null && this.multiplayerServer.enabled;
      }

      public boolean getMultiplayerRealms() {
         return this.multiplayerRealms != null && this.multiplayerRealms.enabled;
      }

      public boolean getTelemetry() {
         return this.telemetry != null && this.telemetry.enabled;
      }

      public boolean getOptionalTelemetry() {
         return this.optionalTelemetry != null && this.optionalTelemetry.enabled;
      }

      public record Privilege(@SerializedName("enabled") boolean enabled) {
      }
   }

   public record ProfanityFilterPreferences(@SerializedName("profanityFilterOn") boolean enabled) {
   }
}
