package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final MapCodec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings)
         )
         .apply(i, i.stable(NoiseBasedChunkGenerator::new))
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final Holder<NoiseGeneratorSettings> settings;
   private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

   public NoiseBasedChunkGenerator(final BiomeSource biomeSource, final Holder<NoiseGeneratorSettings> settings) {
      super(biomeSource);
      this.settings = settings;
      this.globalFluidPicker = Suppliers.memoize(() -> createFluidPicker(settings.value()));
   }

   private static Aquifer.FluidPicker createFluidPicker(final NoiseGeneratorSettings settings) {
      Aquifer.FluidStatus lavaStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int seaLevel = settings.seaLevel();
      Aquifer.FluidStatus seaStatus = new Aquifer.FluidStatus(seaLevel, settings.defaultFluid());
      Aquifer.FluidStatus emptyStatus = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
      return (x, y, z) -> {
         if (SharedConstants.DEBUG_DISABLE_FLUID_GENERATION) {
            return emptyStatus;
         } else {
            return y < Math.min(-54, seaLevel) ? lavaStatus : seaStatus;
         }
      };
   }

   @Override
   public CompletableFuture<ChunkAccess> createBiomes(
      final RandomState randomState, final Blender blender, final StructureManager structureManager, final ChunkAccess protoChunk
   ) {
      return CompletableFuture.supplyAsync(() -> {
         this.doCreateBiomes(blender, randomState, structureManager, protoChunk);
         return protoChunk;
      }, Util.backgroundExecutor().forName("init_biomes"));
   }

   private void doCreateBiomes(final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess protoChunk) {
      NoiseChunk noiseChunk = protoChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
      BiomeResolver biomeResolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.biomeSource), protoChunk);
      protoChunk.fillBiomesFromNoise(biomeResolver, noiseChunk.cachedClimateSampler(randomState.router()));
   }

   private NoiseChunk createNoiseChunk(final ChunkAccess chunk, final StructureManager structureManager, final Blender blender, final RandomState randomState) {
      return NoiseChunk.forChunk(
         chunk, randomState, Beardifier.forStructuresInChunk(structureManager, chunk.getPos()), this.settings.value(), this.globalFluidPicker.get(), blender
      );
   }

   @Override
   public ChunkPos getOrigin(final RandomState randomState) {
      List<SpawnTargetPoint.Wired> spawnTarget = randomState.spawnTarget();
      return spawnTarget.isEmpty() ? super.getOrigin(randomState) : ChunkPos.containing(NoiseSpawnFinder.findSpawnPosition(spawnTarget));
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   public boolean stable(final ResourceKey<NoiseGeneratorSettings> expectedPreset) {
      return this.settings.is(expectedPreset);
   }

   @Override
   public int getBaseHeight(final int x, final int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
      return this.iterateNoiseColumn(heightAccessor, randomState, x, z, null, type.isOpaque()).orElse(heightAccessor.getMinY());
   }

   @Override
   public NoiseColumn getBaseColumn(final int x, final int z, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
      MutableObject<NoiseColumn> result = new MutableObject();
      this.iterateNoiseColumn(heightAccessor, randomState, x, z, result, null);
      return (NoiseColumn)result.get();
   }

   @VisibleForTesting
   public double getInterpolatedNoiseValue(final RandomState randomState, final DensityFunction.FunctionContext context) {
      NoiseSettings noiseSettings = this.settings.value().noiseSettings();
      int cellWidth = noiseSettings.getCellWidth();
      int cellHeight = noiseSettings.getCellHeight();
      int minY = noiseSettings.minY();
      int blockX = context.blockX();
      int blockY = context.blockY();
      int blockZ = context.blockZ();
      if (blockY >= minY && blockY < minY + noiseSettings.height()) {
         NoiseChunk noiseChunk = new NoiseChunk(
            1,
            randomState,
            blockX - Math.floorMod(blockX, cellWidth),
            blockZ - Math.floorMod(blockZ, cellWidth),
            noiseSettings,
            DensityFunctions.BeardifierMarker.INSTANCE,
            this.settings.value(),
            this.globalFluidPicker.get(),
            Blender.empty()
         );
         noiseChunk.initializeForFirstCellX();
         noiseChunk.advanceCellX(0);
         noiseChunk.selectCellYZ(Math.floorDiv(blockY - minY, cellHeight), 0);
         noiseChunk.updateForY(blockY, (double)Math.floorMod(blockY - minY, cellHeight) / cellHeight);
         noiseChunk.updateForX(blockX, (double)Math.floorMod(blockX, cellWidth) / cellWidth);
         noiseChunk.updateForZ(blockZ, (double)Math.floorMod(blockZ, cellWidth) / cellWidth);
         return noiseChunk.getInterpolatedDensity();
      } else {
         return Double.NaN;
      }
   }

   @Override
   public void addDebugScreenInfo(final List<String> result, final RandomState randomState, final BlockPos feetPos) {
      DecimalFormat format = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ROOT));
      NoiseRouter router = randomState.router();
      DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(feetPos.getX(), feetPos.getY(), feetPos.getZ());
      double weirdness = router.ridges().compute(context);
      result.add(
         "NoiseRouter N: "
            + format.format(this.getInterpolatedNoiseValue(randomState, context))
            + " T: "
            + format.format(router.temperature().compute(context))
            + " V: "
            + format.format(router.vegetation().compute(context))
            + " C: "
            + format.format(router.continents().compute(context))
            + " E: "
            + format.format(router.erosion().compute(context))
            + " D: "
            + format.format(router.depth().compute(context))
            + " W: "
            + format.format(weirdness)
            + " PV: "
            + format.format(NoiseRouterData.peaksAndValleys((float)weirdness))
            + " PS: "
            + format.format(router.preliminarySurfaceLevel().compute(context))
      );
   }

   private OptionalInt iterateNoiseColumn(
      final LevelHeightAccessor heightAccessor,
      final RandomState randomState,
      final int blockX,
      final int blockZ,
      final @Nullable MutableObject<NoiseColumn> columnReference,
      final @Nullable Predicate<BlockState> tester
   ) {
      NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(heightAccessor);
      int cellHeight = noiseSettings.getCellHeight();
      int minY = noiseSettings.minY();
      int cellMinY = Mth.floorDiv(minY, cellHeight);
      int cellCountY = Mth.floorDiv(noiseSettings.height(), cellHeight);
      if (cellCountY <= 0) {
         return OptionalInt.empty();
      }

      BlockState[] writeTo;
      if (columnReference == null) {
         writeTo = null;
      } else {
         writeTo = new BlockState[noiseSettings.height()];
         columnReference.setValue(new NoiseColumn(minY, writeTo));
      }

      int cellWidth = noiseSettings.getCellWidth();
      int noiseChunkX = Math.floorDiv(blockX, cellWidth);
      int noiseChunkZ = Math.floorDiv(blockZ, cellWidth);
      int xInCell = Math.floorMod(blockX, cellWidth);
      int zInCell = Math.floorMod(blockZ, cellWidth);
      int firstBlockX = noiseChunkX * cellWidth;
      int firstBlockZ = noiseChunkZ * cellWidth;
      double factorX = (double)xInCell / cellWidth;
      double factorZ = (double)zInCell / cellWidth;
      NoiseChunk noiseChunk = new NoiseChunk(
         1,
         randomState,
         firstBlockX,
         firstBlockZ,
         noiseSettings,
         DensityFunctions.BeardifierMarker.INSTANCE,
         this.settings.value(),
         this.globalFluidPicker.get(),
         Blender.empty()
      );
      noiseChunk.initializeForFirstCellX();
      noiseChunk.advanceCellX(0);

      for (int cellYIndex = cellCountY - 1; cellYIndex >= 0; cellYIndex--) {
         noiseChunk.selectCellYZ(cellYIndex, 0);

         for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
            int posY = (cellMinY + cellYIndex) * cellHeight + yInCell;
            double factorY = (double)yInCell / cellHeight;
            noiseChunk.updateForY(posY, factorY);
            noiseChunk.updateForX(blockX, factorX);
            noiseChunk.updateForZ(blockZ, factorZ);
            BlockState baseState = noiseChunk.getInterpolatedState();
            BlockState state = baseState == null ? this.settings.value().defaultBlock() : baseState;
            if (writeTo != null) {
               int yIndex = cellYIndex * cellHeight + yInCell;
               writeTo[yIndex] = state;
            }

            if (tester != null && tester.test(state)) {
               noiseChunk.stopInterpolation();
               return OptionalInt.of(posY + 1);
            }
         }
      }

      noiseChunk.stopInterpolation();
      return OptionalInt.empty();
   }

   @Override
   public void buildSurface(final WorldGenRegion region, final StructureManager structureManager, final RandomState randomState, final ChunkAccess protoChunk) {
      if (!SharedConstants.debugVoidTerrain(protoChunk.getPos()) && !SharedConstants.DEBUG_DISABLE_SURFACE) {
         WorldGenerationContext context = new WorldGenerationContext(this, region);
         Set<Holder<Biome>> possibleBiomes = collectPossibleBiomes(region, 1);
         this.buildSurface(protoChunk, context, randomState, structureManager, region.getBiomeManager(), Blender.of(region), possibleBiomes);
      }
   }

   private static Set<Holder<Biome>> collectPossibleBiomes(final WorldGenRegion region, final int chunkRadius) {
      Set<Holder<Biome>> chunkBiomes = new ReferenceOpenHashSet();
      ChunkPos center = region.getCenter();

      for (int z = center.z() - chunkRadius; z <= center.z() + chunkRadius; z++) {
         for (int x = center.x() - chunkRadius; x <= center.x() + chunkRadius; x++) {
            region.getChunk(x, z).collectBiomesInPalette(chunkBiomes);
         }
      }

      return chunkBiomes;
   }

   @VisibleForTesting
   public void buildSurface(
      final ChunkAccess protoChunk,
      final WorldGenerationContext context,
      final RandomState randomState,
      final StructureManager structureManager,
      final BiomeManager biomeManager,
      final Blender blender,
      final @Nullable Set<Holder<Biome>> possibleBiomes
   ) {
      NoiseChunk noiseChunk = protoChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
      NoiseGeneratorSettings settings = this.settings.value();
      randomState.surfaceSystem().buildSurface(randomState, biomeManager, context, protoChunk, noiseChunk, settings.materialRule().value(), possibleBiomes);
   }

   @Override
   public void applyCarvers(
      final WorldGenRegion region,
      final long seed,
      final RandomState randomState,
      final BiomeManager biomeManager,
      final StructureManager structureManager,
      final ChunkAccess chunk,
      final CarvingMask.@Nullable Filter filter
   ) {
      if (!SharedConstants.DEBUG_DISABLE_CARVERS && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
         WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         int range = 8;
         ChunkPos pos = chunk.getPos();
         NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(c -> this.createNoiseChunk(c, structureManager, Blender.of(region), randomState));
         WorldGenerationContext context = new WorldGenerationContext(this, chunk.getHeightAccessorForGeneration());
         int protectedBlocksOnTop = chunk.isUpgrading() ? 0 : 7;
         int maxY = context.getMinGenY() + context.getGenDepth() - 1 - protectedBlocksOnTop;
         CarvingMask mask = new CarvingMask(context.getMinGenY() + 1, maxY);

         for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
               ChunkPos sourcePos = new ChunkPos(pos.x() + dx, pos.z() + dz);
               ChunkAccess carverCenterChunk = region.getChunk(sourcePos.x(), sourcePos.z());
               BiomeGenerationSettings sourceBiomeGenerationSettings = carverCenterChunk.carverBiome(
                  () -> this.getBiomeGenerationSettings(
                     this.biomeSource
                        .getNoiseBiome(QuartPos.fromBlock(sourcePos.getMinBlockX()), 0, QuartPos.fromBlock(sourcePos.getMinBlockZ()), randomState.sampler())
                  )
               );
               Iterable<Holder<WorldCarver>> carvers = sourceBiomeGenerationSettings.getCarvers();
               int index = 0;

               for (Holder<WorldCarver> carverHolder : carvers) {
                  WorldCarver carver = carverHolder.value();
                  random.setLargeFeatureSeed(seed + index, sourcePos.x(), sourcePos.z());
                  if (carver.isStartChunk(random)) {
                     carver.carve(context, random, chunk.getPos(), sourcePos, mask);
                  }

                  index++;
               }
            }
         }

         if (!mask.isEmpty()) {
            BiomeManager correctBiomeManager = biomeManager.withDifferentSource(
               (quartX, quartY, quartZ) -> this.biomeSource.getNoiseBiome(quartX, quartY, quartZ, randomState.sampler())
            );
            this.applyCarvingMask(chunk, mask, randomState, context, noiseChunk, correctBiomeManager::getBiome, filter);
         }
      }
   }

   private void applyCarvingMask(
      final ChunkAccess chunk,
      final CarvingMask mask,
      final RandomState randomState,
      final WorldGenerationContext context,
      final NoiseChunk noiseChunk,
      final Function<BlockPos, Holder<Biome>> biomeGetter,
      final CarvingMask.@Nullable Filter filter
   ) {
      ChunkPos chunkPos = chunk.getPos();
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos helperPos = new BlockPos.MutableBlockPos();
      Aquifer aquifer = noiseChunk.aquifer();
      SurfaceRules.RuleSource materialRule = this.settings.value().materialRule().value();
      mask.visit(
         (x, z, bottomY, topY) -> {
            boolean hasGrass = false;
            int worldX = chunkPos.getBlockX(x);
            int worldZ = chunkPos.getBlockZ(z);

            for (int worldY = topY; worldY >= bottomY; worldY--) {
               if (filter == null || filter.test(x, worldY, z)) {
                  blockPos.set(worldX, worldY, worldZ);
                  BlockState blockState = chunk.getBlockState(blockPos);
                  if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
                     hasGrass = true;
                  }

                  BlockState state = aquifer.computeSubstance(new DensityFunction.SinglePointContext(worldX, worldY, worldZ), 0.0);
                  if (state != null) {
                     chunk.setBlockState(blockPos, state);
                     if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                        chunk.markPosForPostProcessing(blockPos);
                     }

                     if (hasGrass) {
                        helperPos.setWithOffset(blockPos, Direction.DOWN);
                        if (chunk.getBlockState(helperPos).is(Blocks.DIRT)) {
                           randomState.surfaceSystem()
                              .topMaterial(materialRule, randomState, context, biomeGetter, chunk, noiseChunk, helperPos, !state.getFluidState().isEmpty())
                              .ifPresent(topMaterial -> {
                                 chunk.setBlockState(helperPos, topMaterial);
                                 if (!topMaterial.getFluidState().isEmpty()) {
                                    chunk.markPosForPostProcessing(helperPos);
                                 }
                              });
                        }
                     }
                  }
               }
            }
         }
      );
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(
      final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk
   ) {
      NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
      int minY = noiseSettings.minY();
      int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
      int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
      return cellCountY <= 0 ? CompletableFuture.completedFuture(centerChunk) : CompletableFuture.supplyAsync(() -> {
         int topSectionIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
         int bottomSectionIndex = centerChunk.getSectionIndex(minY);
         Set<LevelChunkSection> sections = Sets.newHashSet();

         for (int sectionIndex = topSectionIndex; sectionIndex >= bottomSectionIndex; sectionIndex--) {
            LevelChunkSection section = centerChunk.getSection(sectionIndex);
            section.acquire();
            sections.add(section);
         }

         try {
            return this.doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
         } finally {
            for (LevelChunkSection section : sections) {
               section.release();
            }
         }
      }, Util.backgroundExecutor().forName("wgen_fill_noise"));
   }

   private ChunkAccess doFill(
      final Blender blender,
      final StructureManager structureManager,
      final RandomState randomState,
      final ChunkAccess centerChunk,
      final int cellMinY,
      final int cellCountY
   ) {
      NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
      Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos chunkPos = centerChunk.getPos();
      int chunkStartBlockX = chunkPos.getMinBlockX();
      int chunkStartBlockZ = chunkPos.getMinBlockZ();
      Aquifer aquifer = noiseChunk.aquifer();
      noiseChunk.initializeForFirstCellX();
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
      int cellWidth = noiseChunk.cellWidth();
      int cellHeight = noiseChunk.cellHeight();
      int cellCountX = 16 / cellWidth;
      int cellCountZ = 16 / cellWidth;

      for (int cellXIndex = 0; cellXIndex < cellCountX; cellXIndex++) {
         noiseChunk.advanceCellX(cellXIndex);

         for (int cellZIndex = 0; cellZIndex < cellCountZ; cellZIndex++) {
            int lastSectionIndex = centerChunk.getSectionsCount() - 1;
            LevelChunkSection section = centerChunk.getSection(lastSectionIndex);

            for (int cellYIndex = cellCountY - 1; cellYIndex >= 0; cellYIndex--) {
               noiseChunk.selectCellYZ(cellYIndex, cellZIndex);

               for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                  int posY = (cellMinY + cellYIndex) * cellHeight + yInCell;
                  int yInSection = posY & 15;
                  int sectionIndex = centerChunk.getSectionIndex(posY);
                  if (lastSectionIndex != sectionIndex) {
                     lastSectionIndex = sectionIndex;
                     section = centerChunk.getSection(sectionIndex);
                  }

                  double factorY = (double)yInCell / cellHeight;
                  noiseChunk.updateForY(posY, factorY);

                  for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
                     int posX = chunkStartBlockX + cellXIndex * cellWidth + xInCell;
                     int xInSection = posX & 15;
                     double factorX = (double)xInCell / cellWidth;
                     noiseChunk.updateForX(posX, factorX);

                     for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
                        int posZ = chunkStartBlockZ + cellZIndex * cellWidth + zInCell;
                        int zInSection = posZ & 15;
                        double factorZ = (double)zInCell / cellWidth;
                        noiseChunk.updateForZ(posZ, factorZ);
                        BlockState state = noiseChunk.getInterpolatedState();
                        if (state == null) {
                           state = this.settings.value().defaultBlock();
                        }

                        state = this.debugPreliminarySurfaceLevel(noiseChunk, posX, posY, posZ, state);
                        if (state != AIR && !SharedConstants.debugVoidTerrain(centerChunk.getPos())) {
                           section.setBlockState(xInSection, yInSection, zInSection, state, false);
                           oceanFloor.update(xInSection, posY, zInSection, state);
                           worldSurface.update(xInSection, posY, zInSection, state);
                           if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                              blockPos.set(posX, posY, posZ);
                              centerChunk.markPosForPostProcessing(blockPos);
                           }
                        }
                     }
                  }
               }
            }
         }

         noiseChunk.swapSlices();
      }

      noiseChunk.stopInterpolation();
      return centerChunk;
   }

   private BlockState debugPreliminarySurfaceLevel(final NoiseChunk noiseChunk, final int posX, final int posY, final int posZ, BlockState state) {
      if (SharedConstants.DEBUG_AQUIFERS && posZ >= 0 && posZ % 4 == 0) {
         int preliminarySurfaceLevel = noiseChunk.preliminarySurfaceLevel(posX, posZ);
         int adjustedSurfaceLevel = preliminarySurfaceLevel + 8;
         if (posY == adjustedSurfaceLevel) {
            state = adjustedSurfaceLevel < this.getSeaLevel() ? Blocks.SLIME_BLOCK.defaultBlockState() : Blocks.HONEY_BLOCK.defaultBlockState();
         }
      }

      return state;
   }

   @Override
   public int getGenDepth() {
      return this.settings.value().noiseSettings().height();
   }

   @Override
   public int getSeaLevel() {
      return this.settings.value().seaLevel();
   }

   @Override
   public int getMinY() {
      return this.settings.value().noiseSettings().minY();
   }

   @Override
   public void spawnOriginalMobs(final WorldGenRegion worldGenRegion) {
      if (!this.settings.value().disableMobGeneration()) {
         ChunkPos center = worldGenRegion.getCenter();
         Holder<Biome> biome = worldGenRegion.getBiome(center.getWorldPosition().atY(worldGenRegion.getMaxY()));
         WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         random.setDecorationSeed(worldGenRegion.getSeed(), center.getMinBlockX(), center.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(worldGenRegion, biome, center, random);
      }
   }
}
