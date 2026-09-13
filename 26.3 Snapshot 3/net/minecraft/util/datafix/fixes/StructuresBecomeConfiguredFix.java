package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.LongStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructuresBecomeConfiguredFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, StructuresBecomeConfiguredFix.Conversion> CONVERSION_MAP = ImmutableMap.builder()
      .put(
         "mineshaft",
         StructuresBecomeConfiguredFix.Conversion.biomeMapped(
            Map.of(List.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"), "minecraft:mineshaft_mesa"), "minecraft:mineshaft"
         )
      )
      .put(
         "shipwreck",
         StructuresBecomeConfiguredFix.Conversion.biomeMapped(
            Map.of(List.of("minecraft:beach", "minecraft:snowy_beach"), "minecraft:shipwreck_beached"), "minecraft:shipwreck"
         )
      )
      .put(
         "ocean_ruin",
         StructuresBecomeConfiguredFix.Conversion.biomeMapped(
            Map.of(List.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"), "minecraft:ocean_ruin_warm"),
            "minecraft:ocean_ruin_cold"
         )
      )
      .put(
         "village",
         StructuresBecomeConfiguredFix.Conversion.biomeMapped(
            Map.of(
               List.of("minecraft:desert"),
               "minecraft:village_desert",
               List.of("minecraft:savanna"),
               "minecraft:village_savanna",
               List.of("minecraft:snowy_plains"),
               "minecraft:village_snowy",
               List.of("minecraft:taiga"),
               "minecraft:village_taiga"
            ),
            "minecraft:village_plains"
         )
      )
      .put(
         "ruined_portal",
         StructuresBecomeConfiguredFix.Conversion.biomeMapped(
            Map.of(
               List.of("minecraft:desert"),
               "minecraft:ruined_portal_desert",
               List.of(
                  "minecraft:badlands",
                  "minecraft:eroded_badlands",
                  "minecraft:wooded_badlands",
                  "minecraft:windswept_hills",
                  "minecraft:windswept_forest",
                  "minecraft:windswept_gravelly_hills",
                  "minecraft:savanna_plateau",
                  "minecraft:windswept_savanna",
                  "minecraft:stony_shore",
                  "minecraft:meadow",
                  "minecraft:frozen_peaks",
                  "minecraft:jagged_peaks",
                  "minecraft:stony_peaks",
                  "minecraft:snowy_slopes"
               ),
               "minecraft:ruined_portal_mountain",
               List.of("minecraft:bamboo_jungle", "minecraft:jungle", "minecraft:sparse_jungle"),
               "minecraft:ruined_portal_jungle",
               List.of(
                  "minecraft:deep_frozen_ocean",
                  "minecraft:deep_cold_ocean",
                  "minecraft:deep_ocean",
                  "minecraft:deep_lukewarm_ocean",
                  "minecraft:frozen_ocean",
                  "minecraft:ocean",
                  "minecraft:cold_ocean",
                  "minecraft:lukewarm_ocean",
                  "minecraft:warm_ocean"
               ),
               "minecraft:ruined_portal_ocean"
            ),
            "minecraft:ruined_portal"
         )
      )
      .put("pillager_outpost", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:pillager_outpost"))
      .put("mansion", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:mansion"))
      .put("jungle_pyramid", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:jungle_pyramid"))
      .put("desert_pyramid", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:desert_pyramid"))
      .put("igloo", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:igloo"))
      .put("swamp_hut", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:swamp_hut"))
      .put("stronghold", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:stronghold"))
      .put("monument", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:monument"))
      .put("fortress", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:fortress"))
      .put("endcity", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:end_city"))
      .put("buried_treasure", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:buried_treasure"))
      .put("nether_fossil", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:nether_fossil"))
      .put("bastion_remnant", StructuresBecomeConfiguredFix.Conversion.trivial("minecraft:bastion_remnant"))
      .build();

   public StructuresBecomeConfiguredFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      Type<?> newChunkType = this.getInputSchema().getType(References.CHUNK);
      return this.writeFixAndRead("StucturesToConfiguredStructures", chunkType, newChunkType, this::fix);
   }

   private Dynamic<?> fix(final Dynamic<?> chunk) {
      return chunk.update(
         "structures", structures -> structures.update("starts", s -> this.updateStarts(s, chunk)).update("References", r -> this.updateReferences(r, chunk))
      );
   }

   private Dynamic<?> updateStarts(final Dynamic<?> starts, final Dynamic<?> chunk) {
      Map<? extends Dynamic<?>, ? extends Dynamic<?>> values = starts.getMapValues().result().orElse(Map.of());
      HashMap<Dynamic<?>, Dynamic<?>> newMap = Maps.newHashMap();
      values.forEach((key, start) -> {
         if (!start.get("id").asString("INVALID").equals("INVALID")) {
            Dynamic<?> newKey = this.findUpdatedStructureType((Dynamic<?>)key, chunk);
            if (newKey == null) {
               LOGGER.warn("Encountered unknown structure in datafixer: {}", key.asString("<missing key>"));
            } else {
               newMap.computeIfAbsent(newKey, k -> start.set("id", newKey));
            }
         }
      });
      return chunk.createMap(newMap);
   }

   private Dynamic<?> updateReferences(final Dynamic<?> references, final Dynamic<?> chunk) {
      Map<? extends Dynamic<?>, ? extends Dynamic<?>> values = references.getMapValues().result().orElse(Map.of());
      HashMap<Dynamic<?>, Dynamic<?>> newMap = Maps.newHashMap();
      values.forEach(
         (key, refList) -> {
            if (refList.asLongStream().count() != 0L) {
               Dynamic<?> newKey = this.findUpdatedStructureType((Dynamic<?>)key, chunk);
               if (newKey == null) {
                  LOGGER.warn("Encountered unknown structure in datafixer: {}", key.asString("<missing key>"));
               } else {
                  newMap.compute(
                     newKey,
                     (k, oldRefList) -> oldRefList == null
                        ? refList
                        : refList.createLongList(LongStream.concat(oldRefList.asLongStream(), refList.asLongStream()))
                  );
               }
            }
         }
      );
      return chunk.createMap(newMap);
   }

   private @Nullable Dynamic<?> findUpdatedStructureType(final Dynamic<?> dynamicKey, final Dynamic<?> chunk) {
      String key = dynamicKey.asString("UNKNOWN").toLowerCase(Locale.ROOT);
      StructuresBecomeConfiguredFix.Conversion conversion = CONVERSION_MAP.get(key);
      if (conversion == null) {
         return null;
      }

      String resultingId = conversion.fallback;
      if (!conversion.biomeMapping().isEmpty()) {
         Optional<String> result = this.guessConfiguration(chunk, conversion);
         if (result.isPresent()) {
            resultingId = result.get();
         }
      }

      return chunk.createString(resultingId);
   }

   private Optional<String> guessConfiguration(final Dynamic<?> chunk, final StructuresBecomeConfiguredFix.Conversion conversion) {
      Object2IntArrayMap<String> matches = new Object2IntArrayMap();
      chunk.get("sections").asList(Function.identity()).forEach(s -> s.get("biomes").get("palette").asList(Function.identity()).forEach(biome -> {
         String mapping = conversion.biomeMapping().get(biome.asString(""));
         if (mapping != null) {
            matches.mergeInt(mapping, 1, Integer::sum);
         }
      }));
      return matches.object2IntEntrySet()
         .stream()
         .max(Comparator.comparingInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue))
         .map(Entry::getKey);
   }

   private record Conversion(Map<String, String> biomeMapping, String fallback) {
      public static StructuresBecomeConfiguredFix.Conversion trivial(final String result) {
         return new StructuresBecomeConfiguredFix.Conversion(Map.of(), result);
      }

      public static StructuresBecomeConfiguredFix.Conversion biomeMapped(final Map<List<String>, String> mapping, final String fallback) {
         return new StructuresBecomeConfiguredFix.Conversion(unpack(mapping), fallback);
      }

      private static Map<String, String> unpack(final Map<List<String>, String> packed) {
         Builder<String, String> builder = ImmutableMap.builder();

         for (Entry<List<String>, String> entry : packed.entrySet()) {
            entry.getKey().forEach(k -> builder.put(k, entry.getValue()));
         }

         return builder.build();
      }
   }
}
