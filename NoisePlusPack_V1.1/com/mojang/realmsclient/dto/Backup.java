package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Backup extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public final String backupId;
   public final Instant lastModified;
   public final long size;
   public boolean uploadedVersion;
   public final Map<String, String> metadata;
   public final Map<String, String> changeList = new HashMap<>();

   private Backup(String p_456611_, Instant p_460708_, long p_450578_, Map<String, String> p_455907_) {
      this.backupId = p_456611_;
      this.lastModified = p_460708_;
      this.size = p_450578_;
      this.metadata = p_455907_;
   }

   public ZonedDateTime lastModifiedDate() {
      return ZonedDateTime.ofInstant(this.lastModified, ZoneId.systemDefault());
   }

   public static @Nullable Backup parse(JsonElement p_87400_) {
      JsonObject jsonobject = p_87400_.getAsJsonObject();

      try {
         String s = JsonUtils.getStringOr("backupId", jsonobject, "");
         Instant instant = JsonUtils.getDateOr("lastModifiedDate", jsonobject);
         long i = JsonUtils.getLongOr("size", jsonobject, 0L);
         Map<String, String> map = new HashMap<>();
         if (jsonobject.has("metadata")) {
            JsonObject jsonobject1 = jsonobject.getAsJsonObject("metadata");

            for (Entry<String, JsonElement> entry : jsonobject1.entrySet()) {
               if (!entry.getValue().isJsonNull()) {
                  map.put(entry.getKey(), entry.getValue().getAsString());
               }
            }
         }

         return new Backup(s, instant, i, map);
      } catch (Exception exception) {
         LOGGER.error("Could not parse Backup", exception);
         return null;
      }
   }
}
