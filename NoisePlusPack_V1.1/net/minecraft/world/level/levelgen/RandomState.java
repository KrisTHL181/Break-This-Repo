package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
   final PositionalRandomFactory random;
   private final HolderGetter<NormalNoise.NoiseParameters> noises;
   private final NoiseRouter router;
   private final Climate.Sampler sampler;
   private final SurfaceSystem surfaceSystem;
   private final PositionalRandomFactory aquiferRandom;
   private final PositionalRandomFactory oreRandom;
   private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
   private final Map<Identifier, PositionalRandomFactory> positionalRandoms;

   public static RandomState create(HolderGetter.Provider p_255935_, ResourceKey<NoiseGeneratorSettings> p_256314_, long p_256595_) {
      return create(p_255935_.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(p_256314_).value(), p_255935_.lookupOrThrow(Registries.NOISE), p_256595_);
   }

   public static RandomState create(NoiseGeneratorSettings p_255761_, HolderGetter<NormalNoise.NoiseParameters> p_256649_, long p_255965_) {
      return new RandomState(p_255761_, p_256649_, p_255965_);
   }

   private RandomState(NoiseGeneratorSettings p_255668_, HolderGetter<NormalNoise.NoiseParameters> p_256663_, final long p_255691_) {
      this.random = p_255668_.getRandomSource().newInstance(p_255691_).forkPositional();
      this.noises = p_256663_;
      this.aquiferRandom = this.random.fromHashOf(Identifier.withDefaultNamespace("aquifer")).forkPositional();
      this.oreRandom = this.random.fromHashOf(Identifier.withDefaultNamespace("ore")).forkPositional();
      this.noiseIntances = new ConcurrentHashMap<>();
      this.positionalRandoms = new ConcurrentHashMap<>();
      this.surfaceSystem = new SurfaceSystem(this, p_255668_.defaultBlock(), p_255668_.seaLevel(), this.random);
      final boolean flag = p_255668_.useLegacyRandomSource();

      class NoiseWiringHelper implements DensityFunction.Visitor {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private RandomSource newLegacyInstance(long p_224592_) {
            return new LegacyRandomSource(p_255691_ + p_224592_);
         }

         @Override
         public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder p_224594_) {
            Holder<NormalNoise.NoiseParameters> holder = p_224594_.noiseData();
            if (flag) {
               if (holder.is(Noises.TEMPERATURE)) {
                  NormalNoise normalnoise3 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), new NormalNoise.NoiseParameters(-7, 1.0, 1.0));
                  return new DensityFunction.NoiseHolder(holder, normalnoise3);
               }

               if (holder.is(Noises.VEGETATION)) {
                  NormalNoise normalnoise2 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), new NormalNoise.NoiseParameters(-7, 1.0, 1.0));
                  return new DensityFunction.NoiseHolder(holder, normalnoise2);
               }

               if (holder.is(Noises.SHIFT)) {
                  NormalNoise normalnoise1 = NormalNoise.create(
                     RandomState.this.random.fromHashOf(Noises.SHIFT.identifier()), new NormalNoise.NoiseParameters(0, 0.0)
                  );
                  return new DensityFunction.NoiseHolder(holder, normalnoise1);
               }
            }

            NormalNoise normalnoise = RandomState.this.getOrCreateNoise(holder.unwrapKey().orElseThrow());
            return new DensityFunction.NoiseHolder(holder, normalnoise);
         }

         private DensityFunction wrapNew(DensityFunction p_224596_) {
            if (p_224596_ instanceof BlendedNoise blendednoise) {
               RandomSource randomsource = flag ? this.newLegacyInstance(0L) : RandomState.this.random.fromHashOf(Identifier.withDefaultNamespace("terrain"));
               return blendednoise.withNewRandom(randomsource);
            } else {
               return p_224596_ instanceof DensityFunctions.EndIslandDensityFunction ? new DensityFunctions.EndIslandDensityFunction(p_255691_) : p_224596_;
            }
         }

         @Override
         public DensityFunction apply(DensityFunction p_224598_) {
            return this.wrapped.computeIfAbsent(p_224598_, this::wrapNew);
         }
      }

      this.router = p_255668_.noiseRouter().mapAll(new NoiseWiringHelper());
      DensityFunction.Visitor densityfunction$visitor = new DensityFunction.Visitor() {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private DensityFunction wrapNew(DensityFunction p_249732_) {
            if (p_249732_ instanceof DensityFunctions.HolderHolder densityfunctions$holderholder) {
               return densityfunctions$holderholder.function().value();
            } else {
               return p_249732_ instanceof DensityFunctions.Marker densityfunctions$marker ? densityfunctions$marker.wrapped() : p_249732_;
            }
         }

         @Override
         public DensityFunction apply(DensityFunction p_248616_) {
            return this.wrapped.computeIfAbsent(p_248616_, this::wrapNew);
         }
      };
      this.sampler = new Climate.Sampler(
         this.router.temperature().mapAll(densityfunction$visitor),
         this.router.vegetation().mapAll(densityfunction$visitor),
         this.router.continents().mapAll(densityfunction$visitor),
         this.router.erosion().mapAll(densityfunction$visitor),
         this.router.depth().mapAll(densityfunction$visitor),
         this.router.ridges().mapAll(densityfunction$visitor),
         p_255668_.spawnTarget()
      );
   }

   public NormalNoise getOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> p_224561_) {
      return this.noiseIntances.computeIfAbsent(p_224561_, p_255589_ -> Noises.instantiate(this.noises, this.random, p_224561_));
   }

   public PositionalRandomFactory getOrCreateRandomFactory(Identifier p_453754_) {
      return this.positionalRandoms.computeIfAbsent(p_453754_, p_449974_ -> this.random.fromHashOf(p_453754_).forkPositional());
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

   public PositionalRandomFactory aquiferRandom() {
      return this.aquiferRandom;
   }

   public PositionalRandomFactory oreRandom() {
      return this.oreRandom;
   }
}
