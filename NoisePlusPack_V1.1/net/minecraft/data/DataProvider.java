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
   ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap(), p_236070_ -> {
      p_236070_.put("type", 0);
      p_236070_.put("parent", 1);
      p_236070_.defaultReturnValue(2);
   });
   Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(p_236077_ -> (String)p_236077_);
   Logger LOGGER = LogUtils.getLogger();

   CompletableFuture<?> run(CachedOutput var1);

   String getName();

   static <T> CompletableFuture<?> saveAll(CachedOutput p_369703_, Codec<T> p_369384_, PackOutput.PathProvider p_366772_, Map<Identifier, T> p_364004_) {
      return saveAll(p_369703_, p_369384_, p_366772_::json, p_364004_);
   }

   static <T, E> CompletableFuture<?> saveAll(CachedOutput p_378522_, Codec<E> p_375923_, Function<T, Path> p_378335_, Map<T, E> p_378562_) {
      return saveAll(p_378522_, p_374749_ -> (JsonElement)p_375923_.encodeStart(JsonOps.INSTANCE, p_374749_).getOrThrow(), p_378335_, p_378562_);
   }

   static <T, E> CompletableFuture<?> saveAll(CachedOutput p_376020_, Function<E, JsonElement> p_377735_, Function<T, Path> p_375746_, Map<T, E> p_378110_) {
      return CompletableFuture.allOf(p_378110_.entrySet().stream().map(p_374753_ -> {
         Path path = p_375746_.apply(p_374753_.getKey());
         JsonElement jsonelement = p_377735_.apply(p_374753_.getValue());
         return saveStable(p_376020_, jsonelement, path);
      }).toArray(CompletableFuture[]::new));
   }

   static <T> CompletableFuture<?> saveStable(CachedOutput p_300299_, HolderLookup.Provider p_330662_, Codec<T> p_297797_, T p_300766_, Path p_299101_) {
      RegistryOps<JsonElement> registryops = p_330662_.createSerializationContext(JsonOps.INSTANCE);
      return saveStable(p_300299_, registryops, p_297797_, p_300766_, p_299101_);
   }

   static <T> CompletableFuture<?> saveStable(CachedOutput p_364924_, Codec<T> p_367616_, T p_369344_, Path p_369481_) {
      return saveStable(p_364924_, JsonOps.INSTANCE, p_367616_, p_369344_, p_369481_);
   }

   private static <T> CompletableFuture<?> saveStable(
      CachedOutput p_366662_, DynamicOps<JsonElement> p_369056_, Codec<T> p_365700_, T p_360791_, Path p_368062_
   ) {
      JsonElement jsonelement = (JsonElement)p_365700_.encodeStart(p_369056_, p_360791_).getOrThrow();
      return saveStable(p_366662_, jsonelement, p_368062_);
   }

   static CompletableFuture<?> saveStable(CachedOutput p_253653_, JsonElement p_254542_, Path p_254467_) {
      return CompletableFuture.runAsync(() -> {
         try {
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
            HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
            JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(hashingoutputstream, StandardCharsets.UTF_8));

            try {
               jsonwriter.setSerializeNulls(false);
               jsonwriter.setIndent("  ");
               GsonHelper.writeValue(jsonwriter, p_254542_, KEY_COMPARATOR);
            } catch (Throwable var9) {
               try {
                  jsonwriter.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            jsonwriter.close();
            p_253653_.writeIfNeeded(p_254467_, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
         } catch (IOException ioexception) {
            LOGGER.error("Failed to save file to {}", p_254467_, ioexception);
         }
      }, Util.backgroundExecutor().forName("saveStable"));
   }

   @FunctionalInterface
   interface Factory<T extends DataProvider> {
      T create(PackOutput var1);
   }
}
