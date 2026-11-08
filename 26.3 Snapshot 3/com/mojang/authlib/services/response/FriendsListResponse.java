package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record FriendsListResponse(
   @SerializedName("friends") List<FriendDto> friends,
   @SerializedName("incomingRequests") List<FriendDto> incomingRequests,
   @SerializedName("outgoingRequests") List<FriendDto> outgoingRequests
) {
}
