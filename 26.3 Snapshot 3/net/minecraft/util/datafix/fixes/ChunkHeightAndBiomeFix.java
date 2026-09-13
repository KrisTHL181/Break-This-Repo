package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class ChunkHeightAndBiomeFix extends DataFix {
   public static final String DATAFIXER_CONTEXT_TAG = "__context";
   private static final String NAME = "ChunkHeightAndBiomeFix";
   private static final int OLD_SECTION_COUNT = 16;
   private static final int NEW_SECTION_COUNT = 24;
   private static final int NEW_MIN_SECTION_Y = -4;
   public static final int BLOCKS_PER_SECTION = 4096;
   private static final int LONGS_PER_SECTION = 64;
   private static final int HEIGHTMAP_BITS = 9;
   private static final long HEIGHTMAP_MASK = 511L;
   private static final int HEIGHTMAP_OFFSET = 64;
   private static final String[] HEIGHTMAP_TYPES = new String[]{
      "WORLD_SURFACE_WG", "WORLD_SURFACE", "WORLD_SURFACE_IGNORE_SNOW", "OCEAN_FLOOR_WG", "OCEAN_FLOOR", "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES"
   };
   private static final Set<String> STATUS_IS_OR_AFTER_SURFACE = Set.of(
      "surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full"
   );
   private static final Set<String> STATUS_IS_OR_AFTER_NOISE = Set.of(
      "noise", "surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full"
   );
   private static final Set<String> BLOCKS_BEFORE_FEATURE_STATUS = Set.of(
      "minecraft:air",
      "minecraft:basalt",
      "minecraft:bedrock",
      "minecraft:blackstone",
      "minecraft:calcite",
      "minecraft:cave_air",
      "minecraft:coarse_dirt",
      "minecraft:crimson_nylium",
      "minecraft:dirt",
      "minecraft:end_stone",
      "minecraft:grass_block",
      "minecraft:gravel",
      "minecraft:ice",
      "minecraft:lava",
      "minecraft:mycelium",
      "minecraft:nether_wart_block",
      "minecraft:netherrack",
      "minecraft:orange_terracotta",
      "minecraft:packed_ice",
      "minecraft:podzol",
      "minecraft:powder_snow",
      "minecraft:red_sand",
      "minecraft:red_sandstone",
      "minecraft:sand",
      "minecraft:sandstone",
      "minecraft:snow_block",
      "minecraft:soul_sand",
      "minecraft:soul_soil",
      "minecraft:stone",
      "minecraft:terracotta",
      "minecraft:warped_nylium",
      "minecraft:warped_wart_block",
      "minecraft:water",
      "minecraft:white_terracotta"
   );
   private static final int BIOME_CONTAINER_LAYER_SIZE = 16;
   private static final int BIOME_CONTAINER_SIZE = 64;
   private static final int BIOME_CONTAINER_TOP_LAYER_OFFSET = 1008;
   public static final String DEFAULT_BIOME = "minecraft:plains";
   private static final Int2ObjectMap<String> BIOMES_BY_ID = new Int2ObjectOpenHashMap();

   public ChunkHeightAndBiomeFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> oldChunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> levelFinder = oldChunkType.findField("Level");
      OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
      Schema outputSchema = this.getOutputSchema();
      Type<?> chunkType = outputSchema.getType(References.CHUNK);
      Type<?> levelType = chunkType.findField("Level").type();
      Type<?> sectionsType = levelType.findField("Sections").type();
      return this.fixTypeEverywhereTyped(
         "ChunkHeightAndBiomeFix",
         oldChunkType,
         chunkType,
         chunk -> chunk.updateTyped(
            levelFinder,
            levelType,
            level -> {
               Dynamic<?> tag = (Dynamic<?>)level.get(DSL.remainderFinder());
               OptionalDynamic<?> contextTag = ((Dynamic)chunk.get(DSL.remainderFinder())).get("__context");
               String dimension = contextTag.get("dimension").asString().result().orElse("");
               String generator = contextTag.get("generator").asString().result().orElse("");
               boolean isOverworld = "minecraft:overworld".equals(dimension);
               MutableBoolean wasIncreasedHeight = new MutableBoolean();
               int minSection = isOverworld ? -4 : 0;
               Dynamic<?>[] biomeContainers = getBiomeContainers(tag, isOverworld, minSection, wasIncreasedHeight);
               Dynamic<?> airContainer = makePalettedContainer(
                  tag.createList(Stream.of(tag.createMap(ImmutableMap.of(tag.createString("Name"), tag.createString("minecraft:air")))))
               );
               Set<String> blocksInChunk = Sets.newHashSet();
               MutableObject<Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>> bedrockSectionBlocks = new MutableObject(
                  (Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>)() -> null
               );
               level = level.updateTyped(sectionsFinder, sectionsType, sections -> {
                  IntSet doneSections = new IntOpenHashSet();
                  Dynamic<?> dynamic = (Dynamic<?>)sections.write().result().orElseThrow(() -> new IllegalStateException("Malformed Chunk.Level.Sections"));
                  List<Dynamic<?>> sectionsList = dynamic.asStream().map(sectionx -> {
                     int sectionYx = sectionx.get("Y").asInt(0);
                     Dynamic<?> blockStatesContainer = (Dynamic<?>)DataFixUtils.orElse(sectionx.get("Palette").result().flatMap(palette -> {
                        palette.asStream().map(blockState -> blockState.get("Name").asString("minecraft:air")).forEach(blocksInChunk::add);
                        return sectionx.get("BlockStates").result().map(blockStates -> makeOptimizedPalettedContainer(palette, (Dynamic<?>)blockStates));
                     }), airContainer);
                     Dynamic<?> result = sectionx;
                     int sectionYIndex = sectionYx - minSection;
                     if (sectionYIndex >= 0 && sectionYIndex < biomeContainers.length) {
                        result = result.set("biomes", biomeContainers[sectionYIndex]);
                     }

                     doneSections.add(sectionYx);
                     if (sectionx.get("Y").asInt(Integer.MAX_VALUE) == 0) {
                        bedrockSectionBlocks.setValue((Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>)() -> {
                           List<? extends Dynamic<?>> palette = blockStatesContainer.get("palette").asList(Function.identity());
                           long[] data = blockStatesContainer.get("data").asLongStream().toArray();
                           return new ChunkProtoTickListFix.PoorMansPalettedContainer(palette, data);
                        });
                     }

                     return result.set("block_states", blockStatesContainer).remove("Palette").remove("BlockStates");
                  }).collect(Collectors.toCollection(ArrayList::new));

                  for (int sectionIndex = 0; sectionIndex < biomeContainers.length; sectionIndex++) {
                     int sectionY = sectionIndex + minSection;
                     if (doneSections.add(sectionY)) {
                        Dynamic<?> section = tag.createMap(Map.of(tag.createString("Y"), tag.createInt(sectionY)));
                        section = section.set("block_states", airContainer);
                        section = section.set("biomes", biomeContainers[sectionIndex]);
                        sectionsList.add(section);
                     }
                  }

                  return Util.readTypedOrThrow(sectionsType, tag.createList(sectionsList.stream()));
               });
               return level.update(
                  DSL.remainderFinder(),
                  chunkTag -> {
                     if (isOverworld) {
                        chunkTag = this.predictChunkStatusBeforeSurface(chunkTag, blocksInChunk);
                     }

                     return updateChunkTag(
                        chunkTag,
                        isOverworld,
                        wasIncreasedHeight.booleanValue(),
                        "minecraft:noise".equals(generator),
                        (Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>)bedrockSectionBlocks.get()
                     );
                  }
               );
            }
         )
      );
   }

   private Dynamic<?> predictChunkStatusBeforeSurface(final Dynamic<?> chunkTag, final Set<String> blocksInChunk) {
      return chunkTag.update("Status", statusDynamic -> {
         String status = statusDynamic.asString("empty");
         if (STATUS_IS_OR_AFTER_SURFACE.contains(status)) {
            return statusDynamic;
         } else {
            blocksInChunk.remove("minecraft:air");
            boolean hasNonAirBlocks = !blocksInChunk.isEmpty();
            blocksInChunk.removeAll(BLOCKS_BEFORE_FEATURE_STATUS);
            boolean hasFeatureBlocks = !blocksInChunk.isEmpty();
            if (hasFeatureBlocks) {
               return statusDynamic.createString("liquid_carvers");
            } else if ("noise".equals(status) || hasNonAirBlocks) {
               return statusDynamic.createString("noise");
            } else {
               return "biomes".equals(status) ? statusDynamic.createString("structure_references") : statusDynamic;
            }
         }
      });
   }

   private static Dynamic<?>[] getBiomeContainers(
      final Dynamic<?> tag, final boolean increaseHeight, final int minSection, final MutableBoolean wasIncreasedHeight
   ) {
      Dynamic<?>[] biomeContainers = new Dynamic[increaseHeight ? 24 : 16];
      int[] oldBiomes = tag.get("Biomes").asIntStreamOpt().result().map(IntStream::toArray).orElse(null);
      if (oldBiomes != null && oldBiomes.length == 1536) {
         wasIncreasedHeight.setValue(true);

         for (int sectionYIndex = 0; sectionYIndex < 24; sectionYIndex++) {
            int finalSectionYIndex = sectionYIndex;
            biomeContainers[sectionYIndex] = makeBiomeContainer(tag, ix -> getOldBiome(oldBiomes, finalSectionYIndex * 64 + ix));
         }
      } else if (oldBiomes != null && oldBiomes.length == 1024) {
         for (int sectionY = 0; sectionY < 16; sectionY++) {
            int sectionYIndex = sectionY - minSection;
            int finalSectionY = sectionY;
            biomeContainers[sectionYIndex] = makeBiomeContainer(tag, ix -> getOldBiome(oldBiomes, finalSectionY * 64 + ix));
         }

         if (increaseHeight) {
            Dynamic<?> belowWorldBiomes = makeBiomeContainer(tag, ix -> getOldBiome(oldBiomes, ix % 16));
            Dynamic<?> aboveWorldBiomes = makeBiomeContainer(tag, ix -> getOldBiome(oldBiomes, ix % 16 + 1008));

            for (int i = 0; i < 4; i++) {
               biomeContainers[i] = belowWorldBiomes;
            }

            for (int i = 20; i < 24; i++) {
               biomeContainers[i] = aboveWorldBiomes;
            }
         }
      } else {
         Arrays.fill(biomeContainers, makePalettedContainer(tag.createList(Stream.of(tag.createString("minecraft:plains")))));
      }

      return biomeContainers;
   }

   private static int getOldBiome(final int[] oldBiomes, final int index) {
      return oldBiomes[index] & 0xFF;
   }

   private static Dynamic<?> updateChunkTag(
      Dynamic<?> chunkTag,
      final boolean isOverworld,
      final boolean wasIncreasedHeight,
      final boolean needsBlendingAndUpgrade,
      final Supplier<ChunkProtoTickListFix.@Nullable PoorMansPalettedContainer> bedrockSectionBlocks
   ) {
      chunkTag = chunkTag.remove("Biomes");
      if (!isOverworld) {
         return updateCarvingMasks(chunkTag, 16, 0);
      }

      if (wasIncreasedHeight) {
         return updateCarvingMasks(chunkTag, 24, 0);
      }

      chunkTag = updateHeightmaps(chunkTag);
      chunkTag = addPaddingEntries(chunkTag, "LiquidsToBeTicked");
      chunkTag = addPaddingEntries(chunkTag, "PostProcessing");
      chunkTag = addPaddingEntries(chunkTag, "ToBeTicked");
      chunkTag = updateCarvingMasks(chunkTag, 24, 4);
      chunkTag = chunkTag.update("UpgradeData", ChunkHeightAndBiomeFix::shiftUpgradeData);
      if (!needsBlendingAndUpgrade) {
         return chunkTag;
      }

      Optional<? extends Dynamic<?>> statusOpt = chunkTag.get("Status").result();
      if (statusOpt.isPresent()) {
         Dynamic<?> status = (Dynamic<?>)statusOpt.get();
         String lastStatus = status.asString("");
         if (!"empty".equals(lastStatus)) {
            chunkTag = chunkTag.set(
               "blending_data",
               chunkTag.createMap(ImmutableMap.of(chunkTag.createString("old_noise"), chunkTag.createBoolean(STATUS_IS_OR_AFTER_NOISE.contains(lastStatus))))
            );
            if (!SharedConstants.DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION) {
               ChunkProtoTickListFix.PoorMansPalettedContainer poorMansPalettedContainer = bedrockSectionBlocks.get();
               if (poorMansPalettedContainer != null) {
                  BitSet missingBedrock = new BitSet(256);
                  boolean hasAnyBedrock = lastStatus.equals("noise");

                  for (int z = 0; z < 16; z++) {
                     for (int x = 0; x < 16; x++) {
                        Dynamic<?> blockState = poorMansPalettedContainer.get(x, 0, z);
                        boolean isBedrock = blockState != null && "minecraft:bedrock".equals(blockState.get("Name").asString(""));
                        boolean isAir = blockState != null && "minecraft:air".equals(blockState.get("Name").asString(""));
                        if (isAir) {
                           missingBedrock.set(z * 16 + x);
                        }

                        hasAnyBedrock |= isBedrock;
                     }
                  }

                  if (hasAnyBedrock && missingBedrock.cardinality() != missingBedrock.size()) {
                     Dynamic<?> targetStatus = "full".equals(lastStatus) ? chunkTag.createString("heightmaps") : status;
                     chunkTag = chunkTag.set(
                        "below_zero_retrogen",
                        chunkTag.createMap(
                           ImmutableMap.of(
                              chunkTag.createString("target_status"),
                              targetStatus,
                              chunkTag.createString("missing_bedrock"),
                              chunkTag.createLongList(LongStream.of(missingBedrock.toLongArray()))
                           )
                        )
                     );
                     chunkTag = chunkTag.set("Status", chunkTag.createString("empty"));
                  }

                  chunkTag = chunkTag.set("isLightOn", chunkTag.createBoolean(false));
               }
            }
         }
      }

      return chunkTag;
   }

   private static <T> Dynamic<T> shiftUpgradeData(final Dynamic<T> upgradeData) {
      return upgradeData.update("Indices", indices -> {
         Map<Dynamic<?>, Dynamic<?>> shiftedIndices = new HashMap<>();
         indices.getMapValues().ifSuccess(entries -> entries.forEach((index, data) -> {
            try {
               index.asString().result().map(Integer::parseInt).ifPresent(i -> {
                  int shiftedIndex = i - -4;
                  shiftedIndices.put(index.createString(Integer.toString(shiftedIndex)), (Dynamic<?>)data);
               });
            } catch (NumberFormatException var4) {
            }
         }));
         return indices.createMap(shiftedIndices);
      });
   }

   private static Dynamic<?> updateCarvingMasks(final Dynamic<?> chunkTag, final int sectionCount, final int addedSectionsBelow) {
      Dynamic<?> carvingMasks = chunkTag.get("CarvingMasks").orElseEmptyMap();
      carvingMasks = carvingMasks.updateMapValues(pair -> {
         long[] oldValues = BitSet.valueOf(((Dynamic)pair.getSecond()).asByteBuffer().array()).toLongArray();
         long[] newValues = new long[64 * sectionCount];
         System.arraycopy(oldValues, 0, newValues, 64 * addedSectionsBelow, oldValues.length);
         return Pair.of((Dynamic)pair.getFirst(), chunkTag.createLongList(LongStream.of(newValues)));
      });
      return chunkTag.set("CarvingMasks", carvingMasks);
   }

   private static Dynamic<?> addPaddingEntries(final Dynamic<?> chunkTag, final String key) {
      List<Dynamic<?>> list = chunkTag.get(key).orElseEmptyList().asStream().collect(Collectors.toCollection(ArrayList::new));
      if (list.size() == 24) {
         return chunkTag;
      }

      Dynamic<?> emptyList = chunkTag.emptyList();

      for (int i = 0; i < 4; i++) {
         list.add(0, emptyList);
         list.add(emptyList);
      }

      return chunkTag.set(key, chunkTag.createList(list.stream()));
   }

   private static Dynamic<?> updateHeightmaps(final Dynamic<?> chunkTag) {
      return chunkTag.update("Heightmaps", heightmapTag -> {
         for (String heightmapType : HEIGHTMAP_TYPES) {
            heightmapTag = heightmapTag.update(heightmapType, ChunkHeightAndBiomeFix::getFixedHeightmap);
         }

         return heightmapTag;
      });
   }

   private static Dynamic<?> getFixedHeightmap(final Dynamic<?> tag) {
      return tag.createLongList(tag.asLongStream().map(value -> {
         long newValue = 0L;

         for (int bitIndex = 0; bitIndex + 9 <= 64; bitIndex += 9) {
            long oldHeight = value >> bitIndex & 511L;
            long newHeight;
            if (oldHeight == 0L) {
               newHeight = 0L;
            } else {
               newHeight = Math.min(oldHeight + 64L, 511L);
            }

            newValue |= newHeight << bitIndex;
         }

         return newValue;
      }));
   }

   private static Dynamic<?> makeBiomeContainer(final Dynamic<?> tag, final Int2IntFunction sourceStorage) {
      Int2IntMap idMap = new Int2IntLinkedOpenHashMap();

      for (int i = 0; i < 64; i++) {
         int biomeId = sourceStorage.applyAsInt(i);
         if (!idMap.containsKey(biomeId)) {
            idMap.put(biomeId, idMap.size());
         }
      }

      Dynamic<?> palette = tag.createList(
         idMap.keySet().stream().map(biomeId1 -> tag.createString((String)BIOMES_BY_ID.getOrDefault(biomeId1, "minecraft:plains")))
      );
      int bits = ceillog2(idMap.size());
      if (bits == 0) {
         return makePalettedContainer(palette);
      }

      int valuesPerLong = 64 / bits;
      int requiredLength = (64 + valuesPerLong - 1) / valuesPerLong;
      long[] bitStorage = new long[requiredLength];
      int cellIndex = 0;
      int bitIndex = 0;

      for (int i = 0; i < 64; i++) {
         int biomeId = sourceStorage.applyAsInt(i);
         bitStorage[cellIndex] |= (long)idMap.get(biomeId) << bitIndex;
         bitIndex += bits;
         if (bitIndex + bits > 64) {
            cellIndex++;
            bitIndex = 0;
         }
      }

      Dynamic<?> storage = tag.createLongList(Arrays.stream(bitStorage));
      return makePalettedContainer(palette, storage);
   }

   private static Dynamic<?> makePalettedContainer(final Dynamic<?> palette) {
      return palette.createMap(ImmutableMap.of(palette.createString("palette"), palette));
   }

   private static Dynamic<?> makePalettedContainer(final Dynamic<?> palette, final Dynamic<?> storage) {
      return palette.createMap(ImmutableMap.of(palette.createString("palette"), palette, palette.createString("data"), storage));
   }

   private static Dynamic<?> makeOptimizedPalettedContainer(Dynamic<?> palette, final Dynamic<?> data) {
      List<Dynamic<?>> paletteList = palette.asStream().collect(Collectors.toCollection(ArrayList::new));
      if (paletteList.size() == 1) {
         return makePalettedContainer(palette);
      }

      palette = padPaletteEntries(palette, data, paletteList);
      return makePalettedContainer(palette, data);
   }

   private static Dynamic<?> padPaletteEntries(final Dynamic<?> palette, final Dynamic<?> data, final List<Dynamic<?>> paletteList) {
      long dataSizeInBits = data.asLongStream().count() * 64L;
      long estimatedBitsPerBlock = dataSizeInBits / 4096L;
      int paletteSize = paletteList.size();
      int expectedBitsPerBlock = ceillog2(paletteSize);
      if (estimatedBitsPerBlock <= expectedBitsPerBlock) {
         return palette;
      }

      Dynamic<?> airPalleteEntry = palette.createMap(ImmutableMap.of(palette.createString("Name"), palette.createString("minecraft:air")));
      int minimumPaletteSizeToMatchData = (1 << (int)(estimatedBitsPerBlock - 1L)) + 1;
      int additionalPaletteEntries = minimumPaletteSizeToMatchData - paletteSize;

      for (int i = 0; i < additionalPaletteEntries; i++) {
         paletteList.add(airPalleteEntry);
      }

      return palette.createList(paletteList.stream());
   }

   public static int ceillog2(final int input) {
      return input == 0 ? 0 : (int)Math.ceil(Math.log(input) / Math.log(2.0));
   }

   static {
      BIOMES_BY_ID.put(0, "minecraft:ocean");
      BIOMES_BY_ID.put(1, "minecraft:plains");
      BIOMES_BY_ID.put(2, "minecraft:desert");
      BIOMES_BY_ID.put(3, "minecraft:mountains");
      BIOMES_BY_ID.put(4, "minecraft:forest");
      BIOMES_BY_ID.put(5, "minecraft:taiga");
      BIOMES_BY_ID.put(6, "minecraft:swamp");
      BIOMES_BY_ID.put(7, "minecraft:river");
      BIOMES_BY_ID.put(8, "minecraft:nether_wastes");
      BIOMES_BY_ID.put(9, "minecraft:the_end");
      BIOMES_BY_ID.put(10, "minecraft:frozen_ocean");
      BIOMES_BY_ID.put(11, "minecraft:frozen_river");
      BIOMES_BY_ID.put(12, "minecraft:snowy_tundra");
      BIOMES_BY_ID.put(13, "minecraft:snowy_mountains");
      BIOMES_BY_ID.put(14, "minecraft:mushroom_fields");
      BIOMES_BY_ID.put(15, "minecraft:mushroom_field_shore");
      BIOMES_BY_ID.put(16, "minecraft:beach");
      BIOMES_BY_ID.put(17, "minecraft:desert_hills");
      BIOMES_BY_ID.put(18, "minecraft:wooded_hills");
      BIOMES_BY_ID.put(19, "minecraft:taiga_hills");
      BIOMES_BY_ID.put(20, "minecraft:mountain_edge");
      BIOMES_BY_ID.put(21, "minecraft:jungle");
      BIOMES_BY_ID.put(22, "minecraft:jungle_hills");
      BIOMES_BY_ID.put(23, "minecraft:jungle_edge");
      BIOMES_BY_ID.put(24, "minecraft:deep_ocean");
      BIOMES_BY_ID.put(25, "minecraft:stone_shore");
      BIOMES_BY_ID.put(26, "minecraft:snowy_beach");
      BIOMES_BY_ID.put(27, "minecraft:birch_forest");
      BIOMES_BY_ID.put(28, "minecraft:birch_forest_hills");
      BIOMES_BY_ID.put(29, "minecraft:dark_forest");
      BIOMES_BY_ID.put(30, "minecraft:snowy_taiga");
      BIOMES_BY_ID.put(31, "minecraft:snowy_taiga_hills");
      BIOMES_BY_ID.put(32, "minecraft:giant_tree_taiga");
      BIOMES_BY_ID.put(33, "minecraft:giant_tree_taiga_hills");
      BIOMES_BY_ID.put(34, "minecraft:wooded_mountains");
      BIOMES_BY_ID.put(35, "minecraft:savanna");
      BIOMES_BY_ID.put(36, "minecraft:savanna_plateau");
      BIOMES_BY_ID.put(37, "minecraft:badlands");
      BIOMES_BY_ID.put(38, "minecraft:wooded_badlands_plateau");
      BIOMES_BY_ID.put(39, "minecraft:badlands_plateau");
      BIOMES_BY_ID.put(40, "minecraft:small_end_islands");
      BIOMES_BY_ID.put(41, "minecraft:end_midlands");
      BIOMES_BY_ID.put(42, "minecraft:end_highlands");
      BIOMES_BY_ID.put(43, "minecraft:end_barrens");
      BIOMES_BY_ID.put(44, "minecraft:warm_ocean");
      BIOMES_BY_ID.put(45, "minecraft:lukewarm_ocean");
      BIOMES_BY_ID.put(46, "minecraft:cold_ocean");
      BIOMES_BY_ID.put(47, "minecraft:deep_warm_ocean");
      BIOMES_BY_ID.put(48, "minecraft:deep_lukewarm_ocean");
      BIOMES_BY_ID.put(49, "minecraft:deep_cold_ocean");
      BIOMES_BY_ID.put(50, "minecraft:deep_frozen_ocean");
      BIOMES_BY_ID.put(127, "minecraft:the_void");
      BIOMES_BY_ID.put(129, "minecraft:sunflower_plains");
      BIOMES_BY_ID.put(130, "minecraft:desert_lakes");
      BIOMES_BY_ID.put(131, "minecraft:gravelly_mountains");
      BIOMES_BY_ID.put(132, "minecraft:flower_forest");
      BIOMES_BY_ID.put(133, "minecraft:taiga_mountains");
      BIOMES_BY_ID.put(134, "minecraft:swamp_hills");
      BIOMES_BY_ID.put(140, "minecraft:ice_spikes");
      BIOMES_BY_ID.put(149, "minecraft:modified_jungle");
      BIOMES_BY_ID.put(151, "minecraft:modified_jungle_edge");
      BIOMES_BY_ID.put(155, "minecraft:tall_birch_forest");
      BIOMES_BY_ID.put(156, "minecraft:tall_birch_hills");
      BIOMES_BY_ID.put(157, "minecraft:dark_forest_hills");
      BIOMES_BY_ID.put(158, "minecraft:snowy_taiga_mountains");
      BIOMES_BY_ID.put(160, "minecraft:giant_spruce_taiga");
      BIOMES_BY_ID.put(161, "minecraft:giant_spruce_taiga_hills");
      BIOMES_BY_ID.put(162, "minecraft:modified_gravelly_mountains");
      BIOMES_BY_ID.put(163, "minecraft:shattered_savanna");
      BIOMES_BY_ID.put(164, "minecraft:shattered_savanna_plateau");
      BIOMES_BY_ID.put(165, "minecraft:eroded_badlands");
      BIOMES_BY_ID.put(166, "minecraft:modified_wooded_badlands_plateau");
      BIOMES_BY_ID.put(167, "minecraft:modified_badlands_plateau");
      BIOMES_BY_ID.put(168, "minecraft:bamboo_jungle");
      BIOMES_BY_ID.put(169, "minecraft:bamboo_jungle_hills");
      BIOMES_BY_ID.put(170, "minecraft:soul_sand_valley");
      BIOMES_BY_ID.put(171, "minecraft:crimson_forest");
      BIOMES_BY_ID.put(172, "minecraft:warped_forest");
      BIOMES_BY_ID.put(173, "minecraft:basalt_deltas");
      BIOMES_BY_ID.put(174, "minecraft:dripstone_caves");
      BIOMES_BY_ID.put(175, "minecraft:lush_caves");
      BIOMES_BY_ID.put(177, "minecraft:meadow");
      BIOMES_BY_ID.put(178, "minecraft:grove");
      BIOMES_BY_ID.put(179, "minecraft:snowy_slopes");
      BIOMES_BY_ID.put(180, "minecraft:snowcapped_peaks");
      BIOMES_BY_ID.put(181, "minecraft:lofty_peaks");
      BIOMES_BY_ID.put(182, "minecraft:stony_peaks");
   }
}
