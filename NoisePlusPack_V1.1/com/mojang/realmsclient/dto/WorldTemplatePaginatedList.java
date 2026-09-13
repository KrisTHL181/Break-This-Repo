package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record WorldTemplatePaginatedList(List<WorldTemplate> templates, int page, int size, int total) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public WorldTemplatePaginatedList(int p_87761_) {
      this(List.of(), 0, p_87761_, -1);
   }

   public boolean isLastPage() {
      return this.page * this.size >= this.total && this.page > 0 && this.total > 0 && this.size > 0;
   }

   public static WorldTemplatePaginatedList parse(String p_87763_) {
      List<WorldTemplate> list = new ArrayList<>();
      int i = 0;
      int j = 0;
      int k = 0;

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87763_).getAsJsonObject();
         if (jsonobject.get("templates").isJsonArray()) {
            for (JsonElement jsonelement : jsonobject.get("templates").getAsJsonArray()) {
               WorldTemplate worldtemplate = WorldTemplate.parse(jsonelement.getAsJsonObject());
               if (worldtemplate != null) {
                  list.add(worldtemplate);
               }
            }
         }

         i = JsonUtils.getIntOr("page", jsonobject, 0);
         j = JsonUtils.getIntOr("size", jsonobject, 0);
         k = JsonUtils.getIntOr("total", jsonobject, 0);
      } catch (Exception exception) {
         LOGGER.error("Could not parse WorldTemplatePaginatedList", exception);
      }

      return new WorldTemplatePaginatedList(list, i, j, k);
   }
}
