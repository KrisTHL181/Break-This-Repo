package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public record WorldDownload(String downloadLink, String resourcePackUrl, String resourcePackHash) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static WorldDownload parse(final String json) {
      JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();

      try {
         return new WorldDownload(
            JsonUtils.getStringOr("downloadLink", jsonObject, ""),
            JsonUtils.getStringOr("resourcePackUrl", jsonObject, ""),
            JsonUtils.getStringOr("resourcePackHash", jsonObject, "")
         );
      } catch (Exception e) {
         LOGGER.error("Could not parse WorldDownload", e);
         return new WorldDownload("", "", "");
      }
   }
}
