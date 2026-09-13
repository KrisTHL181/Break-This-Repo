package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class LevelDataGeneratorOptionsFix extends DataFix {
   private static final Map<String, String> MAP = Util.make(Maps.newHashMap(), map -> {
      map.put("0", "minecraft:ocean");
      map.put("1", "minecraft:plains");
      map.put("2", "minecraft:desert");
      map.put("3", "minecraft:mountains");
      map.put("4", "minecraft:forest");
      map.put("5", "minecraft:taiga");
      map.put("6", "minecraft:swamp");
      map.put("7", "minecraft:river");
      map.put("8", "minecraft:nether");
      map.put("9", "minecraft:the_end");
      map.put("10", "minecraft:frozen_ocean");
      map.put("11", "minecraft:frozen_river");
      map.put("12", "minecraft:snowy_tundra");
      map.put("13", "minecraft:snowy_mountains");
      map.put("14", "minecraft:mushroom_fields");
      map.put("15", "minecraft:mushroom_field_shore");
      map.put("16", "minecraft:beach");
      map.put("17", "minecraft:desert_hills");
      map.put("18", "minecraft:wooded_hills");
      map.put("19", "minecraft:taiga_hills");
      map.put("20", "minecraft:mountain_edge");
      map.put("21", "minecraft:jungle");
      map.put("22", "minecraft:jungle_hills");
      map.put("23", "minecraft:jungle_edge");
      map.put("24", "minecraft:deep_ocean");
      map.put("25", "minecraft:stone_shore");
      map.put("26", "minecraft:snowy_beach");
      map.put("27", "minecraft:birch_forest");
      map.put("28", "minecraft:birch_forest_hills");
      map.put("29", "minecraft:dark_forest");
      map.put("30", "minecraft:snowy_taiga");
      map.put("31", "minecraft:snowy_taiga_hills");
      map.put("32", "minecraft:giant_tree_taiga");
      map.put("33", "minecraft:giant_tree_taiga_hills");
      map.put("34", "minecraft:wooded_mountains");
      map.put("35", "minecraft:savanna");
      map.put("36", "minecraft:savanna_plateau");
      map.put("37", "minecraft:badlands");
      map.put("38", "minecraft:wooded_badlands_plateau");
      map.put("39", "minecraft:badlands_plateau");
      map.put("40", "minecraft:small_end_islands");
      map.put("41", "minecraft:end_midlands");
      map.put("42", "minecraft:end_highlands");
      map.put("43", "minecraft:end_barrens");
      map.put("44", "minecraft:warm_ocean");
      map.put("45", "minecraft:lukewarm_ocean");
      map.put("46", "minecraft:cold_ocean");
      map.put("47", "minecraft:deep_warm_ocean");
      map.put("48", "minecraft:deep_lukewarm_ocean");
      map.put("49", "minecraft:deep_cold_ocean");
      map.put("50", "minecraft:deep_frozen_ocean");
      map.put("127", "minecraft:the_void");
      map.put("129", "minecraft:sunflower_plains");
      map.put("130", "minecraft:desert_lakes");
      map.put("131", "minecraft:gravelly_mountains");
      map.put("132", "minecraft:flower_forest");
      map.put("133", "minecraft:taiga_mountains");
      map.put("134", "minecraft:swamp_hills");
      map.put("140", "minecraft:ice_spikes");
      map.put("149", "minecraft:modified_jungle");
      map.put("151", "minecraft:modified_jungle_edge");
      map.put("155", "minecraft:tall_birch_forest");
      map.put("156", "minecraft:tall_birch_hills");
      map.put("157", "minecraft:dark_forest_hills");
      map.put("158", "minecraft:snowy_taiga_mountains");
      map.put("160", "minecraft:giant_spruce_taiga");
      map.put("161", "minecraft:giant_spruce_taiga_hills");
      map.put("162", "minecraft:modified_gravelly_mountains");
      map.put("163", "minecraft:shattered_savanna");
      map.put("164", "minecraft:shattered_savanna_plateau");
      map.put("165", "minecraft:eroded_badlands");
      map.put("166", "minecraft:modified_wooded_badlands_plateau");
      map.put("167", "minecraft:modified_badlands_plateau");
   });
   public static final String GENERATOR_OPTIONS = "generatorOptions";

   public LevelDataGeneratorOptionsFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> resultType = this.getOutputSchema().getType(References.LEVEL);
      return this.fixTypeEverywhereTyped(
         "LevelDataGeneratorOptionsFix",
         this.getInputSchema().getType(References.LEVEL),
         resultType,
         input -> Util.writeAndReadTypedOrThrow(input, resultType, tag -> {
            Optional<String> generatorOptions = tag.get("generatorOptions").asString().result();
            if ("flat".equalsIgnoreCase(tag.get("generatorName").asString(""))) {
               String flatOptionString = generatorOptions.orElse("");
               return tag.set("generatorOptions", convert(flatOptionString, tag.getOps()));
            } else if ("buffet".equalsIgnoreCase(tag.get("generatorName").asString("")) && generatorOptions.isPresent()) {
               JsonElement legacyOptions = LenientJsonParser.parse(generatorOptions.get());
               return tag.set("generatorOptions", new Dynamic(JsonOps.INSTANCE, legacyOptions).convert(tag.getOps()));
            } else {
               return tag;
            }
         })
      );
   }

   private static <T> Dynamic<T> convert(final String flatOptionString, final DynamicOps<T> ops) {
      Iterator<String> parts = Splitter.on(';').split(flatOptionString).iterator();
      String biome = "minecraft:plains";
      Map<String, Map<String, String>> structuresOptions = Maps.newHashMap();
      List<Pair<Integer, String>> layerList;
      if (!flatOptionString.isEmpty() && parts.hasNext()) {
         layerList = getLayersInfoFromString(parts.next());
         if (!layerList.isEmpty()) {
            if (parts.hasNext()) {
               biome = MAP.getOrDefault(parts.next(), "minecraft:plains");
            }

            if (parts.hasNext()) {
               String[] structures1 = parts.next().toLowerCase(Locale.ROOT).split(",");

               for (String structure : structures1) {
                  String[] separated = structure.split("\\(", 2);
                  if (!separated[0].isEmpty()) {
                     structuresOptions.put(separated[0], Maps.newHashMap());
                     if (separated.length > 1 && separated[1].endsWith(")") && separated[1].length() > 1) {
                        String[] options = separated[1].substring(0, separated[1].length() - 1).split(" ");

                        for (String part : options) {
                           String[] split = part.split("=", 2);
                           if (split.length == 2) {
                              structuresOptions.get(separated[0]).put(split[0], split[1]);
                           }
                        }
                     }
                  }
               }
            } else {
               structuresOptions.put("village", Maps.newHashMap());
            }
         }
      } else {
         layerList = Lists.newArrayList();
         layerList.add(Pair.of(1, "minecraft:bedrock"));
         layerList.add(Pair.of(2, "minecraft:dirt"));
         layerList.add(Pair.of(1, "minecraft:grass_block"));
         structuresOptions.put("village", Maps.newHashMap());
      }

      T layers = (T)ops.createList(
         layerList.stream()
            .map(
               layer -> ops.createMap(
                  ImmutableMap.of(
                     ops.createString("height"),
                     ops.createInt((Integer)layer.getFirst()),
                     ops.createString("block"),
                     ops.createString((String)layer.getSecond())
                  )
               )
            )
      );
      T structures = (T)ops.createMap(
         structuresOptions.entrySet()
            .stream()
            .map(
               entry -> Pair.of(
                  ops.createString(entry.getKey().toLowerCase(Locale.ROOT)),
                  ops.createMap(
                     entry.getValue()
                        .entrySet()
                        .stream()
                        .map(option -> Pair.of(ops.createString(option.getKey()), ops.createString(option.getValue())))
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))
                  )
               )
            )
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))
      );
      return new Dynamic(
         ops,
         ops.createMap(
            ImmutableMap.of(ops.createString("layers"), layers, ops.createString("biome"), ops.createString(biome), ops.createString("structures"), structures)
         )
      );
   }

   private static @Nullable Pair<Integer, String> getLayerInfoFromString(final String input) {
      String[] parts = input.split("\\*", 2);
      int height;
      if (parts.length == 2) {
         try {
            height = Integer.parseInt(parts[0]);
         } catch (NumberFormatException ignored) {
            return null;
         }
      } else {
         height = 1;
      }

      String block = parts[parts.length - 1];
      return Pair.of(height, block);
   }

   private static List<Pair<Integer, String>> getLayersInfoFromString(final String input) {
      List<Pair<Integer, String>> result = Lists.newArrayList();
      String[] depths = input.split(",");

      for (String depth : depths) {
         Pair<Integer, String> layer = getLayerInfoFromString(depth);
         if (layer == null) {
            return Collections.emptyList();
         }

         result.add(layer);
      }

      return result;
   }
}
