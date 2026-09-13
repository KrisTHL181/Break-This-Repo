package com.mojang.authlib.yggdrasil.request;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.yggdrasil.ToggleValue;

import javax.annotation.Nullable;

public record UserAttributesRequest(
    @SerializedName("profanityFilterPreferences")
    @Nullable ProfanityFilterPreferences profanityFilterPreferences,
    @SerializedName("friendsPreferences")
    @Nullable FriendsPreferences friendsPreferences
) {
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
}
