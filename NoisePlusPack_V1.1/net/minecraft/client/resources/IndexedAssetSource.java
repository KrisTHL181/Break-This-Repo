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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class IndexedAssetSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Splitter PATH_SPLITTER = Splitter.on('/');

   public static Path createIndexFs(Path p_248776_, String p_250235_) {
      Path path = p_248776_.resolve("objects");
      LinkFileSystem.Builder linkfilesystem$builder = LinkFileSystem.builder();
      Path path1 = p_248776_.resolve("indexes/" + p_250235_ + ".json");

      try (BufferedReader bufferedreader = Files.newBufferedReader(path1, StandardCharsets.UTF_8)) {
         JsonObject jsonobject = GsonHelper.parse(bufferedreader);
         JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "objects", null);
         if (jsonobject1 != null) {
            for (Entry<String, JsonElement> entry : jsonobject1.entrySet()) {
               JsonObject jsonobject2 = (JsonObject)entry.getValue();
               String s = entry.getKey();
               List<String> list = PATH_SPLITTER.splitToList(s);
               String s1 = GsonHelper.getAsString(jsonobject2, "hash");
               Path path2 = path.resolve(s1.substring(0, 2) + "/" + s1);
               linkfilesystem$builder.put(list, path2);
            }
         }
      } catch (JsonParseException jsonparseexception) {
         LOGGER.error("Unable to parse resource index file: {}", path1);
      } catch (IOException ioexception) {
         LOGGER.error("Can't open the resource index file: {}", path1);
      }

      return linkfilesystem$builder.build("index-" + p_250235_).getPath("/");
   }
}
