package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.yggdrasil.FriendsService.PlayerData;

import java.util.List;

/**
 * Response for GET /friends/requests/incoming or /friends/requests/outgoing
 * Example:
 * {
 *   "requests": [
 *     {
 *       "profileId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
 *       "name": "PlayerNickname"
 *     }
 *   ]
 * }
 */
public record FriendRequestsResponse(
    @SerializedName("requests")
    List<PlayerData> requests
) {
}

