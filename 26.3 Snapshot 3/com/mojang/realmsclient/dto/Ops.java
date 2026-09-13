package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public record Ops(Set<String> ops) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Ops parse(final String json) {
      Set<String> ops = new HashSet<>();

      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         JsonElement opsArray = jsonObject.get("ops");
         if (opsArray.isJsonArray()) {
            for (JsonElement opsElement : opsArray.getAsJsonArray()) {
               ops.add(opsElement.getAsString());
            }
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse Ops", e);
      }

      return new Ops(ops);
   }
}
