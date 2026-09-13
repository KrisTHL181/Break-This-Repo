package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
   private final PositionalRandomFactory random;
   private final HolderGetter<NormalNoise.NoiseParameters> noises;
   private final NoiseRouter router;
   private final Climate.Sampler sampler;
   private final SurfaceSystem surfaceSystem;
   private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
   private final Map<Identifier, PositionalRandomFactory> positionalRandoms;
   private final List<SpawnTargetPoint.Wired> spawnTarget;

   public static RandomState create(final HolderGetter<NormalNoise.NoiseParameters> noises, final long seed, final NoiseGeneratorSettings settings) {
      return create(
         noises, seed, settings.useLegacyRandomSource(), settings.defaultBlock(), settings.seaLevel(), settings.noiseRouter(), settings.spawnTarget()
      );
   }

   public static RandomState create(
      final HolderGetter<NormalNoise.NoiseParameters> noises,
      final long seed,
      final boolean useLegacyRandom,
      final BlockState defaultBlock,
      final int seaLevel,
      final NoiseRouter noiseRouter,
      final List<SpawnTargetPoint> spawnTarget
   ) {
      return new RandomState(noises, seed, useLegacyRandom, defaultBlock, seaLevel, noiseRouter, spawnTarget);
   }

   private RandomState(
      final HolderGetter<NormalNoise.NoiseParameters> noises,
      final long seed,
      final boolean useLegacyRandom,
      final BlockState defaultBlock,
      final int seaLevel,
      final NoiseRouter noiseRouter,
      final List<SpawnTargetPoint> spawnTarget
   ) {
      WorldgenRandom.Algorithm randomAlgorithm = useLegacyRandom ? WorldgenRandom.Algorithm.LEGACY : WorldgenRandom.Algorithm.XOROSHIRO;
      this.random = randomAlgorithm.newInstance(seed).forkPositional();
      this.noises = noises;
      this.noiseIntances = new ConcurrentHashMap<>();
      this.positionalRandoms = new ConcurrentHashMap<>();
      this.surfaceSystem = new SurfaceSystem(this, defaultBlock, seaLevel, this.random);

      class NoiseWiringHelper implements DensityFunction.Visitor {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private RandomSource newLegacyInstance(final long seedOffset) {
            return new LegacyRandomSource(seed + seedOffset);
         }

         @Override
         public DensityFunction.NoiseHolder visitNoise(final DensityFunction.NoiseHolder noise) {
            Holder<NormalNoise.NoiseParameters> noiseData = noise.noiseData();
            if (noiseData.is(Noises.TEMPERATURE_NETHER)) {
               NormalNoise newNoise = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), noiseData.value());
               return new DensityFunction.NoiseHolder(noiseData, newNoise);
            } else if (noiseData.is(Noises.VEGETATION_NETHER)) {
               NormalNoise newNoise = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), noiseData.value());
               return new DensityFunction.NoiseHolder(noiseData, newNoise);
            } else {
               NormalNoise instantiate = RandomState.this.getOrCreateNoise(noiseData.unwrapKey().orElseThrow());
               return new DensityFunction.NoiseHolder(noiseData, instantiate);
            }
         }

         private DensityFunction wrapNew(final DensityFunction function) {
            if (function instanceof BlendedNoise noise) {
               RandomSource terrainRandom = useLegacyRandom
                  ? this.newLegacyInstance(0L)
                  : RandomState.this.random.fromHashOf(Identifier.withDefaultNamespace("terrain"));
               return noise.withNewRandom(terrainRandom);
            } else {
               return function instanceof DensityFunctions.EndIslandDensityFunction ? new DensityFunctions.EndIslandDensityFunction(seed) : function;
            }
         }

         @Override
         public DensityFunction apply(final DensityFunction function) {
            return this.wrapped.computeIfAbsent(function, this::wrapNew);
         }
      }

      NoiseWiringHelper noiseWirer = new NoiseWiringHelper();
      this.router = noiseRouter.mapAll(noiseWirer);
      DensityFunction.Visitor noiseFlattener = new DensityFunction.Visitor() {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private DensityFunction wrapNew(final DensityFunction function) {
            if (function instanceof DensityFunctions.HolderHolder holder) {
               return holder.function().value();
            } else {
               return function instanceof DensityFunctions.Marker marker ? marker.wrapped() : function;
            }
         }

         @Override
         public DensityFunction apply(final DensityFunction input) {
            return this.wrapped.computeIfAbsent(input, this::wrapNew);
         }
      };
      this.sampler = new Climate.Sampler(
         this.router.temperature().mapAll(noiseFlattener),
         this.router.vegetation().mapAll(noiseFlattener),
         this.router.continents().mapAll(noiseFlattener),
         this.router.erosion().mapAll(noiseFlattener),
         this.router.depth().mapAll(noiseFlattener),
         this.router.ridges().mapAll(noiseFlattener)
      );
      this.spawnTarget = spawnTarget.stream().map(point -> point.wire(noiseWirer, noiseFlattener)).toList();
   }

   public NormalNoise getOrCreateNoise(final ResourceKey<NormalNoise.NoiseParameters> noise) {
      return this.noiseIntances.computeIfAbsent(noise, key -> Noises.instantiate(this.noises, this.random, noise));
   }

   public PositionalRandomFactory getOrCreateRandomFactory(final Identifier name) {
      return this.positionalRandoms.computeIfAbsent(name, key -> this.random.fromHashOf(name).forkPositional());
   }

   public NoiseRouter router() {
      return this.router;
   }

   public Climate.Sampler sampler() {
      return this.sampler;
   }

   public SurfaceSystem surfaceSystem() {
      return this.surfaceSystem;
   }

   public List<SpawnTargetPoint.Wired> spawnTarget() {
      return this.spawnTarget;
   }
}
