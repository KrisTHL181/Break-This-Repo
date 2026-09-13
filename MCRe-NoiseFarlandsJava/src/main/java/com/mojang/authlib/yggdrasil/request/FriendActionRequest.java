package com.mojang.authlib.yggdrasil.request;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Request body for PUT /friends.
 * Used for all friend management operations:
 * - Remove a friend:                  updateType=REMOVE, profileId=friendId
 * - Accept incoming friend request:   updateType=ADD,    profileId=requesterId
 * - Decline incoming friend request:  updateType=REMOVE, profileId=requesterId
 * - Send friend request by name:      updateType=ADD,    name=nickname
 * - Send friend request by UUID:      updateType=ADD,    profileId=playerId
 * - Revoke outgoing friend request:   updateType=REMOVE, profileId=requesteeId
 */
public record FriendActionRequest(
    @SerializedName("name")
    @Nullable String name,
    @SerializedName("profileId")
    @Nullable UUID profileId,
    @SerializedName("updateType")
    UpdateType updateType
) {
    public static FriendActionRequest byId(final UUID id, final UpdateType action) {
        return new FriendActionRequest(null, id, action);
    }

    public static FriendActionRequest byName(final String name, final UpdateType action) {
        return new FriendActionRequest(name, null, action);
    }
}

