package net.minecraft.world.level.chunk;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class ChunkGenerator {
   public static final Codec<ChunkGenerator> CODEC = BuiltInRegistries.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   protected final BiomeSource biomeSource;
   private final Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;
   private final Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter;

   public ChunkGenerator(final BiomeSource biomeSource) {
      this(biomeSource, biome -> biome.value().getGenerationSettings());
   }

   public ChunkGenerator(final BiomeSource biomeSource, final Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter) {
      this.biomeSource = biomeSource;
      this.generationSettingsGetter = generationSettingsGetter;
      this.featuresPerStep = Suppliers.memoize(
         () -> FeatureSorter.buildFeaturesPerStep(List.copyOf(biomeSource.possibleBiomes()), b -> generationSettingsGetter.apply(b).features(), true)
      );
   }

   public void validate() {
      this.featuresPerStep.get();
   }

   public ChunkPos getOrigin(final RandomState randomState) {
      return ChunkPos.ZERO;
   }

   protected abstract MapCodec<? extends ChunkGenerator> codec();

   public ChunkGeneratorStructureState createState(final HolderLookup<StructureSet> structureSets, final RandomState randomState, final long legacyLevelSeed) {
      return ChunkGeneratorStructureState.createForNormal(randomState, legacyLevelSeed, this.getOrigin(randomState), this.biomeSource, structureSets);
   }

   public Optional<Identifier> getTypeNameForDataFixer() {
      return BuiltInRegistries.CHUNK_GENERATOR.getResourceKey(this.codec()).map(ResourceKey::identifier);
   }

   public CompletableFuture<ChunkAccess> createBiomes(
      final RandomState randomState, final Blender blender, final StructureManager structureManager, final ChunkAccess protoChunk
   ) {
      return CompletableFuture.supplyAsync(() -> {
         protoChunk.fillBiomesFromNoise(this.biomeSource, randomState.sampler());
         return protoChunk;
      }, Util.backgroundExecutor().forName("init_biomes"));
   }

   public abstract void applyCarvers(
      WorldGenRegion region,
      long seed,
      RandomState randomState,
      BiomeManager biomeManager,
      StructureManager structureManager,
      ChunkAccess chunk,
      CarvingMask.@Nullable Filter filter
   );

   public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
      final ServerLevel level, final HolderSet<Structure> wantedStructures, final BlockPos pos, final int maxSearchRadius, final boolean createReference
   ) {
      if (SharedConstants.DEBUG_DISABLE_FEATURES) {
         return null;
      }

      if (!level.getServer().getWorldGenSettings().options().generateStructures()) {
         return null;
      }

      ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
      Map<StructurePlacement, Set<Holder<Structure>>> placementScans = new Object2ObjectArrayMap();

      for (Holder<Structure> structure : wantedStructures) {
         for (StructurePlacement placement : generatorState.getPlacementsForStructure(structure)) {
            placementScans.computeIfAbsent(placement, p -> new ObjectArraySet()).add(structure);
         }
      }

      if (placementScans.isEmpty()) {
         return null;
      }

      Pair<BlockPos, Holder<Structure>> nearest = null;
      double distanceSqr = Double.MAX_VALUE;
      StructureManager structureManager = level.structureManager();
      List<Entry<StructurePlacement, Set<Holder<Structure>>>> randomSpreadEntries = new ArrayList<>(placementScans.size());

      for (Entry<StructurePlacement, Set<Holder<Structure>>> entry : placementScans.entrySet()) {
         StructurePlacement placement = entry.getKey();
         if (placement instanceof ConcentricRingsStructurePlacement rings) {
            Pair<BlockPos, Holder<Structure>> generating = this.getNearestGeneratedStructure(
               entry.getValue(), level, structureManager, pos, createReference, rings
            );
            if (generating != null) {
               BlockPos structurePos = (BlockPos)generating.getFirst();
               double newDistanceSqr = pos.distSqr(structurePos);
               if (newDistanceSqr < distanceSqr) {
                  distanceSqr = newDistanceSqr;
                  nearest = generating;
               }
            }
         } else if (placement instanceof RandomSpreadStructurePlacement) {
            randomSpreadEntries.add(entry);
         }
      }

      if (!randomSpreadEntries.isEmpty()) {
         int chunkOriginX = SectionPos.blockToSectionCoord(pos.getX());
         int chunkOriginZ = SectionPos.blockToSectionCoord(pos.getZ());

         for (int radius = 0; radius <= maxSearchRadius; radius++) {
            boolean foundSomething = false;

            for (Entry<StructurePlacement, Set<Holder<Structure>>> entry : randomSpreadEntries) {
               RandomSpreadStructurePlacement randomPlacement = (RandomSpreadStructurePlacement)entry.getKey();
               Pair<BlockPos, Holder<Structure>> structurePos = getNearestGeneratedStructure(
                  entry.getValue(),
                  level,
                  structureManager,
                  chunkOriginX,
                  chunkOriginZ,
                  radius,
                  createReference,
                  generatorState.getLevelSeed(),
                  randomPlacement
               );
               if (structurePos != null) {
                  foundSomething = true;
                  double newDistanceSqr = pos.distSqr((Vec3i)structurePos.getFirst());
                  if (newDistanceSqr < distanceSqr) {
                     distanceSqr = newDistanceSqr;
                     nearest = structurePos;
                  }
               }
            }

            if (foundSomething) {
               return nearest;
            }
         }
      }

      return nearest;
   }

   private @Nullable Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      final Set<Holder<Structure>> structures,
      final ServerLevel level,
      final StructureManager structureManager,
      final BlockPos pos,
      final boolean createReference,
      final ConcentricRingsStructurePlacement rings
   ) {
      List<ChunkPos> positions = level.getChunkSource().getGeneratorState().getRingPositionsFor(rings);
      if (positions == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      }

      Pair<BlockPos, Holder<Structure>> closestPos = null;
      double closest = Double.MAX_VALUE;
      BlockPos.MutableBlockPos structurePos = new BlockPos.MutableBlockPos();

      for (ChunkPos chunkPos : positions) {
         structurePos.set(SectionPos.sectionToBlockCoord(chunkPos.x(), 8), 32, SectionPos.sectionToBlockCoord(chunkPos.z(), 8));
         double distSqr = structurePos.distSqr(pos);
         boolean isClosest = closestPos == null || distSqr < closest;
         if (isClosest) {
            Pair<BlockPos, Holder<Structure>> generating = getStructureGeneratingAt(structures, level, structureManager, createReference, rings, chunkPos);
            if (generating != null) {
               closestPos = generating;
               closest = distSqr;
            }
         }
      }

      return closestPos;
   }

   private static @Nullable Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      final Set<Holder<Structure>> structures,
      final LevelReader level,
      final StructureManager structureManager,
      final int chunkOriginX,
      final int chunkOriginZ,
      final int radius,
      final boolean createReference,
      final long seed,
      final RandomSpreadStructurePlacement config
   ) {
      int spacing = config.spacing();

      for (int x = -radius; x <= radius; x++) {
         boolean xEdge = x == -radius || x == radius;

         for (int z = -radius; z <= radius; z++) {
            boolean zEdge = z == -radius || z == radius;
            if (xEdge || zEdge) {
               int sectorX = chunkOriginX + spacing * x;
               int sectorZ = chunkOriginZ + spacing * z;
               ChunkPos chunkTarget = config.getPotentialStructureChunk(seed, sectorX, sectorZ);
               Pair<BlockPos, Holder<Structure>> generating = getStructureGeneratingAt(
                  structures, level, structureManager, createReference, config, chunkTarget
               );
               if (generating != null) {
                  return generating;
               }
            }
         }
      }

      return null;
   }

   private static @Nullable Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(
      final Set<Holder<Structure>> structures,
      final LevelReader level,
      final StructureManager structureManager,
      final boolean createReference,
      final StructurePlacement config,
      final ChunkPos chunkTarget
   ) {
      for (Holder<Structure> structure : structures) {
         StructureCheckResult fastCheckResult = structureManager.checkStructurePresence(chunkTarget, structure.value(), config, createReference);
         if (fastCheckResult != StructureCheckResult.START_NOT_PRESENT) {
            if (!createReference && fastCheckResult == StructureCheckResult.START_PRESENT) {
               return Pair.of(config.getLocatePos(chunkTarget), structure);
            }

            ChunkAccess chunk = level.getChunk(chunkTarget.x(), chunkTarget.z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = structureManager.getStartForStructure(SectionPos.bottomOf(chunk), structure.value(), chunk);
            if (start != null && start.isValid() && (!createReference || tryAddReference(structureManager, start))) {
               return Pair.of(config.getLocatePos(start.getChunkPos()), structure);
            }
         }
      }

      return null;
   }

   private static boolean tryAddReference(final StructureManager manager, final StructureStart start) {
      if (start.canBeReferenced()) {
         manager.addReference(start);
         return true;
      } else {
         return false;
      }
   }

   public void applyBiomeDecoration(final WorldGenLevel level, final ChunkAccess chunk, final StructureManager structureManager) {
      ChunkPos centerPos = chunk.getPos();
      if (!SharedConstants.debugVoidTerrain(centerPos)) {
         SectionPos sectionPos = SectionPos.of(centerPos, level.getMinSectionY());
         BlockPos origin = sectionPos.origin();
         Registry<Structure> structuresRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
         Map<Integer, List<Structure>> structuresByStep = structuresRegistry.stream().collect(Collectors.groupingBy(structure -> structure.step().ordinal()));
         List<FeatureSorter.StepFeatureData> featureList = this.featuresPerStep.get();
         WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
         long decorationSeed = random.setDecorationSeed(level.getSeed(), origin.getX(), origin.getZ());
         Set<Holder<Biome>> possibleBiomes = new ObjectArraySet();
         ChunkPos.rangeClosed(sectionPos.chunk(), 1).forEach(chunkPos -> {
            ChunkAccess chunkInRange = level.getChunk(chunkPos.x(), chunkPos.z());

            for (LevelChunkSection section : chunkInRange.getSections()) {
               section.getBiomes().getAll(possibleBiomes::add);
            }
         });
         possibleBiomes.retainAll(this.biomeSource.possibleBiomes());
         int featureStepCount = featureList.size();
         FeaturePlacer placer = new FeaturePlacer(level, this);

         try {
            Registry<PlacedFeature> featureRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            int generationSteps = Math.max(GenerationStep.Decoration.values().length, featureStepCount);

            for (int stepIndex = 0; stepIndex < generationSteps; stepIndex++) {
               int index = 0;
               if (structureManager.shouldGenerateStructures()) {
                  for (Structure structure : structuresByStep.getOrDefault(stepIndex, Collections.emptyList())) {
                     random.setFeatureSeed(decorationSeed, index, stepIndex);
                     Supplier<String> currentlyGenerating = () -> structuresRegistry.getResourceKey(structure)
                        .map(Object::toString)
                        .orElseGet(structure::toString);

                     try {
                        level.setCurrentlyGenerating(currentlyGenerating);
                        structureManager.startsForStructure(sectionPos, structure)
                           .forEach(start -> start.placeInChunk(level, structureManager, this, random, getWritableArea(chunk), centerPos));
                     } catch (Exception e) {
                        CrashReport report = CrashReport.forThrowable(e, "Feature placement");
                        report.addCategory("Feature").setDetail("Description", currentlyGenerating::get);
                        throw new ReportedException(report);
                     }

                     index++;
                  }
               }

               if (stepIndex < featureStepCount) {
                  IntSet possibleFeaturesThisStep = new IntArraySet();

                  for (Holder<Biome> biome : possibleBiomes) {
                     List<HolderSet<PlacedFeature>> featuresInBiome = this.generationSettingsGetter.apply(biome).features();
                     if (stepIndex < featuresInBiome.size()) {
                        HolderSet<PlacedFeature> featuresInBiomeThisStep = featuresInBiome.get(stepIndex);
                        FeatureSorter.StepFeatureData stepFeatureData = featureList.get(stepIndex);
                        featuresInBiomeThisStep.stream()
                           .map(Holder::value)
                           .forEach(featurex -> possibleFeaturesThisStep.add(stepFeatureData.indexMapping().applyAsInt(featurex)));
                     }
                  }

                  int numberOfFeaturesInStep = possibleFeaturesThisStep.size();
                  int[] indexArray = possibleFeaturesThisStep.toIntArray();
                  Arrays.sort(indexArray);
                  FeatureSorter.StepFeatureData stepFeatureData = featureList.get(stepIndex);

                  for (int featureIndex = 0; featureIndex < numberOfFeaturesInStep; featureIndex++) {
                     int globalIndexOfFeature = indexArray[featureIndex];
                     PlacedFeature feature = stepFeatureData.features().get(globalIndexOfFeature);
                     Supplier<String> currentlyGenerating = () -> featureRegistry.getResourceKey(feature).map(Object::toString).orElseGet(feature::toString);
                     random.setFeatureSeed(decorationSeed, globalIndexOfFeature, stepIndex);

                     try {
                        level.setCurrentlyGenerating(currentlyGenerating);
                        placer.placeWithBiomeCheck(feature, random, origin);
                     } catch (Exception e) {
                        CrashReport report = CrashReport.forThrowable(e, "Feature placement");
                        report.addCategory("Feature").setDetail("Description", currentlyGenerating::get);
                        throw new ReportedException(report);
                     }
                  }
               }
            }

            level.setCurrentlyGenerating(null);
            if (SharedConstants.DEBUG_FEATURE_COUNT) {
               FeatureCountTracker.chunkDecorated(level.getLevel());
            }
         } catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Biome decoration");
            report.addCategory("Generation")
               .setDetail("CenterX", centerPos.x())
               .setDetail("CenterZ", centerPos.z())
               .setDetail("Decoration Seed", decorationSeed);
            throw new ReportedException(report);
         }
      }
   }

   private static BoundingBox getWritableArea(final ChunkAccess chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int targetBlockX = chunkPos.getMinBlockX();
      int targetBlockZ = chunkPos.getMinBlockZ();
      LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
      int minY = heightAccessor.getMinY() + 1;
      int maxY = heightAccessor.getMaxY();
      return new BoundingBox(targetBlockX, minY, targetBlockZ, targetBlockX + 15, maxY, targetBlockZ + 15);
   }

   public abstract void buildSurface(final WorldGenRegion level, final StructureManager structureManager, final RandomState randomState, ChunkAccess protoChunk);

   public abstract void spawnOriginalMobs(WorldGenRegion worldGenRegion);

   public int getSpawnHeight(final LevelHeightAccessor heightAccessor) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.biomeSource;
   }

   public abstract int getGenDepth();

   public WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(
      final Holder<Biome> biome, final StructureManager structureManager, final MobCategory mobCategory, final BlockPos pos
   ) {
      Map<Structure, LongSet> structures = structureManager.getAllStructuresAt(pos);

      for (Entry<Structure, LongSet> entry : structures.entrySet()) {
         Structure structure = entry.getKey();
         StructureSpawnOverride override = structure.spawnOverrides().get(mobCategory);
         if (override != null) {
            MutableBoolean inOverrideBox = new MutableBoolean(false);
            Predicate<StructureStart> check = override.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE
               ? start -> structureManager.structureHasPieceAt(pos, start)
               : start -> start.getBoundingBox().isInside(pos);
            structureManager.fillStartsForStructure(structure, entry.getValue(), start -> {
               if (inOverrideBox.isFalse() && check.test(start)) {
                  inOverrideBox.setTrue();
               }
            });
            if (inOverrideBox.isTrue()) {
               return override.spawns();
            }
         }
      }

      return biome.value().getMobSettings().getMobs(mobCategory);
   }

   public void createStructures(
      final RegistryAccess registryAccess,
      final ChunkGeneratorStructureState state,
      final StructureManager structureManager,
      final ChunkAccess centerChunk,
      final StructureTemplateManager structureTemplateManager,
      final ResourceKey<Level> level
   ) {
      if (!SharedConstants.DEBUG_DISABLE_STRUCTURES) {
         ChunkPos sourceChunkPos = centerChunk.getPos();
         SectionPos sectionPos = SectionPos.bottomOf(centerChunk);
         RandomState randomState = state.randomState();
         state.possibleStructureSets()
            .forEach(
               set -> {
                  StructurePlacement featurePlacement = set.value().placement();
                  List<StructureSet.StructureSelectionEntry> structures = set.value().structures();

                  for (StructureSet.StructureSelectionEntry structure : structures) {
                     StructureStart existingStart = structureManager.getStartForStructure(sectionPos, structure.structure().value(), centerChunk);
                     if (existingStart != null && existingStart.isValid()) {
                        return;
                     }
                  }

                  if (featurePlacement.isStructureChunk(state, sourceChunkPos.x(), sourceChunkPos.z())) {
                     if (structures.size() == 1) {
                        this.tryGenerateStructure(
                           structures.get(0),
                           structureManager,
                           registryAccess,
                           randomState,
                           structureTemplateManager,
                           state.getLevelSeed(),
                           centerChunk,
                           sourceChunkPos,
                           sectionPos,
                           level
                        );
                     } else {
                        ArrayList<StructureSet.StructureSelectionEntry> options = new ArrayList<>(structures.size());
                        options.addAll(structures);
                        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
                        random.setLargeFeatureSeed(state.getLevelSeed(), sourceChunkPos.x(), sourceChunkPos.z());
                        int total = 0;

                        for (StructureSet.StructureSelectionEntry option : options) {
                           total += option.weight();
                        }

                        while (!options.isEmpty()) {
                           int choice = random.nextInt(total);
                           int index = 0;

                           for (StructureSet.StructureSelectionEntry option : options) {
                              choice -= option.weight();
                              if (choice < 0) {
                                 break;
                              }

                              index++;
                           }

                           StructureSet.StructureSelectionEntry selected = options.get(index);
                           if (this.tryGenerateStructure(
                              selected,
                              structureManager,
                              registryAccess,
                              randomState,
                              structureTemplateManager,
                              state.getLevelSeed(),
                              centerChunk,
                              sourceChunkPos,
                              sectionPos,
                              level
                           )) {
                              return;
                           }

                           options.remove(index);
                           total -= selected.weight();
                        }
                     }
                  }
               }
            );
      }
   }

   private boolean tryGenerateStructure(
      final StructureSet.StructureSelectionEntry selected,
      final StructureManager structureManager,
      final RegistryAccess registryAccess,
      final RandomState randomState,
      final StructureTemplateManager structureTemplateManager,
      final long seed,
      final ChunkAccess centerChunk,
      final ChunkPos sourceChunkPos,
      final SectionPos sectionPos,
      final ResourceKey<Level> level
   ) {
      Structure structure = selected.structure().value();
      int references = fetchReferences(structureManager, centerChunk, sectionPos, structure);
      HolderSet<Biome> biomeAllowedForStructure = structure.biomes();
      Predicate<Holder<Biome>> biomePredicate = biomeAllowedForStructure::contains;
      StructureStart start = structure.generate(
         selected.structure(),
         level,
         registryAccess,
         this,
         this.biomeSource,
         randomState,
         structureTemplateManager,
         seed,
         sourceChunkPos,
         references,
         centerChunk,
         biomePredicate
      );
      if (start.isValid()) {
         structureManager.setStartForStructure(sectionPos, structure, start, centerChunk);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(
      final StructureManager structureManager, final ChunkAccess centerChunk, final SectionPos sectionPos, final Structure structure
   ) {
      StructureStart prevEntry = structureManager.getStartForStructure(sectionPos, structure, centerChunk);
      return prevEntry != null ? prevEntry.getReferences() : 0;
   }

   public void createReferences(final WorldGenLevel level, final StructureManager structureManager, final ChunkAccess centerChunk) {
      int range = 8;
      ChunkPos chunkPos = centerChunk.getPos();
      int targetX = chunkPos.x();
      int targetZ = chunkPos.z();
      int targetBlockX = chunkPos.getMinBlockX();
      int targetBlockZ = chunkPos.getMinBlockZ();
      SectionPos pos = SectionPos.bottomOf(centerChunk);

      for (int sourceX = targetX - 8; sourceX <= targetX + 8; sourceX++) {
         for (int sourceZ = targetZ - 8; sourceZ <= targetZ + 8; sourceZ++) {
            long sourceChunkKey = ChunkPos.pack(sourceX, sourceZ);

            for (StructureStart start : level.getChunk(sourceX, sourceZ).getAllStarts().values()) {
               try {
                  if (start.isValid() && start.getBoundingBox().intersects(targetBlockX, targetBlockZ, targetBlockX + 15, targetBlockZ + 15)) {
                     structureManager.addReferenceForStructure(pos, start.getStructure(), sourceChunkKey, centerChunk);
                  }
               } catch (Exception e) {
                  CrashReport report = CrashReport.forThrowable(e, "Generating structure reference");
                  CrashReportCategory structure = report.addCategory("Structure");
                  Optional<? extends Registry<Structure>> configuredStructuresRegistry = level.registryAccess().lookup(Registries.STRUCTURE);
                  structure.setDetail("Id", () -> configuredStructuresRegistry.<String>map(r -> r.getKey(start.getStructure()).toString()).orElse("UNKNOWN"));
                  structure.setDetail("Name", () -> BuiltInRegistries.STRUCTURE_TYPE.getKey(start.getStructure().type()).toString());
                  structure.setDetail("Class", () -> start.getStructure().getClass().getCanonicalName());
                  throw new ReportedException(report);
               }
            }
         }
      }
   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(
      final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk
   );

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int x, int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState);

   public abstract NoiseColumn getBaseColumn(final int x, final int z, final LevelHeightAccessor heightAccessor, final RandomState randomState);

   public int getFirstFreeHeight(final int x, final int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
      return this.getBaseHeight(x, z, type, heightAccessor, randomState);
   }

   public int getFirstOccupiedHeight(
      final int x, final int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState
   ) {
      return this.getBaseHeight(x, z, type, heightAccessor, randomState) - 1;
   }

   public abstract void addDebugScreenInfo(final List<String> result, final RandomState randomState, final BlockPos feetPos);

   @Deprecated
   public BiomeGenerationSettings getBiomeGenerationSettings(final Holder<Biome> biome) {
      return this.generationSettingsGetter.apply(biome);
   }
}
