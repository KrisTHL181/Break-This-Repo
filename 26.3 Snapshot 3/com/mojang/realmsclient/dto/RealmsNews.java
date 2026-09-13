package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record RealmsNews(@Nullable String newsLink) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsNews parse(final String json) {
      String newsLink = null;

      try {
         JsonObject object = LenientJsonParser.parse(json).getAsJsonObject();
         newsLink = JsonUtils.getStringOr("newsLink", object, null);
      } catch (Exception e) {
         LOGGER.error("Could not parse RealmsNews", e);
      }

      return new RealmsNews(newsLink);
   }
}
