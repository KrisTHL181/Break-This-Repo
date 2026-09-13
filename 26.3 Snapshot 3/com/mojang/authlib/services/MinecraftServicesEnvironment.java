package com.mojang.authlib.services;

import com.mojang.authlib.Environment;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public enum MinecraftServicesEnvironment {
   PROD("https://discovery.minecraftservices.com/minecraft/client"),
   STAGING("https://discovery-staging.minecraftservices.com/minecraft/client");

   private final Environment environment;

   MinecraftServicesEnvironment(String discoveryUrl) {
      this.environment = new Environment(discoveryUrl, this.name());
   }

   public Environment getEnvironment() {
      return this.environment;
   }

   public static Optional<Environment> fromString(@Nullable String value) {
      return Optional.ofNullable(value).map(str -> {
         return switch (value.toLowerCase(Locale.ROOT)) {
            case "prod" -> PROD;
            case "staging" -> STAGING;
            default -> null;
         };
      }).map(MinecraftServicesEnvironment::getEnvironment);
   }
}
