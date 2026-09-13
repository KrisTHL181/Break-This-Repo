package com.mojang.authlib.services.request;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.services.ToggleValue;
import org.jspecify.annotations.Nullable;

public record UserAttributesRequest(
   @Nullable @SerializedName("profanityFilterPreferences") UserAttributesRequest.ProfanityFilterPreferences profanityFilterPreferences,
   @Nullable @SerializedName("friendsPreferences") UserAttributesRequest.FriendsPreferences friendsPreferences
) {
   public record FriendsPreferences(@SerializedName("friends") ToggleValue friends, @SerializedName("acceptInvites") ToggleValue acceptInvites) {
   }

   public record ProfanityFilterPreferences(@SerializedName("profanityFilterOn") boolean enabled) {
   }
}
