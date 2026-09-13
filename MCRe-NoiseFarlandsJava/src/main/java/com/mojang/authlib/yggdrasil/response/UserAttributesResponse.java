package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.yggdrasil.ChatToggleValue;
import com.mojang.authlib.yggdrasil.ToggleValue;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserAttributesResponse(
    @SerializedName("privileges")
    @Nullable Privileges privileges,
    @SerializedName("profanityFilterPreferences")
    @Nullable ProfanityFilterPreferences profanityFilterPreferences,
    @SerializedName("friendsPreferences")
    @Nullable FriendsPreferences friendsPreferences,
    @SerializedName("chatPreferences")
    @Nullable ChatPreferences chatPreferences,
    @SerializedName("banStatus")
    @Nullable BanStatus banStatus
) {
    public record Privileges(
        @SerializedName("onlineChat")
        @Nullable Privilege onlineChat,
        @SerializedName("multiplayerServer")
        @Nullable Privilege multiplayerServer,
        @SerializedName("multiplayerRealms")
        @Nullable Privilege multiplayerRealms,
        @SerializedName("telemetry")
        @Nullable Privilege telemetry,
        @SerializedName("optionalTelemetry")
        @Nullable Privilege optionalTelemetry
    ) {
        /**
         * @deprecated Use {@link UserAttributesResponse#chatPreferences()}.{@link ChatPreferences#textCommunication()}
         */
        @Deprecated
        public boolean getOnlineChat() {
            return onlineChat != null && onlineChat.enabled;
        }

        public boolean getMultiplayerServer() {
            return multiplayerServer != null && multiplayerServer.enabled;
        }

        public boolean getMultiplayerRealms() {
            return multiplayerRealms != null && multiplayerRealms.enabled;
        }

        public boolean getTelemetry() {
            return telemetry != null && telemetry.enabled;
        }

        public boolean getOptionalTelemetry() {
            return optionalTelemetry != null && optionalTelemetry.enabled;
        }

        public record Privilege(
            @SerializedName("enabled")
            boolean enabled
        ) {
        }
    }

    public record ProfanityFilterPreferences(
        @SerializedName("profanityFilterOn")
        boolean enabled
    ) {
    }

    public record FriendsPreferences(
        @SerializedName("friends")
        ToggleValue friends,
        @SerializedName("acceptInvites")
        ToggleValue acceptInvites
    ) {
    }

    public record ChatPreferences(
        @SerializedName("textCommunication")
        ChatToggleValue textCommunication
    ) {
    }

    public record BanStatus(
        @SerializedName("bannedScopes")
        Map<String, BannedScope> bannedScopes
    ) {
        public record BannedScope(
            @SerializedName("banId")
            UUID banId,
            @SerializedName("expires")
            @Nullable Instant expires,
            @SerializedName("reason")
            String reason,
            @SerializedName("reasonMessage")
            @Nullable String reasonMessage
        ) {
        }
    }
}
