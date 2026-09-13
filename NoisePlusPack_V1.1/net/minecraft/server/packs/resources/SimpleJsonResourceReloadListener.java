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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DynamicOps<JsonElement> ops;
   private final Codec<T> codec;
   private final FileToIdConverter lister;

   protected SimpleJsonResourceReloadListener(HolderLookup.Provider p_378826_, Codec<T> p_361980_, ResourceKey<? extends Registry<T>> p_376437_) {
      this(p_378826_.createSerializationContext(JsonOps.INSTANCE), p_361980_, FileToIdConverter.registry(p_376437_));
   }

   protected SimpleJsonResourceReloadListener(Codec<T> p_370137_, FileToIdConverter p_375758_) {
      this(JsonOps.INSTANCE, p_370137_, p_375758_);
   }

   private SimpleJsonResourceReloadListener(DynamicOps<JsonElement> p_376631_, Codec<T> p_362926_, FileToIdConverter p_376605_) {
      this.ops = p_376631_;
      this.codec = p_362926_;
      this.lister = p_376605_;
   }

   protected Map<Identifier, T> prepare(ResourceManager p_10771_, ProfilerFiller p_10772_) {
      Map<Identifier, T> map = new HashMap<>();
      scanDirectory(p_10771_, this.lister, this.ops, this.codec, map);
      return map;
   }

   public static <T> void scanDirectory(
      ResourceManager p_279308_,
      ResourceKey<? extends Registry<T>> p_377536_,
      DynamicOps<JsonElement> p_369854_,
      Codec<T> p_368755_,
      Map<Identifier, T> p_279404_
   ) {
      scanDirectory(p_279308_, FileToIdConverter.registry(p_377536_), p_369854_, p_368755_, p_279404_);
   }

   public static <T> void scanDirectory(
      ResourceManager p_376562_, FileToIdConverter p_377980_, DynamicOps<JsonElement> p_378080_, Codec<T> p_376362_, Map<Identifier, T> p_377922_
   ) {
      for (Entry<Identifier, Resource> entry : p_377980_.listMatchingResources(p_376562_).entrySet()) {
         Identifier identifier = entry.getKey();
         Identifier identifier1 = p_377980_.fileToId(identifier);

         try (Reader reader = entry.getValue().openAsReader()) {
            p_376362_.parse(p_378080_, StrictJsonParser.parse(reader)).ifSuccess(p_370131_ -> {
               if (p_377922_.putIfAbsent(identifier1, (T)p_370131_) != null) {
                  throw new IllegalStateException("Duplicate data file ignored with ID " + identifier1);
               }
            }).ifError(p_362245_ -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", new Object[]{identifier1, identifier, p_362245_}));
         } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
            LOGGER.error("Couldn't parse data file '{}' from '{}'", new Object[]{identifier1, identifier, jsonparseexception});
         }
      }
   }
}
