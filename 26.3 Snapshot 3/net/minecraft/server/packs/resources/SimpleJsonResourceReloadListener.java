package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DynamicOps<JsonElement> ops;
   private final Codec<T> codec;
   private final FileToIdConverter lister;

   protected SimpleJsonResourceReloadListener(final Codec<T> codec, final FileToIdConverter lister) {
      this(JsonOps.INSTANCE, codec, lister);
   }

   private SimpleJsonResourceReloadListener(final DynamicOps<JsonElement> ops, final Codec<T> codec, final FileToIdConverter lister) {
      this.ops = ops;
      this.codec = codec;
      this.lister = lister;
   }

   protected Map<Identifier, T> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      Map<Identifier, T> result = new HashMap<>();

      for (Entry<Identifier, Resource> entry : this.lister.listMatchingResources(manager).entrySet()) {
         Identifier location = entry.getKey();
         Identifier id = this.lister.fileToId(location);

         try (Reader reader = entry.getValue().openAsReader()) {
            this.codec.parse(this.ops, StrictJsonParser.parse(reader)).ifSuccess(parsed -> {
               if (result.putIfAbsent(id, (T)parsed) != null) {
                  throw new IllegalStateException("Duplicate data file ignored with ID " + id);
               }
            }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", new Object[]{id, location, error}));
         } catch (JsonParseException | IllegalArgumentException | IOException e) {
            LOGGER.error("Couldn't parse data file '{}' from '{}'", new Object[]{id, location, e});
         }
      }

      return result;
   }
}
