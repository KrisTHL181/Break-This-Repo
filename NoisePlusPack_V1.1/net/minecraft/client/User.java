package net.minecraft.client;

import com.mojang.util.UndashedUuid;
import java.util.Optional;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class User {
   private final String name;
   private final UUID uuid;
   private final String accessToken;
   private final Optional<String> xuid;
   private final Optional<String> clientId;

   public User(String p_193799_, UUID p_297254_, String p_193800_, Optional<String> p_193802_, Optional<String> p_193803_) {
      this.name = p_193799_;
      this.uuid = p_297254_;
      this.accessToken = p_193800_;
      this.xuid = p_193802_;
      this.clientId = p_193803_;
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
