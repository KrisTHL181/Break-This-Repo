package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenSettingsFix extends DataFix {
   private static final String VILLAGE = "minecraft:village";
   private static final String DESERT_PYRAMID = "minecraft:desert_pyramid";
   private static final String IGLOO = "minecraft:igloo";
   private static final String JUNGLE_TEMPLE = "minecraft:jungle_pyramid";
   private static final String SWAMP_HUT = "minecraft:swamp_hut";
   private static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
   private static final String END_CITY = "minecraft:endcity";
   private static final String WOODLAND_MANSION = "minecraft:mansion";
   private static final String OCEAN_MONUMENT = "minecraft:monument";
   private static final ImmutableMap<String, WorldGenSettingsFix.StructureFeatureConfiguration> DEFAULTS = ImmutableMap.builder()
      .put("minecraft:village", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 10387312))
      .put("minecraft:desert_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357617))
      .put("minecraft:igloo", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357618))
      .put("minecraft:jungle_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357619))
      .put("minecraft:swamp_hut", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357620))
      .put("minecraft:pillager_outpost", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 165745296))
      .put("minecraft:monument", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 5, 10387313))
      .put("minecraft:endcity", new WorldGenSettingsFix.StructureFeatureConfiguration(20, 11, 10387313))
      .put("minecraft:mansion", new WorldGenSettingsFix.StructureFeatureConfiguration(80, 20, 10387319))
      .build();

   public WorldGenSettingsFix(final Schema parent) {
      super(parent, true);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "WorldGenSettings building",
         this.getInputSchema().getType(References.WORLD_GEN_SETTINGS),
         settings -> settings.update(DSL.remainderFinder(), WorldGenSettingsFix::fix)
      );
   }

   private static <T> Dynamic<T> noise(final long seed, final DynamicLike<T> input, final Dynamic<T> noiseGeneratorSettings, final Dynamic<T> biomeSource) {
      return input.createMap(
         ImmutableMap.of(
            input.createString("type"),
            input.createString("minecraft:noise"),
            input.createString("biome_source"),
            biomeSource,
            input.createString("seed"),
            input.createLong(seed),
            input.createString("settings"),
            noiseGeneratorSettings
         )
      );
   }

   private static <T> Dynamic<T> vanillaBiomeSource(final Dynamic<T> input, final long seed, final boolean legacyBiomeInitLayer, final boolean largeBiomes) {
      Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.builder()
         .put(input.createString("type"), input.createString("minecraft:vanilla_layered"))
         .put(input.createString("seed"), input.createLong(seed))
         .put(input.createString("large_biomes"), input.createBoolean(largeBiomes));
      if (legacyBiomeInitLayer) {
         builder.put(input.createString("legacy_biome_init_layer"), input.createBoolean(legacyBiomeInitLayer));
      }

      return input.createMap(builder.build());
   }

   private static <T> Dynamic<T> fix(final Dynamic<T> input) {
      DynamicOps<T> ops = input.getOps();
      long seed = input.get("RandomSeed").asLong(0L);
      Optional<String> name = input.get("generatorName").asString().map(n -> n.toLowerCase(Locale.ROOT)).result();
      Optional<String> legacyCustomOptions = input.get("legacy_custom_options")
         .asString()
         .result()
         .map(Optional::of)
         .orElseGet(() -> name.equals(Optional.of("customized")) ? input.get("generatorOptions").asString().result() : Optional.empty());
      boolean caves = false;
      Dynamic<T> generator;
      if (name.equals(Optional.of("customized"))) {
         generator = defaultOverworld(input, seed);
      } else if (name.isEmpty()) {
         generator = defaultOverworld(input, seed);
      } else {
         switch ((String)name.get()) {
            case "flat":
               OptionalDynamic<T> flatSettings = input.get("generatorOptions");
               Map<Dynamic<T>, Dynamic<T>> structureBuilder = fixFlatStructures(ops, flatSettings);
               generator = input.createMap(
                  ImmutableMap.of(
                     input.createString("type"),
                     input.createString("minecraft:flat"),
                     input.createString("settings"),
                     input.createMap(
                        ImmutableMap.of(
                           input.createString("structures"),
                           input.createMap(structureBuilder),
                           input.createString("layers"),
                           flatSettings.get("layers")
                              .result()
                              .orElseGet(
                                 () -> input.createList(
                                    Stream.of(
                                       input.createMap(
                                          ImmutableMap.of(
                                             input.createString("height"),
                                             input.createInt(1),
                                             input.createString("block"),
                                             input.createString("minecraft:bedrock")
                                          )
                                       ),
                                       input.createMap(
                                          ImmutableMap.of(
                                             input.createString("height"),
                                             input.createInt(2),
                                             input.createString("block"),
                                             input.createString("minecraft:dirt")
                                          )
                                       ),
                                       input.createMap(
                                          ImmutableMap.of(
                                             input.createString("height"),
                                             input.createInt(1),
                                             input.createString("block"),
                                             input.createString("minecraft:grass_block")
                                          )
                                       )
                                    )
                                 )
                              ),
                           input.createString("biome"),
                           input.createString(flatSettings.get("biome").asString("minecraft:plains"))
                        )
                     )
                  )
               );
               break;
            case "debug_all_block_states":
               generator = input.createMap(ImmutableMap.of(input.createString("type"), input.createString("minecraft:debug")));
               break;
            case "buffet":
               OptionalDynamic<T> settings = input.get("generatorOptions");
               OptionalDynamic<?> chunkGeneratorObject = settings.get("chunk_generator");
               Optional<String> type = chunkGeneratorObject.get("type").asString().result();
               Dynamic<T> noiseGeneratorSettings;
               if (Objects.equals(type, Optional.of("minecraft:caves"))) {
                  noiseGeneratorSettings = input.createString("minecraft:caves");
                  caves = true;
               } else if (Objects.equals(type, Optional.of("minecraft:floating_islands"))) {
                  noiseGeneratorSettings = input.createString("minecraft:floating_islands");
               } else {
                  noiseGeneratorSettings = input.createString("minecraft:overworld");
               }

               Dynamic<T> biomeSource = settings.get("biome_source")
                  .result()
                  .orElseGet(() -> input.createMap(ImmutableMap.of(input.createString("type"), input.createString("minecraft:fixed"))));
               Dynamic<T> fixedSource;
               if (biomeSource.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
                  String biome = biomeSource.get("options").get("biomes").asStream().findFirst().flatMap(b -> b.asString().result()).orElse("minecraft:ocean");
                  fixedSource = biomeSource.remove("options").set("biome", input.createString(biome));
               } else {
                  fixedSource = biomeSource;
               }

               generator = noise(seed, input, noiseGeneratorSettings, fixedSource);
               break;
            default:
               boolean normal = name.get().equals("default");
               boolean legacyBiomeInitLayer = name.get().equals("default_1_1") || normal && input.get("generatorVersion").asInt(0) == 0;
               boolean isAmplified = name.get().equals("amplified");
               boolean largeBiomes = name.get().equals("largebiomes");
               generator = noise(
                  seed,
                  input,
                  input.createString(isAmplified ? "minecraft:amplified" : "minecraft:overworld"),
                  vanillaBiomeSource(input, seed, legacyBiomeInitLayer, largeBiomes)
               );
         }
      }

      boolean generateMapFeatures = input.get("MapFeatures").asBoolean(true);
      boolean generateBonusChest = input.get("BonusChest").asBoolean(false);
      Builder<T, T> builder = ImmutableMap.builder();
      builder.put(ops.createString("seed"), ops.createLong(seed));
      builder.put(ops.createString("generate_features"), ops.createBoolean(generateMapFeatures));
      builder.put(ops.createString("bonus_chest"), ops.createBoolean(generateBonusChest));
      builder.put(ops.createString("dimensions"), vanillaLevels(input, seed, generator, caves));
      legacyCustomOptions.ifPresent(o -> builder.put(ops.createString("legacy_custom_options"), ops.createString(o)));
      return new Dynamic(ops, ops.createMap(builder.build()));
   }

   protected static <T> Dynamic<T> defaultOverworld(final Dynamic<T> input, final long seed) {
      return noise(seed, input, input.createString("minecraft:overworld"), vanillaBiomeSource(input, seed, false, false));
   }

   protected static <T> T vanillaLevels(final Dynamic<T> input, final long seed, final Dynamic<T> overworldGenerator, final boolean caves) {
      DynamicOps<T> ops = input.getOps();
      return (T)ops.createMap(
         ImmutableMap.of(
            ops.createString("minecraft:overworld"),
            ops.createMap(
               ImmutableMap.of(
                  ops.createString("type"),
                  ops.createString("minecraft:overworld" + (caves ? "_caves" : "")),
                  ops.createString("generator"),
                  overworldGenerator.getValue()
               )
            ),
            ops.createString("minecraft:the_nether"),
            ops.createMap(
               ImmutableMap.of(
                  ops.createString("type"),
                  ops.createString("minecraft:the_nether"),
                  ops.createString("generator"),
                  noise(
                        seed,
                        input,
                        input.createString("minecraft:nether"),
                        input.createMap(
                           ImmutableMap.of(
                              input.createString("type"),
                              input.createString("minecraft:multi_noise"),
                              input.createString("seed"),
                              input.createLong(seed),
                              input.createString("preset"),
                              input.createString("minecraft:nether")
                           )
                        )
                     )
                     .getValue()
               )
            ),
            ops.createString("minecraft:the_end"),
            ops.createMap(
               ImmutableMap.of(
                  ops.createString("type"),
                  ops.createString("minecraft:the_end"),
                  ops.createString("generator"),
                  noise(
                        seed,
                        input,
                        input.createString("minecraft:end"),
                        input.createMap(
                           ImmutableMap.of(
                              input.createString("type"), input.createString("minecraft:the_end"), input.createString("seed"), input.createLong(seed)
                           )
                        )
                     )
                     .getValue()
               )
            )
         )
      );
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> fixFlatStructures(final DynamicOps<T> ops, final OptionalDynamic<T> settings) {
      MutableInt strongholdDistance = new MutableInt(32);
      MutableInt strongholdSpread = new MutableInt(3);
      MutableInt strongholdCount = new MutableInt(128);
      MutableBoolean hasStronghold = new MutableBoolean(false);
      Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> structureConfig = Maps.newHashMap();
      if (settings.result().isEmpty()) {
         hasStronghold.setTrue();
         structureConfig.put("minecraft:village", (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get("minecraft:village"));
      }

      settings.get("structures")
         .flatMap(Dynamic::getMapValues)
         .ifSuccess(
            map -> map.forEach(
               (structureKey, value1) -> value1.getMapValues()
                  .result()
                  .ifPresent(
                     m -> m.forEach(
                        (optionKey, optionValue) -> {
                           String structureName = structureKey.asString("");
                           String optionName = optionKey.asString("");
                           String value = optionValue.asString("");
                           if ("stronghold".equals(structureName)) {
                              hasStronghold.setTrue();
                              switch (optionName) {
                                 case "distance":
                                    strongholdDistance.setValue(getInt(value, strongholdDistance.intValue(), 1));
                                    return;
                                 case "spread":
                                    strongholdSpread.setValue(getInt(value, strongholdSpread.intValue(), 1));
                                    return;
                                 case "count":
                                    strongholdCount.setValue(getInt(value, strongholdCount.intValue(), 1));
                                    return;
                              }
                           } else {
                              switch (optionName) {
                                 case "distance":
                                    switch (structureName) {
                                       case "village":
                                          setSpacing(structureConfig, "minecraft:village", value, 9);
                                          return;
                                       case "biome_1":
                                          setSpacing(structureConfig, "minecraft:desert_pyramid", value, 9);
                                          setSpacing(structureConfig, "minecraft:igloo", value, 9);
                                          setSpacing(structureConfig, "minecraft:jungle_pyramid", value, 9);
                                          setSpacing(structureConfig, "minecraft:swamp_hut", value, 9);
                                          setSpacing(structureConfig, "minecraft:pillager_outpost", value, 9);
                                          return;
                                       case "endcity":
                                          setSpacing(structureConfig, "minecraft:endcity", value, 1);
                                          return;
                                       case "mansion":
                                          setSpacing(structureConfig, "minecraft:mansion", value, 1);
                                          return;
                                       default:
                                          return;
                                    }
                                 case "separation":
                                    if ("oceanmonument".equals(structureName)) {
                                       WorldGenSettingsFix.StructureFeatureConfiguration config = structureConfig.getOrDefault(
                                          "minecraft:monument", (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get("minecraft:monument")
                                       );
                                       int spacing = getInt(value, config.separation, 1);
                                       structureConfig.put(
                                          "minecraft:monument", new WorldGenSettingsFix.StructureFeatureConfiguration(spacing, config.separation, config.salt)
                                       );
                                    }

                                    return;
                                 case "spacing":
                                    if ("oceanmonument".equals(structureName)) {
                                       setSpacing(structureConfig, "minecraft:monument", value, 1);
                                    }

                                    return;
                              }
                           }
                        }
                     )
                  )
            )
         );
      Builder<Dynamic<T>, Dynamic<T>> structureBuilder = ImmutableMap.builder();
      structureBuilder.put(
         settings.createString("structures"),
         settings.createMap(
            structureConfig.entrySet().stream().collect(Collectors.toMap(e -> settings.createString(e.getKey()), e -> e.getValue().serialize(ops)))
         )
      );
      if (hasStronghold.isTrue()) {
         structureBuilder.put(
            settings.createString("stronghold"),
            settings.createMap(
               ImmutableMap.of(
                  settings.createString("distance"),
                  settings.createInt(strongholdDistance.intValue()),
                  settings.createString("spread"),
                  settings.createInt(strongholdSpread.intValue()),
                  settings.createString("count"),
                  settings.createInt(strongholdCount.intValue())
               )
            )
         );
      }

      return structureBuilder.build();
   }

   private static int getInt(final String input, final int def) {
      return NumberUtils.toInt(input, def);
   }

   private static int getInt(final String input, final int def, final int min) {
      return Math.max(min, getInt(input, def));
   }

   private static void setSpacing(
      final Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> structureConfig, final String structure, final String optionValue, final int min
   ) {
      WorldGenSettingsFix.StructureFeatureConfiguration config = structureConfig.getOrDefault(
         structure, (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get(structure)
      );
      int spacing = getInt(optionValue, config.spacing, min);
      structureConfig.put(structure, new WorldGenSettingsFix.StructureFeatureConfiguration(spacing, config.separation, config.salt));
   }

   private static final class StructureFeatureConfiguration {
      public static final Codec<WorldGenSettingsFix.StructureFeatureConfiguration> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.INT.fieldOf("spacing").forGetter(c -> c.spacing),
               Codec.INT.fieldOf("separation").forGetter(c -> c.separation),
               Codec.INT.fieldOf("salt").forGetter(c -> c.salt)
            )
            .apply(i, WorldGenSettingsFix.StructureFeatureConfiguration::new)
      );
      private final int spacing;
      private final int separation;
      private final int salt;

      public StructureFeatureConfiguration(final int spacing, final int separation, final int salt) {
         this.spacing = spacing;
         this.separation = separation;
         this.salt = salt;
      }

      public <T> Dynamic<T> serialize(final DynamicOps<T> ops) {
         return new Dynamic(ops, CODEC.encodeStart(ops, this).result().orElse(ops.emptyMap()));
      }
   }
}
