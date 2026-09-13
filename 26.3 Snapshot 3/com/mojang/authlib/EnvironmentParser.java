package com.mojang.authlib;

import com.mojang.authlib.services.MinecraftServicesEnvironment;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentParser {
   @Nullable
   private static String environmentOverride;
   private static final String PROP_PREFIX = "minecraft.api.";
   private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentParser.class);
   public static final String PROP_ENV = "minecraft.api.env";
   public static final String PROP_DISCOVERY_HOST = "minecraft.api.discovery.host";

   public static void setEnvironmentOverride(@Nullable String override) {
      environmentOverride = override;
   }

   public static Optional<Environment> getEnvironmentFromProperties() {
      String envName = environmentOverride != null ? environmentOverride : System.getProperty("minecraft.api.env");
      Optional<Environment> env = MinecraftServicesEnvironment.fromString(envName);
      return env.isPresent() ? env : fromHostNames();
   }

   private static Optional<Environment> fromHostNames() {
      String discovery = System.getProperty("minecraft.api.discovery.host");
      if (discovery != null) {
         return Optional.of(new Environment(discovery, "properties"));
      }

      LOGGER.info("Ignoring hosts properties. {} or {} needs to be set", "minecraft.api.discovery.host", "minecraft.api.env");
      return Optional.empty();
   }
}
