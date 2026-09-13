package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record WorldDownload(String downloadLink, String resourcePackUrl, String resourcePackHash) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static WorldDownload parse(String p_87725_) {
      JsonObject jsonobject = LenientJsonParser.parse(p_87725_).getAsJsonObject();

      try {
         return new WorldDownload(
            JsonUtils.getStringOr("downloadLink", jsonobject, ""),
            JsonUtils.getStringOr("resourcePackUrl", jsonobject, ""),
            JsonUtils.getStringOr("resourcePackHash", jsonobject, "")
         );
      } catch (Exception exception) {
         LOGGER.error("Could not parse WorldDownload", exception);
         return new WorldDownload("", "", "");
      }
   }
}
