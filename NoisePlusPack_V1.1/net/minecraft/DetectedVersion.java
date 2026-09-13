package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final WorldVersion BUILT_IN = createBuiltIn(UUID.randomUUID().toString().replaceAll("-", ""), "Development Version");

   public static WorldVersion createBuiltIn(String p_428778_, String p_429892_) {
      return createBuiltIn(p_428778_, p_429892_, true);
   }

   public static WorldVersion createBuiltIn(String p_425498_, String p_428529_, boolean p_429114_) {
      return new WorldVersion.Simple(
         p_425498_,
         p_428529_,
         new DataVersion(4671, "main"),
         SharedConstants.getProtocolVersion(),
         PackFormat.of(75, 0),
         PackFormat.of(94, 1),
         new Date(),
         p_429114_
      );
   }

   private static WorldVersion createFromJson(JsonObject p_405818_) {
      JsonObject jsonobject = GsonHelper.getAsJsonObject(p_405818_, "pack_version");
      return new WorldVersion.Simple(
         GsonHelper.getAsString(p_405818_, "id"),
         GsonHelper.getAsString(p_405818_, "name"),
         new DataVersion(GsonHelper.getAsInt(p_405818_, "world_version"), GsonHelper.getAsString(p_405818_, "series_id", "main")),
         GsonHelper.getAsInt(p_405818_, "protocol_version"),
         PackFormat.of(GsonHelper.getAsInt(jsonobject, "resource_major"), GsonHelper.getAsInt(jsonobject, "resource_minor")),
         PackFormat.of(GsonHelper.getAsInt(jsonobject, "data_major"), GsonHelper.getAsInt(jsonobject, "data_minor")),
         Date.from(ZonedDateTime.parse(GsonHelper.getAsString(p_405818_, "build_time")).toInstant()),
         GsonHelper.getAsBoolean(p_405818_, "stable")
      );
   }

   public static WorldVersion tryDetectVersion() {
      try (InputStream inputstream = DetectedVersion.class.getResourceAsStream("/version.json")) {
         if (inputstream == null) {
            LOGGER.warn("Missing version information!");
            return BUILT_IN;
         }

         try (InputStreamReader inputstreamreader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
            return createFromJson(GsonHelper.parse(inputstreamreader));
         }
      } catch (JsonParseException | IOException ioexception) {
         throw new IllegalStateException("Game version information is corrupt", ioexception);
      }
   }
}
