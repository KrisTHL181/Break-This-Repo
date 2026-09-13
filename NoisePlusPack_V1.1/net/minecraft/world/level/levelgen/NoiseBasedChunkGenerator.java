package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final MapCodec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_255585_ -> p_255585_.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(p_255584_ -> p_255584_.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(p_224278_ -> p_224278_.settings)
         )
         .apply(p_255585_, p_255585_.stable(NoiseBasedChunkGenerator::new))
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final Holder<NoiseGeneratorSettings> settings;
   private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

   public NoiseBasedChunkGenerator(BiomeSource p_256415_, Holder<NoiseGeneratorSettings> p_256182_) {
      super(p_256415_);
      this.settings = p_256182_;
      this.globalFluidPicker = Suppliers.memoize(() -> createFluidPicker(p_256182_.value()));
   }

   private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings p_249264_) {
      Aquifer.FluidStatus aquifer$fluidstatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int i = p_249264_.seaLevel();
      Aquifer.FluidStatus aquifer$fluidstatus1 = new Aquifer.FluidStatus(i, p_249264_.defaultFluid());
      Aquifer.FluidStatus aquifer$fluidstatus2 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
      return (p_422216_, p_422217_, p_422218_) -> {
         if (SharedConstants.DEBUG_DISABLE_FLUID_GENERATION) {
            return aquifer$fluidstatus2;
         } else {
            return p_422217_ < Math.min(-54, i) ? aquifer$fluidstatus : aquifer$fluidstatus1;
         }
      };
   }

   @Override
   public CompletableFuture<ChunkAccess> createBiomes(RandomState p_224299_, Blender p_224300_, StructureManager p_224301_, ChunkAccess p_224302_) {
      return CompletableFuture.supplyAsync(() -> {
         this.doCreateBiomes(p_224300_, p_224299_, p_224301_, p_224302_);
         return p_224302_;
      }, Util.backgroundExecutor().forName("init_biomes"));
   }

   private void doCreateBiomes(Blender p_224292_, RandomState p_224293_, StructureManager p_224294_, ChunkAccess p_224295_) {
      NoiseChunk noisechunk = p_224295_.getOrCreateNoiseChunk(p_224340_ -> this.createNoiseChunk(p_224340_, p_224294_, p_224292_, p_224293_));
      BiomeResolver biomeresolver = BelowZeroRetrogen.getBiomeResolver(p_224292_.getBiomeResolver(this.biomeSource), p_224295_);
      p_224295_.fillBiomesFromNoise(biomeresolver, noisechunk.cachedClimateSampler(p_224293_.router(), this.settings.value().spawnTarget()));
   }

   private NoiseChunk createNoiseChunk(ChunkAccess p_224257_, StructureManager p_224258_, Blender p_224259_, RandomState p_224260_) {
      return NoiseChunk.forChunk(
         p_224257_, p_224260_, Beardifier.forStructuresInChunk(p_224258_, p_224257_.getPos()), this.settings.value(), this.globalFluidPicker.get(), p_224259_
      );
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   public boolean stable(ResourceKey<NoiseGeneratorSettings> p_224222_) {
      return this.settings.is(p_224222_);
   }

   @Override
   public int getBaseHeight(int p_224216_, int p_224217_, Heightmap.Types p_224218_, LevelHeightAccessor p_224219_, RandomState p_224220_) {
      return this.iterateNoiseColumn(p_224219_, p_224220_, p_224216_, p_224217_, null, p_224218_.isOpaque()).orElse(p_224219_.getMinY());
   }

   @Override
   public NoiseColumn getBaseColumn(int p_224211_, int p_224212_, LevelHeightAccessor p_224213_, RandomState p_224214_) {
      MutableObject<NoiseColumn> mutableobject = new MutableObject();
      this.iterateNoiseColumn(p_224213_, p_224214_, p_224211_, p_224212_, mutableobject, null);
      return (NoiseColumn)mutableobject.get();
   }

   @Override
   public void addDebugScreenInfo(List<String> p_224304_, RandomState p_224305_, BlockPos p_224306_) {
      DecimalFormat decimalformat = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ROOT));
      NoiseRouter noiserouter = p_224305_.router();
      DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(
         p_224306_.getX(), p_224306_.getY(), p_224306_.getZ()
      );
      double d0 = noiserouter.ridges().compute(densityfunction$singlepointcontext);
      p_224304_.add(
         "NoiseRouter T: "
            + decimalformat.format(noiserouter.temperature().compute(densityfunction$singlepointcontext))
            + " V: "
            + decimalformat.format(noiserouter.vegetation().compute(densityfunction$singlepointcontext))
            + " C: "
            + decimalformat.format(noiserouter.continents().compute(densityfunction$singlepointcontext))
            + " E: "
            + decimalformat.format(noiserouter.erosion().compute(densityfunction$singlepointcontext))
            + " D: "
            + decimalformat.format(noiserouter.depth().compute(densityfunction$singlepointcontext))
            + " W: "
            + decimalformat.format(d0)
            + " PV: "
            + decimalformat.format(NoiseRouterData.peaksAndValleys((float)d0))
            + " PS: "
            + decimalformat.format(noiserouter.preliminarySurfaceLevel().compute(densityfunction$singlepointcontext))
            + " N: "
            + decimalformat.format(noiserouter.finalDensity().compute(densityfunction$singlepointcontext))
      );
   }

   private OptionalInt iterateNoiseColumn(
      LevelHeightAccessor p_224240_,
      RandomState p_224241_,
      int p_224242_,
      int p_224243_,
      @Nullable MutableObject<NoiseColumn> p_224244_,
      @Nullable Predicate<BlockState> p_224245_
   ) {
      NoiseSettings noisesettings = this.settings.value().noiseSettings().clampToHeightAccessor(p_224240_);
      int i = noisesettings.getCellHeight();
      int j = noisesettings.minY();
      int k = Mth.floorDiv(j, i);
      int l = Mth.floorDiv(noisesettings.height(), i);
      if (l <= 0) {
         return OptionalInt.empty();
      }

      BlockState[] ablockstate;
      if (p_224244_ == null) {
         ablockstate = null;
      } else {
         ablockstate = new BlockState[noisesettings.height()];
         p_224244_.setValue(new NoiseColumn(j, ablockstate));
      }

      int i1 = noisesettings.getCellWidth();
      int j1 = Math.floorDiv(p_224242_, i1);
      int k1 = Math.floorDiv(p_224243_, i1);
      int l1 = Math.floorMod(p_224242_, i1);
      int i2 = Math.floorMod(p_224243_, i1);
      int j2 = j1 * i1;
      int k2 = k1 * i1;
      double d0 = (double)l1 / i1;
      double d1 = (double)i2 / i1;
      NoiseChunk noisechunk = new NoiseChunk(
         1, p_224241_, j2, k2, noisesettings, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty()
      );
      noisechunk.initializeForFirstCellX();
      noisechunk.advanceCellX(0);

      for (int l2 = l - 1; l2 >= 0; l2--) {
         noisechunk.selectCellYZ(l2, 0);

         for (int i3 = i - 1; i3 >= 0; i3--) {
            int j3 = (k + l2) * i + i3;
            double d2 = (double)i3 / i;
            noisechunk.updateForY(j3, d2);
            noisechunk.updateForX(p_224242_, d0);
            noisechunk.updateForZ(p_224243_, d1);
            BlockState blockstate = noisechunk.getInterpolatedState();
            BlockState blockstate1 = blockstate == null ? this.settings.value().defaultBlock() : blockstate;
            if (ablockstate != null) {
               int k3 = l2 * i + i3;
               ablockstate[k3] = blockstate1;
            }

            if (p_224245_ != null && p_224245_.test(blockstate1)) {
               noisechunk.stopInterpolation();
               return OptionalInt.of(j3 + 1);
            }
         }
      }

      noisechunk.stopInterpolation();
      return OptionalInt.empty();
   }

   @Override
   public void buildSurface(WorldGenRegion p_224232_, StructureManager p_224233_, RandomState p_224234_, ChunkAccess p_224235_) {
      if (!SharedConstants.debugVoidTerrain(p_224235_.getPos()) && !SharedConstants.DEBUG_DISABLE_SURFACE) {
         WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(this, p_224232_);
         this.buildSurface(
            p_224235_,
            worldgenerationcontext,
            p_224234_,
            p_224233_,
            p_224232_.getBiomeManager(),
            p_224232_.registryAccess().lookupOrThrow(Registries.BIOME),
            Blender.of(p_224232_)
         );
      }
   }

   @VisibleForTesting
   public void buildSurface(
      ChunkAccess p_224262_,
      WorldGenerationContext p_224263_,
      RandomState p_224264_,
      StructureManager p_224265_,
      BiomeManager p_224266_,
      Registry<Biome> p_224267_,
      Blender p_224268_
   ) {
      NoiseChunk noisechunk = p_224262_.getOrCreateNoiseChunk(p_224321_ -> this.createNoiseChunk(p_224321_, p_224265_, p_224268_, p_224264_));
      NoiseGeneratorSettings noisegeneratorsettings = this.settings.value();
      p_224264_.surfaceSystem()
         .buildSurface(
            p_224264_,
            p_224266_,
            p_224267_,
            noisegeneratorsettings.useLegacyRandomSource(),
            p_224263_,
            p_224262_,
            noisechunk,
            noisegeneratorsettings.surfaceRule()
         );
   }

   @Override
   public void applyCarvers(
      WorldGenRegion p_224224_, long p_224225_, RandomState p_224226_, BiomeManager p_224227_, StructureManager p_224228_, ChunkAccess p_224229_
   ) {
      if (!SharedConstants.DEBUG_DISABLE_CARVERS) {
         BiomeManager biomemanager = p_224227_.withDifferentSource(
            (p_255581_, p_255582_, p_255583_) -> this.biomeSource.getNoiseBiome(p_255581_, p_255582_, p_255583_, p_224226_.sampler())
         );
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         int i = 8;
         ChunkPos chunkpos = p_224229_.getPos();
         NoiseChunk noisechunk = p_224229_.getOrCreateNoiseChunk(p_224250_ -> this.createNoiseChunk(p_224250_, p_224228_, Blender.of(p_224224_), p_224226_));
         Aquifer aquifer = noisechunk.aquifer();
         CarvingContext carvingcontext = new CarvingContext(
            this, p_224224_.registryAccess(), p_224229_.getHeightAccessorForGeneration(), noisechunk, p_224226_, this.settings.value().surfaceRule()
         );
         CarvingMask carvingmask = ((ProtoChunk)p_224229_).getOrCreateCarvingMask();

         for (int j = -8; j <= 8; j++) {
            for (int k = -8; k <= 8; k++) {
               ChunkPos chunkpos1 = new ChunkPos(chunkpos.x + j, chunkpos.z + k);
               ChunkAccess chunkaccess = p_224224_.getChunk(chunkpos1.x, chunkpos1.z);
               BiomeGenerationSettings biomegenerationsettings = chunkaccess.carverBiome(
                  () -> this.getBiomeGenerationSettings(
                     this.biomeSource
                        .getNoiseBiome(QuartPos.fromBlock(chunkpos1.getMinBlockX()), 0, QuartPos.fromBlock(chunkpos1.getMinBlockZ()), p_224226_.sampler())
                  )
               );
               Iterable<Holder<ConfiguredWorldCarver<?>>> iterable = biomegenerationsettings.getCarvers();
               int l = 0;

               for (Holder<ConfiguredWorldCarver<?>> holder : iterable) {
                  ConfiguredWorldCarver<?> configuredworldcarver = holder.value();
                  worldgenrandom.setLargeFeatureSeed(p_224225_ + l, chunkpos1.x, chunkpos1.z);
                  if (configuredworldcarver.isStartChunk(worldgenrandom)) {
                     configuredworldcarver.carve(carvingcontext, p_224229_, biomemanager::getBiome, worldgenrandom, aquifer, chunkpos1, carvingmask);
                  }

                  l++;
               }
            }
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Blender p_224313_, RandomState p_224314_, StructureManager p_224315_, ChunkAccess p_224316_) {
      NoiseSettings noisesettings = this.settings.value().noiseSettings().clampToHeightAccessor(p_224316_.getHeightAccessorForGeneration());
      int i = noisesettings.minY();
      int j = Mth.floorDiv(i, noisesettings.getCellHeight());
      int k = Mth.floorDiv(noisesettings.height(), noisesettings.getCellHeight());
      return k <= 0 ? CompletableFuture.completedFuture(p_224316_) : CompletableFuture.supplyAsync(() -> {
         int l = p_224316_.getSectionIndex(k * noisesettings.getCellHeight() - 1 + i);
         int i1 = p_224316_.getSectionIndex(i);
         Set<LevelChunkSection> set = Sets.newHashSet();

         for (int j1 = l; j1 >= i1; j1--) {
            LevelChunkSection levelchunksection = p_224316_.getSection(j1);
            levelchunksection.acquire();
            set.add(levelchunksection);
         }

         ChunkAccess chunkaccess;
         try {
            chunkaccess = this.doFill(p_224313_, p_224315_, p_224314_, p_224316_, j, k);
         } finally {
            for (LevelChunkSection levelchunksection1 : set) {
               levelchunksection1.release();
            }
         }

         return chunkaccess;
      }, Util.backgroundExecutor().forName("wgen_fill_noise"));
   }

   private ChunkAccess doFill(Blender p_224285_, StructureManager p_224286_, RandomState p_224287_, ChunkAccess p_224288_, int p_224289_, int p_224290_) {
      NoiseChunk noisechunk = p_224288_.getOrCreateNoiseChunk(p_224255_ -> this.createNoiseChunk(p_224255_, p_224286_, p_224285_, p_224287_));
      Heightmap heightmap = p_224288_.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap heightmap1 = p_224288_.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos chunkpos = p_224288_.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      Aquifer aquifer = noisechunk.aquifer();
      noisechunk.initializeForFirstCellX();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      int k = noisechunk.cellWidth();
      int l = noisechunk.cellHeight();
      int i1 = 16 / k;
      int j1 = 16 / k;

      for (int k1 = 0; k1 < i1; k1++) {
         noisechunk.advanceCellX(k1);

         for (int l1 = 0; l1 < j1; l1++) {
            int i2 = p_224288_.getSectionsCount() - 1;
            LevelChunkSection levelchunksection = p_224288_.getSection(i2);

            for (int j2 = p_224290_ - 1; j2 >= 0; j2--) {
               noisechunk.selectCellYZ(j2, l1);

               for (int k2 = l - 1; k2 >= 0; k2--) {
                  int l2 = (p_224289_ + j2) * l + k2;
                  int i3 = l2 & 15;
                  int j3 = p_224288_.getSectionIndex(l2);
                  if (i2 != j3) {
                     i2 = j3;
                     levelchunksection = p_224288_.getSection(j3);
                  }

                  double d0 = (double)k2 / l;
                  noisechunk.updateForY(l2, d0);

                  for (int k3 = 0; k3 < k; k3++) {
                     int l3 = i + k1 * k + k3;
                     int i4 = l3 & 15;
                     double d1 = (double)k3 / k;
                     noisechunk.updateForX(l3, d1);

                     for (int j4 = 0; j4 < k; j4++) {
                        int k4 = j + l1 * k + j4;
                        int l4 = k4 & 15;
                        double d2 = (double)j4 / k;
                        noisechunk.updateForZ(k4, d2);
                        BlockState blockstate = noisechunk.getInterpolatedState();
                        if (blockstate == null) {
                           blockstate = this.settings.value().defaultBlock();
                        }

                        blockstate = this.debugPreliminarySurfaceLevel(noisechunk, l3, l2, k4, blockstate);
                        if (blockstate != AIR && !SharedConstants.debugVoidTerrain(p_224288_.getPos())) {
                           levelchunksection.setBlockState(i4, i3, l4, blockstate, false);
                           heightmap.update(i4, l2, l4, blockstate);
                           heightmap1.update(i4, l2, l4, blockstate);
                           if (aquifer.shouldScheduleFluidUpdate() && !blockstate.getFluidState().isEmpty()) {
                              blockpos$mutableblockpos.set(l3, l2, k4);
                              p_224288_.markPosForPostprocessing(blockpos$mutableblockpos);
                           }
                        }
                     }
                  }
               }
            }
         }

         noisechunk.swapSlices();
      }

      noisechunk.stopInterpolation();
      return p_224288_;
   }

   private BlockState debugPreliminarySurfaceLevel(NoiseChunk p_198232_, int p_198233_, int p_198234_, int p_198235_, BlockState p_198236_) {
      if (SharedConstants.DEBUG_AQUIFERS && p_198235_ >= 0 && p_198235_ % 4 == 0) {
         int i = p_198232_.preliminarySurfaceLevel(p_198233_, p_198235_);
         int j = i + 8;
         if (p_198234_ == j) {
            p_198236_ = j < this.getSeaLevel() ? Blocks.SLIME_BLOCK.defaultBlockState() : Blocks.HONEY_BLOCK.defaultBlockState();
         }
      }

      return p_198236_;
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
   public void spawnOriginalMobs(WorldGenRegion p_64379_) {
      if (!this.settings.value().disableMobGeneration()) {
         ChunkPos chunkpos = p_64379_.getCenter();
         Holder<Biome> holder = p_64379_.getBiome(chunkpos.getWorldPosition().atY(p_64379_.getMaxY()));
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         worldgenrandom.setDecorationSeed(p_64379_.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(p_64379_, holder, chunkpos, worldgenrandom);
      }
   }
}
