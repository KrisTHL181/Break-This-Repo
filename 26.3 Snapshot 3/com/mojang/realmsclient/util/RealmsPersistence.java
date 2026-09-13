package com.mojang.realmsclient.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsPersistence {
   private static final String FILE_NAME = "realms_persistence.json";
   private static final GuardedSerializer GSON = new GuardedSerializer();
   private static final Logger LOGGER = LogUtils.getLogger();

   public RealmsPersistence.RealmsPersistenceData read() {
      return readFile();
   }

   public void save(final RealmsPersistence.RealmsPersistenceData data) {
      writeFile(data);
   }

   public static RealmsPersistence.RealmsPersistenceData readFile() {
      Path file = getPathToData();

      try {
         String contents = Files.readString(file, StandardCharsets.UTF_8);
         RealmsPersistence.RealmsPersistenceData realmsPersistenceData = GSON.fromJson(contents, RealmsPersistence.RealmsPersistenceData.class);
         if (realmsPersistenceData != null) {
            return realmsPersistenceData;
         }
      } catch (NoSuchFileException var3) {
      } catch (Exception e) {
         LOGGER.warn("Failed to read Realms storage {}", file, e);
      }

      return new RealmsPersistence.RealmsPersistenceData();
   }

   public static void writeFile(final RealmsPersistence.RealmsPersistenceData data) {
      Path file = getPathToData();

      try {
         Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
      } catch (Exception var3) {
      }
   }

   private static Path getPathToData() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("realms_persistence.json");
   }

   public static class RealmsPersistenceData implements ReflectionBasedSerialization {
      @SerializedName("newsLink")
      public @Nullable String newsLink;
      @SerializedName("hasUnreadNews")
      public boolean hasUnreadNews;
   }
}
