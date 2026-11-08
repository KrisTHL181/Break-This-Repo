package com.mojang.authlib.services;

import com.mojang.authlib.GameProfile;
import java.util.Set;

public record ProfileResult(GameProfile profile, Set<ProfileActionType> actions) {
   public ProfileResult(GameProfile profile) {
      this(profile, Set.of());
   }
}
