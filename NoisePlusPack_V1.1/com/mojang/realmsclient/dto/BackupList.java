package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record BackupList(List<Backup> backups) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static BackupList parse(String p_87410_) {
      List<Backup> list = new ArrayList<>();

      try {
         JsonElement jsonelement = LenientJsonParser.parse(p_87410_).getAsJsonObject().get("backups");
         if (jsonelement.isJsonArray()) {
            for (JsonElement jsonelement1 : jsonelement.getAsJsonArray()) {
               Backup backup = Backup.parse(jsonelement1);
               if (backup != null) {
                  list.add(backup);
               }
            }
         }
      } catch (Exception exception) {
         LOGGER.error("Could not parse BackupList", exception);
      }

      return new BackupList(list);
   }
}
