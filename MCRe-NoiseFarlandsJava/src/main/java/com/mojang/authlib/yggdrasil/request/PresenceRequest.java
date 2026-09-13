package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.yggdrasil.response.PresenceStatus;
import org.jspecify.annotations.Nullable;

public record PresenceRequest(PresenceStatus status, @Nullable JoinInfoUpdate joinInfo) {
}

