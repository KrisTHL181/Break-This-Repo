package com.mojang.authlib.yggdrasil.response;

import java.util.List;

/**
 * Response body for POST /presence.
 * Contains the current presence for every friend of the caller.
 */
public record PresenceResponse(List<PresenceStatusDto> presence) {

    public static PresenceResponse empty() {
        return new PresenceResponse(List.of());
    }
}

