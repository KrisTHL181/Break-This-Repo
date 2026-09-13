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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPersistence {
   private static final String FILE_NAME = "realms_persistence.json";
   private static final GuardedSerializer GSON = new GuardedSerializer();
   private static final Logger LOGGER = LogUtils.getLogger();

   public RealmsPersistence.RealmsPersistenceData read() {
      return readFile();
   }

   public void save(RealmsPersistence.RealmsPersistenceData p_167617_) {
      writeFile(p_167617_);
   }

   public static RealmsPersistence.RealmsPersistenceData readFile() {
      Path path = getPathToData();

      try {
         String s = Files.readString(path, StandardCharsets.UTF_8);
         RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = GSON.fromJson(s, RealmsPersistence.RealmsPersistenceData.class);
         if (realmspersistence$realmspersistencedata != null) {
            return realmspersistence$realmspersistencedata;
         }
      } catch (NoSuchFileException var3) {
      } catch (Exception exception) {
         LOGGER.warn("Failed to read Realms storage {}", path, exception);
      }

      return new RealmsPersistence.RealmsPersistenceData();
   }

   public static void writeFile(RealmsPersistence.RealmsPersistenceData p_90173_) {
      Path path = getPathToData();

      try {
         Files.writeString(path, GSON.toJson(p_90173_), StandardCharsets.UTF_8);
      } catch (Exception var3) {
      }
   }

   private static Path getPathToData() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("realms_persistence.json");
   }

   @OnlyIn(Dist.CLIENT)
   public static class RealmsPersistenceData implements ReflectionBasedSerialization {
      @SerializedName("newsLink")
      public @Nullable String newsLink;
      @SerializedName("hasUnreadNews")
      public boolean hasUnreadNews;
   }
}
