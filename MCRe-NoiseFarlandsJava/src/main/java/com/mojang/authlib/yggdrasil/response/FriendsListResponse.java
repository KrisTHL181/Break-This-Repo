package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response for GET /friends
 * Returns friends, incoming and outgoing friend requests in a single call.
 * Example:
 * {
 *   "friends": [ { "profileId": "uuid1", "name": "foo1" } ],
 *   "incomingRequest": [ { "profileId": "uuid3", "name": "foo3" } ],
 *   "outgoingRequest": [ { "profileId": "uuid5", "name": "foo5" } ]
 * }
 */
public record FriendsListResponse(
    @SerializedName("friends")
    List<FriendDto> friends,
    @SerializedName("incomingRequests")
    List<FriendDto> incomingRequests,
    @SerializedName("outgoingRequests")
    List<FriendDto> outgoingRequests
) {
}

