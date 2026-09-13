package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.yggdrasil.request.JoinInfoUpdate;
import com.mojang.authlib.yggdrasil.response.FriendData;
import com.mojang.authlib.yggdrasil.response.PresenceResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public interface FriendsService {

    ResultCode getFriendData(Consumer<FriendData> friendData);
    ResultCode removeFriend(UUID playerID);

    ResultCode acceptIncomingFriendRequest(UUID id);
    ResultCode declineIncomingFriendRequest(UUID id);

    ResultCode sendFriendRequest(String name);
    ResultCode sendFriendRequest(UUID playerID);
    ResultCode revokeOutgoingFriendRequest(UUID id);

    ResultCode updateFriendSettings(boolean enableFriendlist, boolean enableFriendInvites);

    PresenceResponse presence(String status);
    PresenceResponse presence(String status, @Nullable JoinInfoUpdate joinInfoUpdate);

    default Optional<Duration> getFriendsPollInterval() {
        return Optional.empty();
    }

    default Optional<Duration> getPresencePollInterval() {
        return Optional.empty();
    }

    record PlayerData(UUID id, String name) {}

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
        GENERIC_ERROR
    }
}