package com.mojang.authlib;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;

import static java.util.Arrays.asList;

public class EnvironmentParser {
    @Nullable
    private static String environmentOverride;

    public static void setEnvironmentOverride(@Nullable final String override) {
        environmentOverride = override;
    }

    private static final String PROP_PREFIX = "minecraft.api.";
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentParser.class);

    public static final String PROP_ENV = PROP_PREFIX + "env";
    public static final String PROP_SESSION_HOST = PROP_PREFIX + "session.host";
    public static final String PROP_SERVICES_HOST = PROP_PREFIX + "services.host";
    public static final String PROP_PROFILES_HOST = PROP_PREFIX + "profiles.host";

    public static Optional<Environment> getEnvironmentFromProperties() {
        final String envName = environmentOverride != null ? environmentOverride : System.getProperty(PROP_ENV);
        final Optional<Environment> env = YggdrasilEnvironment.fromString(envName);
        return env.isPresent() ? env : fromHostNames();

    }

    private static Optional<Environment> fromHostNames() {
        final String session = System.getProperty(PROP_SESSION_HOST);
        final String services = System.getProperty(PROP_SERVICES_HOST);
        final String profiles = System.getProperty(PROP_PROFILES_HOST);

        if (services != null && session != null && profiles != null) {
            return Optional.of(new Environment(session, services, profiles, "properties"));
        }
        if (services != null || session != null || profiles != null) {
            LOGGER.info(
                "Ignoring hosts properties. All need to be set: {}",
                List.of(PROP_SERVICES_HOST, PROP_SESSION_HOST, PROP_PROFILES_HOST)
            );
        }
        return Optional.empty();

    }

}
