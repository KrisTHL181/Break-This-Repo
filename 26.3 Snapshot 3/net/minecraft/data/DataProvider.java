package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public interface DataProvider {
   ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap(), m -> {
      m.put("type", 0);
      m.put("parent", 1);
      m.defaultReturnValue(2);
   });
   Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(e -> (String)e);
   Logger LOGGER = LogUtils.getLogger();

   CompletableFuture<?> run(CachedOutput cache);

   String getName();

   static <T> CompletableFuture<?> saveAll(
      final CachedOutput cache, final Codec<T> codec, final PackOutput.PathProvider pathProvider, final Map<Identifier, T> entries
   ) {
      return saveAll(cache, codec, pathProvider::json, entries);
   }

   static <T, E> CompletableFuture<?> saveAll(final CachedOutput cache, final Codec<E> codec, final Function<T, Path> pathGetter, final Map<T, E> contents) {
      return saveAll(cache, e -> (JsonElement)codec.encodeStart(JsonOps.INSTANCE, e).getOrThrow(), pathGetter, contents);
   }

   static <T, E> CompletableFuture<?> saveAll(
      final CachedOutput cache, final Function<E, JsonElement> serializer, final Function<T, Path> pathGetter, final Map<T, E> contents
   ) {
      return CompletableFuture.allOf(contents.entrySet().stream().map(entry -> {
         Path path = pathGetter.apply(entry.getKey());
         JsonElement json = serializer.apply(entry.getValue());
         return saveStable(cache, json, path);
      }).toArray(CompletableFuture[]::new));
   }

   static <T> CompletableFuture<?> saveStable(
      final CachedOutput cache, final HolderLookup.Provider registries, final Codec<T> codec, final T value, final Path path
   ) {
      RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
      return saveStable(cache, ops, codec, value, path);
   }

   static <T> CompletableFuture<?> saveStable(final CachedOutput cache, final Codec<T> codec, final T value, final Path path) {
      return saveStable(cache, JsonOps.INSTANCE, codec, value, path);
   }

   private static <T> CompletableFuture<?> saveStable(
      final CachedOutput cache, final DynamicOps<JsonElement> ops, final Codec<T> codec, final T value, final Path path
   ) {
      JsonElement json = (JsonElement)codec.encodeStart(ops, value).getOrThrow();
      return saveStable(cache, json, path);
   }

   static CompletableFuture<?> saveStable(final CachedOutput cache, final JsonElement root, final Path path) {
      return CompletableFuture.runAsync(() -> {
         try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), bytes);
            JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(hashedBytes, StandardCharsets.UTF_8));

            try {
               jsonWriter.setSerializeNulls(false);
               jsonWriter.setIndent("  ");
               GsonHelper.writeValue(jsonWriter, root, KEY_COMPARATOR);
            } catch (Throwable t$) {
               try {
                  jsonWriter.close();
               } catch (Throwable x2) {
                  t$.addSuppressed(x2);
               }

               throw t$;
            }

            jsonWriter.close();
            cache.writeIfNeeded(path, bytes.toByteArray(), hashedBytes.hash());
         } catch (IOException e) {
            LOGGER.error("Failed to save file to {}", path, e);
         }
      }, Util.backgroundExecutor().forName("saveStable"));
   }

   @FunctionalInterface
   interface Factory<T extends DataProvider> {
      T create(PackOutput output);
   }
}
