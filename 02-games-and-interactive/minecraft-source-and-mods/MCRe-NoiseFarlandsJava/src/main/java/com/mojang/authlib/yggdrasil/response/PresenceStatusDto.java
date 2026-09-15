package com.mojang.authlib.yggdrasil.response;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PresenceStatusDto(
    UUID profileId,
    UUID pmid,
    PresenceStatus status,
    @Nullable JoinInfo joinInfo,
    Instant lastUpdated
) {
    public record JoinInfo(String value, boolean invited) {
    }
}