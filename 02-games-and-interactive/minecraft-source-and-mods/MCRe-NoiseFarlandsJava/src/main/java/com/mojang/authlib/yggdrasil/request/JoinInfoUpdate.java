package com.mojang.authlib.yggdrasil.request;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record JoinInfoUpdate(String value, @Nullable Set<UUID> invites) {
}