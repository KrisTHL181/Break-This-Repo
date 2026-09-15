package com.mojang.authlib.services.response;

import java.util.List;

public record FriendData(List<FriendDto> friends, List<FriendDto> incomingRequests, List<FriendDto> outgoingRequests) {
   public static FriendData empty() {
      return new FriendData(List.of(), List.of(), List.of());
   }
}
