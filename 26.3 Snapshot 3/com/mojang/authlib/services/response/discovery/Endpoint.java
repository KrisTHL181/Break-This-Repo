package com.mojang.authlib.services.response.discovery;

import java.util.List;

public record Endpoint(String uri, List<String> validUris) {
   public Endpoint(String uri) {
      this(uri, List.of());
   }
}
