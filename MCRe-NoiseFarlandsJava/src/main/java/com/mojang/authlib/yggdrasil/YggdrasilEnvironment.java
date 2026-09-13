package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public enum YggdrasilEnvironment {
    PROD(
        "https://sessionserver.mojang.com",
        "https://api.minecraftservices.com",
        "https://api.mojang.com"
    ),
    STAGING(
        "https://sessionserver-staging.mojang.com",
        "https://api-staging.minecraftservices.com",
        "https://api-staging.mojang.com"
    );

    private final Environment environment;

    YggdrasilEnvironment(final String sessionHost, final String servicesHost, final String profilesHost) {
        this.environment = new Environment(sessionHost, servicesHost, profilesHost, name());
    }

    public Environment getEnvironment() {
        return environment;
    }

    public static Optional<Environment> fromString(@Nullable final String value) {
        return Stream
            .of(YggdrasilEnvironment.values())
            .filter(env -> value != null && value.equalsIgnoreCase(env.name()))
            .findFirst()
            .map(YggdrasilEnvironment::getEnvironment);
    }
}
