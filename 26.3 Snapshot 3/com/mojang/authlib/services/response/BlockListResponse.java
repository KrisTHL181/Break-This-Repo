package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import java.util.UUID;

public record BlockListResponse(@SerializedName("blockedProfiles") Set<UUID> blockedProfiles) {
}
