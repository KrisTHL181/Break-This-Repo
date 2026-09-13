package net.minecraft.client;

import com.mojang.util.UndashedUuid;
import java.util.Optional;
import java.util.UUID;

public class User {
   private final String name;
   private final UUID uuid;
   private final String accessToken;
   private final Optional<String> xuid;
   private final Optional<String> clientId;

   public User(final String name, final UUID uuid, final String accessToken, final Optional<String> xuid, final Optional<String> clientId) {
      this.name = name;
      this.uuid = uuid;
      this.accessToken = accessToken;
      this.xuid = xuid;
      this.clientId = clientId;
   }

   public String getSessionId() {
      return "token:" + this.accessToken + ":" + UndashedUuid.toString(this.uuid);
   }

   public UUID getProfileId() {
      return this.uuid;
   }

   public String getName() {
      return this.name;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public Optional<String> getClientId() {
      return this.clientId;
   }

   public Optional<String> getXuid() {
      return this.xuid;
   }
}
