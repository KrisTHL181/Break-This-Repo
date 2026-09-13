package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record WorldTemplate(
   String id,
   String name,
   String version,
   String author,
   String link,
   @Nullable String image,
   String trailer,
   String recommendedPlayers,
   WorldTemplate.WorldTemplateType type
) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static @Nullable WorldTemplate parse(JsonObject p_87739_) {
      try {
         String s = JsonUtils.getStringOr("type", p_87739_, null);
         return new WorldTemplate(
            JsonUtils.getStringOr("id", p_87739_, ""),
            JsonUtils.getStringOr("name", p_87739_, ""),
            JsonUtils.getStringOr("version", p_87739_, ""),
            JsonUtils.getStringOr("author", p_87739_, ""),
            JsonUtils.getStringOr("link", p_87739_, ""),
            JsonUtils.getStringOr("image", p_87739_, null),
            JsonUtils.getStringOr("trailer", p_87739_, ""),
            JsonUtils.getStringOr("recommendedPlayers", p_87739_, ""),
            s == null ? WorldTemplate.WorldTemplateType.WORLD_TEMPLATE : WorldTemplate.WorldTemplateType.valueOf(s)
         );
      } catch (Exception exception) {
         LOGGER.error("Could not parse WorldTemplate", exception);
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum WorldTemplateType {
      WORLD_TEMPLATE,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
