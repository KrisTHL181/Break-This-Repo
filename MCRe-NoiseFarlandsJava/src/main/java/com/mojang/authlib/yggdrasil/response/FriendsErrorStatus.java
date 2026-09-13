package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

/**
 * Status codes returned in the "details.status" field of a 400 Bad Request error from /friends.
 * Example error body:
 * {
 *   "path": "/friends",
 *   "details": { "status": "CANNOT_ADD_SELF" },
 *   "errorMessage": "You cannot add yourself to your friend list"
 * }
 */
public enum FriendsErrorStatus {
    @SerializedName("UNKNOWN_PROFILE")
    UNKNOWN_PROFILE,
    @SerializedName("CANNOT_ADD_SELF")
    CANNOT_ADD_SELF,
    @SerializedName("DUPLICATED_PROFILES")
    DUPLICATED_PROFILES
}

