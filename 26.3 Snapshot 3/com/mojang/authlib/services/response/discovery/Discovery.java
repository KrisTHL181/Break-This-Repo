package com.mojang.authlib.services.response.discovery;

public record Discovery(String product, Endpoints authentication, Endpoints session, Endpoints player, Endpoints profiles, Endpoints telemetry) {
   static Discovery offline() {
      return new Discovery("offline", Endpoints.empty(), Endpoints.empty(), Endpoints.empty(), Endpoints.empty(), Endpoints.empty());
   }

   public Endpoints mapEndpoints(Service service) {
      return switch (service) {
         case AUTHENTICATION -> this.authentication;
         case SESSION -> this.session;
         case PLAYER -> this.player;
         case PROFILES -> this.profiles;
         case TELEMETRY -> this.telemetry;
      };
   }
}
