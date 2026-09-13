package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsNews(@Nullable String newsLink) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsNews parse(String p_87472_) {
      String s = null;

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87472_).getAsJsonObject();
         s = JsonUtils.getStringOr("newsLink", jsonobject, null);
      } catch (Exception exception) {
         LOGGER.error("Could not parse RealmsNews", exception);
      }

      return new RealmsNews(s);
   }
}
