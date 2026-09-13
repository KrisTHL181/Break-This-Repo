package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public record BackupList(List<Backup> backups) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static BackupList parse(final String json) {
      List<Backup> backups = new ArrayList<>();

      try {
         JsonElement node = LenientJsonParser.parse(json).getAsJsonObject().get("backups");
         if (node.isJsonArray()) {
            for (JsonElement element : node.getAsJsonArray()) {
               Backup entry = Backup.parse(element);
               if (entry != null) {
                  backups.add(entry);
               }
            }
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse BackupList", e);
      }

      return new BackupList(backups);
   }
}
