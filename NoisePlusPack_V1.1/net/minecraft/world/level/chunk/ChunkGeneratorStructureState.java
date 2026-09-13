package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkGeneratorStructureState {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RandomState randomState;
   private final BiomeSource biomeSource;
   private final long levelSeed;
   private final long concentricRingsSeed;
   private final Map<Structure, List<StructurePlacement>> placementsForStructure = new Object2ObjectOpenHashMap();
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap();
   private boolean hasGeneratedPositions;
   private final List<Holder<StructureSet>> possibleStructureSets;

   public static ChunkGeneratorStructureState createForFlat(
      RandomState p_256240_, long p_256404_, BiomeSource p_256274_, Stream<Holder<StructureSet>> p_256348_
   ) {
      List<Holder<StructureSet>> list = p_256348_.filter(p_255616_ -> hasBiomesForStructureSet(p_255616_.value(), p_256274_)).toList();
      return new ChunkGeneratorStructureState(p_256240_, p_256274_, p_256404_, 0L, list);
   }

   public static ChunkGeneratorStructureState createForNormal(
      RandomState p_256197_, long p_255806_, BiomeSource p_256653_, HolderLookup<StructureSet> p_256659_
   ) {
      List<Holder<StructureSet>> list = p_256659_.listElements()
         .filter(p_256144_ -> hasBiomesForStructureSet(p_256144_.value(), p_256653_))
         .collect(Collectors.toUnmodifiableList());
      return new ChunkGeneratorStructureState(p_256197_, p_256653_, p_255806_, p_255806_, list);
   }

   private static boolean hasBiomesForStructureSet(StructureSet p_255766_, BiomeSource p_256424_) {
      Stream<Holder<Biome>> stream = p_255766_.structures().stream().flatMap(p_255738_ -> {
         Structure structure = p_255738_.structure().value();
         return structure.biomes().stream();
      });
      return stream.anyMatch(p_256424_.possibleBiomes()::contains);
   }

   private ChunkGeneratorStructureState(RandomState p_256401_, BiomeSource p_255742_, long p_256615_, long p_255979_, List<Holder<StructureSet>> p_256237_) {
      this.randomState = p_256401_;
      this.levelSeed = p_256615_;
      this.biomeSource = p_255742_;
      this.concentricRingsSeed = p_255979_;
      this.possibleStructureSets = p_256237_;
   }

   public List<Holder<StructureSet>> possibleStructureSets() {
      return this.possibleStructureSets;
   }

   private void generatePositions() {
      Set<Holder<Biome>> set = this.biomeSource.possibleBiomes();
      this.possibleStructureSets()
         .forEach(
            p_255638_ -> {
               StructureSet structureset = p_255638_.value();
               boolean flag = false;

               for (StructureSet.StructureSelectionEntry structureset$structureselectionentry : structureset.structures()) {
                  Structure structure = structureset$structureselectionentry.structure().value();
                  if (structure.biomes().stream().anyMatch(set::contains)) {
                     this.placementsForStructure.computeIfAbsent(structure, p_256235_ -> new ArrayList<>()).add(structureset.placement());
                     flag = true;
                  }
               }

               if (flag && structureset.placement() instanceof ConcentricRingsStructurePlacement concentricringsstructureplacement) {
                  this.ringPositions
                     .put(concentricringsstructureplacement, this.generateRingPositions((Holder<StructureSet>)p_255638_, concentricringsstructureplacement));
               }
            }
         );
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> p_255966_, ConcentricRingsStructurePlacement p_255744_) {
      if (p_255744_.count() == 0) {
         return CompletableFuture.completedFuture(List.of());
      }

      Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
      int i = p_255744_.distance();
      int j = p_255744_.count();
      List<CompletableFuture<ChunkPos>> list = new ArrayList<>(j);
      int k = p_255744_.spread();
      HolderSet<Biome> holderset = p_255744_.preferredBiomes();
      RandomSource randomsource = RandomSource.create();
      randomsource.setSeed(this.concentricRingsSeed);
      double d0 = randomsource.nextDouble() * Math.PI * 2.0;
      int l = 0;
      int i1 = 0;

      for (int j1 = 0; j1 < j; j1++) {
         double d1 = 4 * i + i * i1 * 6 + (randomsource.nextDouble() - 0.5) * (i * 2.5);
         int k1 = (int)Math.round(Math.cos(d0) * d1);
         int l1 = (int)Math.round(Math.sin(d0) * d1);
         RandomSource randomsource1 = randomsource.fork();
         list.add(
            CompletableFuture.supplyAsync(
               () -> {
                  Pair<BlockPos, Holder<Biome>> pair = this.biomeSource
                     .findBiomeHorizontal(
                        SectionPos.sectionToBlockCoord(k1, 8),
                        0,
                        SectionPos.sectionToBlockCoord(l1, 8),
                        112,
                        holderset::contains,
                        randomsource1,
                        this.randomState.sampler()
                     );
                  if (pair != null) {
                     BlockPos blockpos = (BlockPos)pair.getFirst();
                     return new ChunkPos(SectionPos.blockToSectionCoord(blockpos.getX()), SectionPos.blockToSectionCoord(blockpos.getZ()));
                  } else {
                     return new ChunkPos(k1, l1);
                  }
               },
               Util.backgroundExecutor().forName("structureRings")
            )
         );
         d0 += (Math.PI * 2) / k;
         if (++l == k) {
            i1++;
            l = 0;
            k += 2 * k / (i1 + 1);
            k = Math.min(k, j - j1);
            d0 += randomsource.nextDouble() * Math.PI * 2.0;
         }
      }

      return Util.sequence(list).thenApply(p_256372_ -> {
         double d2 = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0;
         LOGGER.debug("Calculation for {} took {}s", p_255966_, d2);
         return p_256372_;
      });
   }

   public void ensureStructuresGenerated() {
      if (!this.hasGeneratedPositions) {
         this.generatePositions();
         this.hasGeneratedPositions = true;
      }
   }

   public @Nullable List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement p_256667_) {
      this.ensureStructuresGenerated();
      CompletableFuture<List<ChunkPos>> completablefuture = this.ringPositions.get(p_256667_);
      return completablefuture != null ? completablefuture.join() : null;
   }

   public List<StructurePlacement> getPlacementsForStructure(Holder<Structure> p_256494_) {
      this.ensureStructuresGenerated();
      return this.placementsForStructure.getOrDefault(p_256494_.value(), List.of());
   }

   public RandomState randomState() {
      return this.randomState;
   }

   public boolean hasStructureChunkInRange(Holder<StructureSet> p_256489_, int p_256593_, int p_256115_, int p_256619_) {
      StructurePlacement structureplacement = p_256489_.value().placement();

      for (int i = p_256593_ - p_256619_; i <= p_256593_ + p_256619_; i++) {
         for (int j = p_256115_ - p_256619_; j <= p_256115_ + p_256619_; j++) {
            if (structureplacement.isStructureChunk(this, i, j)) {
               return true;
            }
         }
      }

      return false;
   }

   public long getLevelSeed() {
      return this.levelSeed;
   }
}
