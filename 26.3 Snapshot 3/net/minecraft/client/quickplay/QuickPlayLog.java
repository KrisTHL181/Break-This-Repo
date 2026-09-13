package net.minecraft.client.quickplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class QuickPlayLog {
   private static final QuickPlayLog INACTIVE = new QuickPlayLog("") {
      @Override
      public void log(final Minecraft minecraft) {
      }

      @Override
      public void setWorldData(final QuickPlayLog.Type type, final String id, final String name) {
      }
   };
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private final Path path;
   private QuickPlayLog.@Nullable QuickPlayWorld worldData;

   private QuickPlayLog(final String quickPlayPath) {
      this.path = Minecraft.getInstance().gameDirectory.toPath().resolve(quickPlayPath);
   }

   public static QuickPlayLog of(final @Nullable String path) {
      return path == null ? INACTIVE : new QuickPlayLog(path);
   }

   public void setWorldData(final QuickPlayLog.Type type, final String id, final String name) {
      this.worldData = new QuickPlayLog.QuickPlayWorld(type, id, name);
   }

   public void log(final Minecraft minecraft) {
      if (minecraft.gameMode != null && this.worldData != null) {
         Util.ioPool()
            .execute(
               () -> {
                  try {
                     Files.deleteIfExists(this.path);
                  } catch (IOException e) {
                     LOGGER.error("Failed to delete quickplay log file {}", this.path, e);
                  }

                  QuickPlayLog.QuickPlayEntry quickPlayEntry = new QuickPlayLog.QuickPlayEntry(
                     this.worldData, Instant.now(), minecraft.gameMode.getPlayerMode()
                  );
                  Codec.list(QuickPlayLog.QuickPlayEntry.CODEC)
                     .encodeStart(JsonOps.INSTANCE, List.of(quickPlayEntry))
                     .resultOrPartial(Util.prefix("Quick Play: ", LOGGER::error))
                     .ifPresent(json -> {
                        try {
                           Files.createDirectories(this.path.getParent());
                           Files.writeString(this.path, GSON.toJson(json));
                        } catch (IOException e) {
                           LOGGER.error("Failed to write to quickplay log file {}", this.path, e);
                        }
                     });
               }
            );
      } else {
         LOGGER.error("Failed to log session for quickplay. Missing world data or gamemode");
      }
   }

   private record QuickPlayEntry(QuickPlayLog.QuickPlayWorld quickPlayWorld, Instant lastPlayedTime, GameType gamemode) {
      public static final Codec<QuickPlayLog.QuickPlayEntry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               QuickPlayLog.QuickPlayWorld.MAP_CODEC.forGetter(QuickPlayLog.QuickPlayEntry::quickPlayWorld),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("lastPlayedTime").forGetter(QuickPlayLog.QuickPlayEntry::lastPlayedTime),
               GameType.CODEC.fieldOf("gamemode").forGetter(QuickPlayLog.QuickPlayEntry::gamemode)
            )
            .apply(i, QuickPlayLog.QuickPlayEntry::new)
      );
   }

   private record QuickPlayWorld(QuickPlayLog.Type type, String id, String name) {
      public static final MapCodec<QuickPlayLog.QuickPlayWorld> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               QuickPlayLog.Type.CODEC.fieldOf("type").forGetter(QuickPlayLog.QuickPlayWorld::type),
               ExtraCodecs.ESCAPED_STRING.fieldOf("id").forGetter(QuickPlayLog.QuickPlayWorld::id),
               Codec.STRING.fieldOf("name").forGetter(QuickPlayLog.QuickPlayWorld::name)
            )
            .apply(i, QuickPlayLog.QuickPlayWorld::new)
      );
   }

   public enum Type implements StringRepresentable {
      SINGLEPLAYER("singleplayer"),
      MULTIPLAYER("multiplayer"),
      REALMS("realms");

      private static final Codec<QuickPlayLog.Type> CODEC = StringRepresentable.fromEnum(QuickPlayLog.Type::values);
      private final String name;

      Type(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
