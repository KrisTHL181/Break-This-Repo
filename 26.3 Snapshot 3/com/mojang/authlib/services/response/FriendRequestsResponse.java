package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.services.FriendsService;
import java.util.List;

public record FriendRequestsResponse(@SerializedName("requests") List<FriendsService.PlayerData> requests) {
}
