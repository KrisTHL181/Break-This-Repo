package com.mojang.authlib;

import com.mojang.authlib.minecraft.SessionService;

public interface DiscoveryService {
   SessionService createMinecraftSessionService();

   GameProfileRepository createProfileRepository();
}
