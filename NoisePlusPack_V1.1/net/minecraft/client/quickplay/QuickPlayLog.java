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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class QuickPlayLog {
   private static final QuickPlayLog INACTIVE = new QuickPlayLog("") {
      @Override
      public void log(Minecraft p_279484_) {
      }

      @Override
      public void setWorldData(QuickPlayLog.Type p_279348_, String p_279305_, String p_279177_) {
      }
   };
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private final Path path;
   private QuickPlayLog.@Nullable QuickPlayWorld worldData;

   QuickPlayLog(String p_279463_) {
      this.path = Minecraft.getInstance().gameDirectory.toPath().resolve(p_279463_);
   }

   public static QuickPlayLog of(@Nullable String p_279275_) {
      return p_279275_ == null ? INACTIVE : new QuickPlayLog(p_279275_);
   }

   public void setWorldData(QuickPlayLog.Type p_279380_, String p_279427_, String p_279470_) {
      this.worldData = new QuickPlayLog.QuickPlayWorld(p_279380_, p_279427_, p_279470_);
   }

   public void log(Minecraft p_279258_) {
      if (p_279258_.gameMode != null && this.worldData != null) {
         Util.ioPool()
            .execute(
               () -> {
                  try {
                     Files.deleteIfExists(this.path);
                  } catch (IOException ioexception) {
                     LOGGER.error("Failed to delete quickplay log file {}", this.path, ioexception);
                  }

                  QuickPlayLog.QuickPlayEntry quickplaylog$quickplayentry = new QuickPlayLog.QuickPlayEntry(
                     this.worldData, Instant.now(), p_279258_.gameMode.getPlayerMode()
                  );
                  Codec.list(QuickPlayLog.QuickPlayEntry.CODEC)
                     .encodeStart(JsonOps.INSTANCE, List.of(quickplaylog$quickplayentry))
                     .resultOrPartial(Util.prefix("Quick Play: ", LOGGER::error))
                     .ifPresent(p_279238_ -> {
                        try {
                           Files.createDirectories(this.path.getParent());
                           Files.writeString(this.path, GSON.toJson(p_279238_));
                        } catch (IOException ioexception1) {
                           LOGGER.error("Failed to write to quickplay log file {}", this.path, ioexception1);
                        }
                     });
               }
            );
      } else {
         LOGGER.error("Failed to log session for quickplay. Missing world data or gamemode");
      }
   }

   @OnlyIn(Dist.CLIENT)
   record QuickPlayEntry(QuickPlayLog.QuickPlayWorld quickPlayWorld, Instant lastPlayedTime, GameType gamemode) {
      public static final Codec<QuickPlayLog.QuickPlayEntry> CODEC = RecordCodecBuilder.create(
         p_279196_ -> p_279196_.group(
               QuickPlayLog.QuickPlayWorld.MAP_CODEC.forGetter(QuickPlayLog.QuickPlayEntry::quickPlayWorld),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("lastPlayedTime").forGetter(QuickPlayLog.QuickPlayEntry::lastPlayedTime),
               GameType.CODEC.fieldOf("gamemode").forGetter(QuickPlayLog.QuickPlayEntry::gamemode)
            )
            .apply(p_279196_, QuickPlayLog.QuickPlayEntry::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   record QuickPlayWorld(QuickPlayLog.Type type, String id, String name) {
      public static final MapCodec<QuickPlayLog.QuickPlayWorld> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_296245_ -> p_296245_.group(
               QuickPlayLog.Type.CODEC.fieldOf("type").forGetter(QuickPlayLog.QuickPlayWorld::type),
               ExtraCodecs.ESCAPED_STRING.fieldOf("id").forGetter(QuickPlayLog.QuickPlayWorld::id),
               Codec.STRING.fieldOf("name").forGetter(QuickPlayLog.QuickPlayWorld::name)
            )
            .apply(p_296245_, QuickPlayLog.QuickPlayWorld::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   public enum Type implements StringRepresentable {
      SINGLEPLAYER("singleplayer"),
      MULTIPLAYER("multiplayer"),
      REALMS("realms");

      static final Codec<QuickPlayLog.Type> CODEC = StringRepresentable.fromEnum(QuickPlayLog.Type::values);
      private final String name;

      Type(final String p_279349_) {
         this.name = p_279349_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
