package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.GameProfile;

import java.util.Set;

public record ProfileResult(
    GameProfile profile,
    Set<ProfileActionType> actions
) {
    public ProfileResult(final GameProfile profile) {
        this(profile, Set.of());
    }
}
