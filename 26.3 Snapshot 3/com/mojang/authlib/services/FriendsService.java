package com.mojang.authlib.services;

import com.mojang.authlib.services.response.FriendData;
import com.mojang.authlib.services.response.PresenceResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface FriendsService {
   FriendsService.ResultCode getFriendData(Consumer<FriendData> var1);

   FriendsService.ResultCode removeFriend(UUID var1);

   FriendsService.ResultCode acceptIncomingFriendRequest(UUID var1);

   FriendsService.ResultCode declineIncomingFriendRequest(UUID var1);

   FriendsService.ResultCode sendFriendRequest(String var1);

   FriendsService.ResultCode sendFriendRequest(UUID var1);

   FriendsService.ResultCode revokeOutgoingFriendRequest(UUID var1);

   FriendsService.ResultCode updateFriendSettings(boolean var1, boolean var2);

   PresenceResponse presence(String var1);

   default Optional<Duration> getFriendsPollInterval() {
      return Optional.empty();
   }

   default Optional<Duration> getPresencePollInterval() {
      return Optional.empty();
   }

   record PlayerData(UUID id, String name) {
   }

   enum ResultCode {
      SUCCESS,
      ERROR,
      SERVICE_NOT_AVAILABLE,
      TOO_MANY_REQUESTS,
      FORBIDDEN,
      UPGRADE_NEEDED,
      CONNECTION_ISSUE,
      TEMPORARY_UNAVAILABLE,
      UNKNOWN_PROFILE,
      UNAUTHORIZED,
      GENERIC_ERROR;
   }
}
