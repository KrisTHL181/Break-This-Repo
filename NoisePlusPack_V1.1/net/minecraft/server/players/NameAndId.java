package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.Nullable;

public record NameAndId(UUID id, String name) {
   public static final Codec<NameAndId> CODEC = RecordCodecBuilder.create(
      p_429976_ -> p_429976_.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(NameAndId::id), Codec.STRING.fieldOf("name").forGetter(NameAndId::name))
         .apply(p_429976_, NameAndId::new)
   );

   public NameAndId(GameProfile p_424833_) {
      this(p_424833_.id(), p_424833_.name());
   }

   public NameAndId(com.mojang.authlib.yggdrasil.response.NameAndId p_428936_) {
      this(p_428936_.id(), p_428936_.name());
   }

   public static @Nullable NameAndId fromJson(JsonObject p_425683_) {
      if (p_425683_.has("uuid") && p_425683_.has("name")) {
         String s = p_425683_.get("uuid").getAsString();

         UUID uuid;
         try {
            uuid = UUID.fromString(s);
         } catch (Throwable throwable) {
            return null;
         }

         return new NameAndId(uuid, p_425683_.get("name").getAsString());
      } else {
         return null;
      }
   }

   public void appendTo(JsonObject p_423921_) {
      p_423921_.addProperty("uuid", this.id().toString());
      p_423921_.addProperty("name", this.name());
   }

   public static NameAndId createOffline(String p_424701_) {
      UUID uuid = UUIDUtil.createOfflinePlayerUUID(p_424701_);
      return new NameAndId(uuid, p_424701_);
   }
}
