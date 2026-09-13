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

   public ChunkGenerator(BiomeSource p_256133_) {
      this(p_256133_, p_223234_ -> p_223234_.value().getGenerationSettings());
   }

   public ChunkGenerator(BiomeSource p_255838_, Function<Holder<Biome>, BiomeGenerationSettings> p_256216_) {
      this.biomeSource = p_255838_;
      this.generationSettingsGetter = p_256216_;
      this.featuresPerStep = Suppliers.memoize(
         () -> FeatureSorter.buildFeaturesPerStep(List.copyOf(p_255838_.possibleBiomes()), p_223216_ -> p_256216_.apply(p_223216_).features(), true)
      );
   }

   public void validate() {
      this.featuresPerStep.get();
   }

   protected abstract MapCodec<? extends ChunkGenerator> codec();

   public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> p_256405_, RandomState p_256101_, long p_256018_) {
      return ChunkGeneratorStructureState.createForNormal(p_256101_, p_256018_, this.biomeSource, p_256405_);
   }

   public Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return BuiltInRegistries.CHUNK_GENERATOR.getResourceKey(this.codec());
   }

   public CompletableFuture<ChunkAccess> createBiomes(RandomState p_223160_, Blender p_223161_, StructureManager p_223162_, ChunkAccess p_223163_) {
      return CompletableFuture.supplyAsync(() -> {
         p_223163_.fillBiomesFromNoise(this.biomeSource, p_223160_.sampler());
         return p_223163_;
      }, Util.backgroundExecutor().forName("init_biomes"));
   }

   public abstract void applyCarvers(WorldGenRegion var1, long var2, RandomState var4, BiomeManager var5, StructureManager var6, ChunkAccess var7);

   public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
      ServerLevel p_223038_, HolderSet<Structure> p_223039_, BlockPos p_223040_, int p_223041_, boolean p_223042_
   ) {
      if (SharedConstants.DEBUG_DISABLE_FEATURES) {
         return null;
      }

      ChunkGeneratorStructureState chunkgeneratorstructurestate = p_223038_.getChunkSource().getGeneratorState();
      Map<StructurePlacement, Set<Holder<Structure>>> map = new Object2ObjectArrayMap();

      for (Holder<Structure> holder : p_223039_) {
         for (StructurePlacement structureplacement : chunkgeneratorstructurestate.getPlacementsForStructure(holder)) {
            map.computeIfAbsent(structureplacement, p_223127_ -> new ObjectArraySet()).add(holder);
         }
      }

      if (map.isEmpty()) {
         return null;
      }

      Pair<BlockPos, Holder<Structure>> pair2 = null;
      double d2 = Double.MAX_VALUE;
      StructureManager structuremanager = p_223038_.structureManager();
      List<Entry<StructurePlacement, Set<Holder<Structure>>>> list = new ArrayList<>(map.size());

      for (Entry<StructurePlacement, Set<Holder<Structure>>> entry : map.entrySet()) {
         StructurePlacement structureplacement1 = entry.getKey();
         if (structureplacement1 instanceof ConcentricRingsStructurePlacement concentricringsstructureplacement) {
            Pair<BlockPos, Holder<Structure>> pair = this.getNearestGeneratedStructure(
               entry.getValue(), p_223038_, structuremanager, p_223040_, p_223042_, concentricringsstructureplacement
            );
            if (pair != null) {
               BlockPos blockpos = (BlockPos)pair.getFirst();
               double d0 = p_223040_.distSqr(blockpos);
               if (d0 < d2) {
                  d2 = d0;
                  pair2 = pair;
               }
            }
         } else if (structureplacement1 instanceof RandomSpreadStructurePlacement) {
            list.add(entry);
         }
      }

      if (!list.isEmpty()) {
         int i = SectionPos.blockToSectionCoord(p_223040_.getX());
         int j = SectionPos.blockToSectionCoord(p_223040_.getZ());

         for (int k = 0; k <= p_223041_; k++) {
            boolean flag = false;

            for (Entry<StructurePlacement, Set<Holder<Structure>>> entry1 : list) {
               RandomSpreadStructurePlacement randomspreadstructureplacement = (RandomSpreadStructurePlacement)entry1.getKey();
               Pair<BlockPos, Holder<Structure>> pair1 = getNearestGeneratedStructure(
                  entry1.getValue(),
                  p_223038_,
                  structuremanager,
                  i,
                  j,
                  k,
                  p_223042_,
                  chunkgeneratorstructurestate.getLevelSeed(),
                  randomspreadstructureplacement
               );
               if (pair1 != null) {
                  flag = true;
                  double d1 = p_223040_.distSqr((Vec3i)pair1.getFirst());
                  if (d1 < d2) {
                     d2 = d1;
                     pair2 = pair1;
                  }
               }
            }

            if (flag) {
               return pair2;
            }
         }
      }

      return pair2;
   }

   private @Nullable Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      Set<Holder<Structure>> p_223182_,
      ServerLevel p_223183_,
      StructureManager p_223184_,
      BlockPos p_223185_,
      boolean p_223186_,
      ConcentricRingsStructurePlacement p_223187_
   ) {
      List<ChunkPos> list = p_223183_.getChunkSource().getGeneratorState().getRingPositionsFor(p_223187_);
      if (list == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      }

      Pair<BlockPos, Holder<Structure>> pair = null;
      double d0 = Double.MAX_VALUE;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (ChunkPos chunkpos : list) {
         blockpos$mutableblockpos.set(SectionPos.sectionToBlockCoord(chunkpos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkpos.z, 8));
         double d1 = blockpos$mutableblockpos.distSqr(p_223185_);
         boolean flag = pair == null || d1 < d0;
         if (flag) {
            Pair<BlockPos, Holder<Structure>> pair1 = getStructureGeneratingAt(p_223182_, p_223183_, p_223184_, p_223186_, p_223187_, chunkpos);
            if (pair1 != null) {
               pair = pair1;
               d0 = d1;
            }
         }
      }

      return pair;
   }

   private static @Nullable Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      Set<Holder<Structure>> p_223189_,
      LevelReader p_223190_,
      StructureManager p_223191_,
      int p_223192_,
      int p_223193_,
      int p_223194_,
      boolean p_223195_,
      long p_223196_,
      RandomSpreadStructurePlacement p_223197_
   ) {
      int i = p_223197_.spacing();

      for (int j = -p_223194_; j <= p_223194_; j++) {
         boolean flag = j == -p_223194_ || j == p_223194_;

         for (int k = -p_223194_; k <= p_223194_; k++) {
            boolean flag1 = k == -p_223194_ || k == p_223194_;
            if (flag || flag1) {
               int l = p_223192_ + i * j;
               int i1 = p_223193_ + i * k;
               ChunkPos chunkpos = p_223197_.getPotentialStructureChunk(p_223196_, l, i1);
               Pair<BlockPos, Holder<Structure>> pair = getStructureGeneratingAt(p_223189_, p_223190_, p_223191_, p_223195_, p_223197_, chunkpos);
               if (pair != null) {
                  return pair;
               }
            }
         }
      }

      return null;
   }

   private static @Nullable Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(
      Set<Holder<Structure>> p_223199_, LevelReader p_223200_, StructureManager p_223201_, boolean p_223202_, StructurePlacement p_223203_, ChunkPos p_223204_
   ) {
      for (Holder<Structure> holder : p_223199_) {
         StructureCheckResult structurecheckresult = p_223201_.checkStructurePresence(p_223204_, holder.value(), p_223203_, p_223202_);
         if (structurecheckresult != StructureCheckResult.START_NOT_PRESENT) {
            if (!p_223202_ && structurecheckresult == StructureCheckResult.START_PRESENT) {
               return Pair.of(p_223203_.getLocatePos(p_223204_), holder);
            }

            ChunkAccess chunkaccess = p_223200_.getChunk(p_223204_.x, p_223204_.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structurestart = p_223201_.getStartForStructure(SectionPos.bottomOf(chunkaccess), holder.value(), chunkaccess);
            if (structurestart != null && structurestart.isValid() && (!p_223202_ || tryAddReference(p_223201_, structurestart))) {
               return Pair.of(p_223203_.getLocatePos(structurestart.getChunkPos()), holder);
            }
         }
      }

      return null;
   }

   private static boolean tryAddReference(StructureManager p_223060_, StructureStart p_223061_) {
      if (p_223061_.canBeReferenced()) {
         p_223060_.addReference(p_223061_);
         return true;
      } else {
         return false;
      }
   }

   public void applyBiomeDecoration(WorldGenLevel p_223087_, ChunkAccess p_223088_, StructureManager p_223089_) {
      ChunkPos chunkpos = p_223088_.getPos();
      if (!SharedConstants.debugVoidTerrain(chunkpos)) {
         SectionPos sectionpos = SectionPos.of(chunkpos, p_223087_.getMinSectionY());
         BlockPos blockpos = sectionpos.origin();
         Registry<Structure> registry = p_223087_.registryAccess().lookupOrThrow(Registries.STRUCTURE);
         Map<Integer, List<Structure>> map = registry.stream().collect(Collectors.groupingBy(p_223103_ -> p_223103_.step().ordinal()));
         List<FeatureSorter.StepFeatureData> list = this.featuresPerStep.get();
         WorldgenRandom worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
         long i = worldgenrandom.setDecorationSeed(p_223087_.getSeed(), blockpos.getX(), blockpos.getZ());
         Set<Holder<Biome>> set = new ObjectArraySet();
         ChunkPos.rangeClosed(sectionpos.chunk(), 1).forEach(p_223093_ -> {
            ChunkAccess chunkaccess = p_223087_.getChunk(p_223093_.x, p_223093_.z);

            for (LevelChunkSection levelchunksection : chunkaccess.getSections()) {
               levelchunksection.getBiomes().getAll(set::add);
            }
         });
         set.retainAll(this.biomeSource.possibleBiomes());
         int j = list.size();

         try {
            Registry<PlacedFeature> registry1 = p_223087_.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            int i1 = Math.max(GenerationStep.Decoration.values().length, j);

            for (int k = 0; k < i1; k++) {
               int l = 0;
               if (p_223089_.shouldGenerateStructures()) {
                  for (Structure structure : map.getOrDefault(k, Collections.emptyList())) {
                     worldgenrandom.setFeatureSeed(i, l, k);
                     Supplier<String> supplier = () -> registry.getResourceKey(structure).map(Object::toString).orElseGet(structure::toString);

                     try {
                        p_223087_.setCurrentlyGenerating(supplier);
                        p_223089_.startsForStructure(sectionpos, structure)
                           .forEach(p_223086_ -> p_223086_.placeInChunk(p_223087_, p_223089_, this, worldgenrandom, getWritableArea(p_223088_), chunkpos));
                     } catch (Exception exception) {
                        CrashReport crashreport1 = CrashReport.forThrowable(exception, "Feature placement");
                        crashreport1.addCategory("Feature").setDetail("Description", supplier::get);
                        throw new ReportedException(crashreport1);
                     }

                     l++;
                  }
               }

               if (k < j) {
                  IntSet intset = new IntArraySet();

                  for (Holder<Biome> holder : set) {
                     List<HolderSet<PlacedFeature>> list1 = this.generationSettingsGetter.apply(holder).features();
                     if (k < list1.size()) {
                        HolderSet<PlacedFeature> holderset = list1.get(k);
                        FeatureSorter.StepFeatureData featuresorter$stepfeaturedata1 = list.get(k);
                        holderset.stream()
                           .map(Holder::value)
                           .forEach(p_223174_ -> intset.add(featuresorter$stepfeaturedata1.indexMapping().applyAsInt(p_223174_)));
                     }
                  }

                  int j1 = intset.size();
                  int[] aint = intset.toIntArray();
                  Arrays.sort(aint);
                  FeatureSorter.StepFeatureData featuresorter$stepfeaturedata = list.get(k);

                  for (int k1 = 0; k1 < j1; k1++) {
                     int l1 = aint[k1];
                     PlacedFeature placedfeature = featuresorter$stepfeaturedata.features().get(l1);
                     Supplier<String> supplier1 = () -> registry1.getResourceKey(placedfeature).map(Object::toString).orElseGet(placedfeature::toString);
                     worldgenrandom.setFeatureSeed(i, l1, k);

                     try {
                        p_223087_.setCurrentlyGenerating(supplier1);
                        placedfeature.placeWithBiomeCheck(p_223087_, this, worldgenrandom, blockpos);
                     } catch (Exception exception1) {
                        CrashReport crashreport2 = CrashReport.forThrowable(exception1, "Feature placement");
                        crashreport2.addCategory("Feature").setDetail("Description", supplier1::get);
                        throw new ReportedException(crashreport2);
                     }
                  }
               }
            }

            p_223087_.setCurrentlyGenerating(null);
            if (SharedConstants.DEBUG_FEATURE_COUNT) {
               FeatureCountTracker.chunkDecorated(p_223087_.getLevel());
            }
         } catch (Exception exception2) {
            CrashReport crashreport = CrashReport.forThrowable(exception2, "Biome decoration");
            crashreport.addCategory("Generation").setDetail("CenterX", chunkpos.x).setDetail("CenterZ", chunkpos.z).setDetail("Decoration Seed", i);
            throw new ReportedException(crashreport);
         }
      }
   }

   private static BoundingBox getWritableArea(ChunkAccess p_187718_) {
      ChunkPos chunkpos = p_187718_.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      LevelHeightAccessor levelheightaccessor = p_187718_.getHeightAccessorForGeneration();
      int k = levelheightaccessor.getMinY() + 1;
      int l = levelheightaccessor.getMaxY();
      return new BoundingBox(i, k, j, i + 15, l, j + 15);
   }

   public abstract void buildSurface(WorldGenRegion var1, StructureManager var2, RandomState var3, ChunkAccess var4);

   public abstract void spawnOriginalMobs(WorldGenRegion var1);

   public int getSpawnHeight(LevelHeightAccessor p_156157_) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.biomeSource;
   }

   public abstract int getGenDepth();

   public WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> p_223134_, StructureManager p_223135_, MobCategory p_223136_, BlockPos p_223137_) {
      Map<Structure, LongSet> map = p_223135_.getAllStructuresAt(p_223137_);

      for (Entry<Structure, LongSet> entry : map.entrySet()) {
         Structure structure = entry.getKey();
         StructureSpawnOverride structurespawnoverride = structure.spawnOverrides().get(p_223136_);
         if (structurespawnoverride != null) {
            MutableBoolean mutableboolean = new MutableBoolean(false);
            Predicate<StructureStart> predicate = structurespawnoverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE
               ? p_223065_ -> p_223135_.structureHasPieceAt(p_223137_, p_223065_)
               : p_223130_ -> p_223130_.getBoundingBox().isInside(p_223137_);
            p_223135_.fillStartsForStructure(structure, entry.getValue(), p_223220_ -> {
               if (mutableboolean.isFalse() && predicate.test(p_223220_)) {
                  mutableboolean.setTrue();
               }
            });
            if (mutableboolean.isTrue()) {
               return structurespawnoverride.spawns();
            }
         }
      }

      return p_223134_.value().getMobSettings().getMobs(p_223136_);
   }

   public void createStructures(
      RegistryAccess p_255835_,
      ChunkGeneratorStructureState p_256505_,
      StructureManager p_255934_,
      ChunkAccess p_255767_,
      StructureTemplateManager p_255832_,
      ResourceKey<Level> p_377248_
   ) {
      if (!SharedConstants.DEBUG_DISABLE_STRUCTURES) {
         ChunkPos chunkpos = p_255767_.getPos();
         SectionPos sectionpos = SectionPos.bottomOf(p_255767_);
         RandomState randomstate = p_256505_.randomState();
         p_256505_.possibleStructureSets()
            .forEach(
               p_255564_ -> {
                  StructurePlacement structureplacement = p_255564_.value().placement();
                  List<StructureSet.StructureSelectionEntry> list = p_255564_.value().structures();

                  for (StructureSet.StructureSelectionEntry structureset$structureselectionentry : list) {
                     StructureStart structurestart = p_255934_.getStartForStructure(
                        sectionpos, structureset$structureselectionentry.structure().value(), p_255767_
                     );
                     if (structurestart != null && structurestart.isValid()) {
                        return;
                     }
                  }

                  if (structureplacement.isStructureChunk(p_256505_, chunkpos.x, chunkpos.z)) {
                     if (list.size() == 1) {
                        this.tryGenerateStructure(
                           list.get(0), p_255934_, p_255835_, randomstate, p_255832_, p_256505_.getLevelSeed(), p_255767_, chunkpos, sectionpos, p_377248_
                        );
                     } else {
                        ArrayList<StructureSet.StructureSelectionEntry> arraylist = new ArrayList<>(list.size());
                        arraylist.addAll(list);
                        WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
                        worldgenrandom.setLargeFeatureSeed(p_256505_.getLevelSeed(), chunkpos.x, chunkpos.z);
                        int i = 0;

                        for (StructureSet.StructureSelectionEntry structureset$structureselectionentry1 : arraylist) {
                           i += structureset$structureselectionentry1.weight();
                        }

                        while (!arraylist.isEmpty()) {
                           int j = worldgenrandom.nextInt(i);
                           int k = 0;

                           for (StructureSet.StructureSelectionEntry structureset$structureselectionentry2 : arraylist) {
                              j -= structureset$structureselectionentry2.weight();
                              if (j < 0) {
                                 break;
                              }

                              k++;
                           }

                           StructureSet.StructureSelectionEntry structureset$structureselectionentry3 = arraylist.get(k);
                           if (this.tryGenerateStructure(
                              structureset$structureselectionentry3,
                              p_255934_,
                              p_255835_,
                              randomstate,
                              p_255832_,
                              p_256505_.getLevelSeed(),
                              p_255767_,
                              chunkpos,
                              sectionpos,
                              p_377248_
                           )) {
                              return;
                           }

                           arraylist.remove(k);
                           i -= structureset$structureselectionentry3.weight();
                        }
                     }
                  }
               }
            );
      }
   }

   private boolean tryGenerateStructure(
      StructureSet.StructureSelectionEntry p_223105_,
      StructureManager p_223106_,
      RegistryAccess p_223107_,
      RandomState p_223108_,
      StructureTemplateManager p_223109_,
      long p_223110_,
      ChunkAccess p_223111_,
      ChunkPos p_223112_,
      SectionPos p_223113_,
      ResourceKey<Level> p_376331_
   ) {
      Structure structure = p_223105_.structure().value();
      int i = fetchReferences(p_223106_, p_223111_, p_223113_, structure);
      HolderSet<Biome> holderset = structure.biomes();
      Predicate<Holder<Biome>> predicate = holderset::contains;
      StructureStart structurestart = structure.generate(
         p_223105_.structure(), p_376331_, p_223107_, this, this.biomeSource, p_223108_, p_223109_, p_223110_, p_223112_, i, p_223111_, predicate
      );
      if (structurestart.isValid()) {
         p_223106_.setStartForStructure(p_223113_, structure, structurestart, p_223111_);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(StructureManager p_223055_, ChunkAccess p_223056_, SectionPos p_223057_, Structure p_223058_) {
      StructureStart structurestart = p_223055_.getStartForStructure(p_223057_, p_223058_, p_223056_);
      return structurestart != null ? structurestart.getReferences() : 0;
   }

   public void createReferences(WorldGenLevel p_223077_, StructureManager p_223078_, ChunkAccess p_223079_) {
      int i = 8;
      ChunkPos chunkpos = p_223079_.getPos();
      int j = chunkpos.x;
      int k = chunkpos.z;
      int l = chunkpos.getMinBlockX();
      int i1 = chunkpos.getMinBlockZ();
      SectionPos sectionpos = SectionPos.bottomOf(p_223079_);

      for (int j1 = j - 8; j1 <= j + 8; j1++) {
         for (int k1 = k - 8; k1 <= k + 8; k1++) {
            long l1 = ChunkPos.asLong(j1, k1);

            for (StructureStart structurestart : p_223077_.getChunk(j1, k1).getAllStarts().values()) {
               try {
                  if (structurestart.isValid() && structurestart.getBoundingBox().intersects(l, i1, l + 15, i1 + 15)) {
                     p_223078_.addReferenceForStructure(sectionpos, structurestart.getStructure(), l1, p_223079_);
                  }
               } catch (Exception exception) {
                  CrashReport crashreport = CrashReport.forThrowable(exception, "Generating structure reference");
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Structure");
                  Optional<? extends Registry<Structure>> optional = p_223077_.registryAccess().lookup(Registries.STRUCTURE);
                  crashreportcategory.setDetail(
                     "Id", () -> optional.<String>map(p_449966_ -> p_449966_.getKey(structurestart.getStructure()).toString()).orElse("UNKNOWN")
                  );
                  crashreportcategory.setDetail("Name", () -> BuiltInRegistries.STRUCTURE_TYPE.getKey(structurestart.getStructure().type()).toString());
                  crashreportcategory.setDetail("Class", () -> structurestart.getStructure().getClass().getCanonicalName());
                  throw new ReportedException(crashreport);
               }
            }
         }
      }
   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Blender var1, RandomState var2, StructureManager var3, ChunkAccess var4);

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4, RandomState var5);

   public abstract NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3, RandomState var4);

   public int getFirstFreeHeight(int p_223222_, int p_223223_, Heightmap.Types p_223224_, LevelHeightAccessor p_223225_, RandomState p_223226_) {
      return this.getBaseHeight(p_223222_, p_223223_, p_223224_, p_223225_, p_223226_);
   }

   public int getFirstOccupiedHeight(int p_223236_, int p_223237_, Heightmap.Types p_223238_, LevelHeightAccessor p_223239_, RandomState p_223240_) {
      return this.getBaseHeight(p_223236_, p_223237_, p_223238_, p_223239_, p_223240_) - 1;
   }

   public abstract void addDebugScreenInfo(List<String> var1, RandomState var2, BlockPos var3);

   @Deprecated
   public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> p_223132_) {
      return this.generationSettingsGetter.apply(p_223132_);
   }
}
