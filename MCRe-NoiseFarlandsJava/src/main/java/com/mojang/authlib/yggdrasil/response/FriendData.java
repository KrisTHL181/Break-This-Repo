package com.mojang.authlib.yggdrasil.response;

import java.util.List;

/**
 * Snapshot of the friend list as returned by GET /friends.
 *
 * @param friends          confirmed friends
 * @param incomingRequests players who sent the user a friend request
 * @param outgoingRequests players to whom the user sent a friend request
 */
public record FriendData(
    List<FriendDto> friends,
    List<FriendDto> incomingRequests,
    List<FriendDto> outgoingRequests
) {
    public static FriendData empty() {
        return new FriendData(List.of(), List.of(), List.of());
    }
}
