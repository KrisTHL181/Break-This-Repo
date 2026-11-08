package com.mojang.authlib.services.response;

import java.util.List;

public record PresenceResponse(List<PresenceStatusDto> presence) {
   public static PresenceResponse empty() {
      return new PresenceResponse(List.of());
   }
}
