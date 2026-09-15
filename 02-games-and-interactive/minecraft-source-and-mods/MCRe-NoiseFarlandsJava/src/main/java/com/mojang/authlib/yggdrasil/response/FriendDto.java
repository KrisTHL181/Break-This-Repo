package com.mojang.authlib.yggdrasil.response;

import java.util.UUID;

public record FriendDto(
    UUID profileId,
    String name
) {
}
