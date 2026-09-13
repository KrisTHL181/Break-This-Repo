package com.mojang.authlib;

import com.mojang.authlib.minecraft.MinecraftSessionService;

public interface AuthenticationService {
    /**
     * Creates a relevant {@link com.mojang.authlib.minecraft.MinecraftSessionService} designed for this authentication service.
     * </p>
     * This is a Minecraft specific service and is not relevant to any other game agent.
     *
     * @return New minecraft session service
     */
    MinecraftSessionService createMinecraftSessionService();

    /**
     * Creates a relevant {@link com.mojang.authlib.GameProfileRepository} designed for this authentication service.
     *
     * @return New profile repository
     */
    GameProfileRepository createProfileRepository();
}
