package com.mojang.authlib.services.request;

import com.mojang.authlib.services.response.PresenceStatus;

public record PresenceRequest(PresenceStatus status) {
}
