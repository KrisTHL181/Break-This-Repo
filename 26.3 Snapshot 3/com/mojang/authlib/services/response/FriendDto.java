package com.mojang.authlib.services.response;

import java.util.UUID;

public record FriendDto(UUID profileId, String name) {
}
