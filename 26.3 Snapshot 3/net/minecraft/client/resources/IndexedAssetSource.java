package net.minecraft.client.resources;

import com.google.common.base.Splitter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public class IndexedAssetSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Splitter PATH_SPLITTER = Splitter.on('/');

   public static Path createIndexFs(final Path assetsDirectory, final String index) {
      Path objectsDirectory = assetsDirectory.resolve("objects");
      LinkFileSystem.Builder builder = LinkFileSystem.builder();
      Path indexFile = assetsDirectory.resolve("indexes/" + index + ".json");

      try (BufferedReader reader = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8)) {
         JsonObject root = GsonHelper.parse(reader);
         JsonObject objects = GsonHelper.getAsJsonObject(root, "objects", null);
         if (objects != null) {
            for (Entry<String, JsonElement> entry : objects.entrySet()) {
               JsonObject object = (JsonObject)entry.getValue();
               String filename = entry.getKey();
               List<String> path = PATH_SPLITTER.splitToList(filename);
               String hash = GsonHelper.getAsString(object, "hash");
               Path file = objectsDirectory.resolve(hash.substring(0, 2) + "/" + hash);
               builder.put(path, file);
            }
         }
      } catch (JsonParseException ignored) {
         LOGGER.error("Unable to parse resource index file: {}", indexFile);
      } catch (IOException ignored) {
         LOGGER.error("Can't open the resource index file: {}", indexFile);
      }

      return builder.build("index-" + index).getPath("/");
   }
}
