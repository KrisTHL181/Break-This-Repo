package com.mojang.authlib.services.response.discovery;

import java.util.Map;

public record Endpoints(Map<String, Endpoint> endpoints) {
   public static Endpoints empty() {
      return new Endpoints(Map.of());
   }
}
