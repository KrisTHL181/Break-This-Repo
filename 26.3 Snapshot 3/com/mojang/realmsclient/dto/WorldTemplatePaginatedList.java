package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public record WorldTemplatePaginatedList(List<WorldTemplate> templates, int page, int size, int total) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public WorldTemplatePaginatedList(final int size) {
      this(List.of(), 0, size, -1);
   }

   public boolean isLastPage() {
      return this.page * this.size >= this.total && this.page > 0 && this.total > 0 && this.size > 0;
   }

   public static WorldTemplatePaginatedList parse(final String json) {
      List<WorldTemplate> templates = new ArrayList<>();
      int page = 0;
      int size = 0;
      int total = 0;

      try {
         JsonObject object = LenientJsonParser.parse(json).getAsJsonObject();
         if (object.get("templates").isJsonArray()) {
            for (JsonElement element : object.get("templates").getAsJsonArray()) {
               WorldTemplate template = WorldTemplate.parse(element.getAsJsonObject());
               if (template != null) {
                  templates.add(template);
               }
            }
         }

         page = JsonUtils.getIntOr("page", object, 0);
         size = JsonUtils.getIntOr("size", object, 0);
         total = JsonUtils.getIntOr("total", object, 0);
      } catch (Exception e) {
         LOGGER.error("Could not parse WorldTemplatePaginatedList", e);
      }

      return new WorldTemplatePaginatedList(templates, page, size, total);
   }
}
