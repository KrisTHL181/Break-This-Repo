package net.minecraft.world.level.levelgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
   public static final float GLOBAL_OFFSET = -0.50375F;
   private static final float ORE_THICKNESS = 0.08F;
   private static final double VEININESS_FREQUENCY = 1.5;
   private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5;
   private static final double SURFACE_DENSITY_THRESHOLD = 1.5625;
   private static final double CHEESE_NOISE_TARGET = -0.703125;
   public static final double NOISE_ZERO = 0.390625;
   public static final int ISLAND_CHUNK_DISTANCE = 64;
   public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
   private static final int DENSITY_Y_ANCHOR_BOTTOM = -64;
   private static final int DENSITY_Y_ANCHOR_TOP = 320;
   private static final double DENSITY_Y_BOTTOM = 1.5;
   private static final double DENSITY_Y_TOP = -1.5;
   private static final int OVERWORLD_BOTTOM_SLIDE_HEIGHT = 24;
   private static final double BASE_DENSITY_MULTIPLIER = 4.0;
   private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0);
   private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
   private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
   private static final ResourceKey<DensityFunction> Y = createKey("y");
   private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
   private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = createKey("overworld/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = createKey("nether/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = createKey("end/base_3d_noise");
   public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
   public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
   public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
   public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
   public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
   public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
   public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
   public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
   public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
   public static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
   private static final ResourceKey<DensityFunction> OFFSET_LARGE = createKey("overworld_large_biomes/offset");
   private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = createKey("overworld_large_biomes/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
   private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = createKey("overworld_amplified/offset");
   private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = createKey("overworld_amplified/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = createKey("overworld_amplified/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = createKey("overworld_amplified/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = createKey("overworld_amplified/sloped_cheese");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
   private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
   private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
   private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
   private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

   private static ResourceKey<DensityFunction> createKey(String p_209537_) {
      return ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(p_209537_));
   }

   public static Holder<? extends DensityFunction> bootstrap(BootstrapContext<DensityFunction> p_335193_) {
      HolderGetter<NormalNoise.NoiseParameters> holdergetter = p_335193_.lookup(Registries.NOISE);
      HolderGetter<DensityFunction> holdergetter1 = p_335193_.lookup(Registries.DENSITY_FUNCTION);
      p_335193_.register(ZERO, DensityFunctions.zero());
      int i = DimensionType.MIN_Y * 2;
      int j = DimensionType.MAX_Y * 2;
      p_335193_.register(Y, DensityFunctions.yClampedGradient(i, j, i, j));
      DensityFunction densityfunction = registerAndWrap(
         p_335193_, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(holdergetter.getOrThrow(Noises.SHIFT))))
      );
      DensityFunction densityfunction1 = registerAndWrap(
         p_335193_, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(holdergetter.getOrThrow(Noises.SHIFT))))
      );
      p_335193_.register(BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0));
      p_335193_.register(BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 60.0, 8.0));
      p_335193_.register(BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25, 0.25, 80.0, 160.0, 4.0));
      Holder<DensityFunction> holder = p_335193_.register(
         CONTINENTS,
         DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, holdergetter.getOrThrow(Noises.CONTINENTALNESS)))
      );
      Holder<DensityFunction> holder1 = p_335193_.register(
         EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, holdergetter.getOrThrow(Noises.EROSION)))
      );
      DensityFunction densityfunction2 = registerAndWrap(
         p_335193_,
         RIDGES,
         DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, holdergetter.getOrThrow(Noises.RIDGE)))
      );
      p_335193_.register(RIDGES_FOLDED, peaksAndValleys(densityfunction2));
      DensityFunction densityfunction3 = DensityFunctions.noise(holdergetter.getOrThrow(Noises.JAGGED), 1500.0, 0.0);
      registerTerrainNoises(p_335193_, holdergetter1, densityfunction3, holder, holder1, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
      Holder<DensityFunction> holder2 = p_335193_.register(
         CONTINENTS_LARGE,
         DensityFunctions.flatCache(
            DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, holdergetter.getOrThrow(Noises.CONTINENTALNESS_LARGE))
         )
      );
      Holder<DensityFunction> holder3 = p_335193_.register(
         EROSION_LARGE,
         DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, holdergetter.getOrThrow(Noises.EROSION_LARGE)))
      );
      registerTerrainNoises(
         p_335193_, holdergetter1, densityfunction3, holder2, holder3, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false
      );
      registerTerrainNoises(
         p_335193_,
         holdergetter1,
         densityfunction3,
         holder,
         holder1,
         OFFSET_AMPLIFIED,
         FACTOR_AMPLIFIED,
         JAGGEDNESS_AMPLIFIED,
         DEPTH_AMPLIFIED,
         SLOPED_CHEESE_AMPLIFIED,
         true
      );
      p_335193_.register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(holdergetter1, BASE_3D_NOISE_END)));
      p_335193_.register(SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction(holdergetter));
      p_335193_.register(
         SPAGHETTI_2D_THICKNESS_MODULATOR,
         DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(holdergetter.getOrThrow(Noises.SPAGHETTI_2D_THICKNESS), 2.0, 1.0, -0.6, -1.3))
      );
      p_335193_.register(SPAGHETTI_2D, spaghetti2D(holdergetter1, holdergetter));
      p_335193_.register(ENTRANCES, entrances(holdergetter1, holdergetter));
      p_335193_.register(NOODLE, noodle(holdergetter1, holdergetter));
      return p_335193_.register(PILLARS, pillars(holdergetter));
   }

   private static void registerTerrainNoises(
      BootstrapContext<DensityFunction> p_330992_,
      HolderGetter<DensityFunction> p_256393_,
      DensityFunction p_224476_,
      Holder<DensityFunction> p_224477_,
      Holder<DensityFunction> p_224478_,
      ResourceKey<DensityFunction> p_224479_,
      ResourceKey<DensityFunction> p_224480_,
      ResourceKey<DensityFunction> p_224481_,
      ResourceKey<DensityFunction> p_224482_,
      ResourceKey<DensityFunction> p_224483_,
      boolean p_224484_
   ) {
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate = new DensityFunctions.Spline.Coordinate(p_224477_);
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate1 = new DensityFunctions.Spline.Coordinate(p_224478_);
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate2 = new DensityFunctions.Spline.Coordinate(p_256393_.getOrThrow(RIDGES));
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate3 = new DensityFunctions.Spline.Coordinate(p_256393_.getOrThrow(RIDGES_FOLDED));
      DensityFunction densityfunction = registerAndWrap(
         p_330992_,
         p_224479_,
         splineWithBlending(
            DensityFunctions.add(
               DensityFunctions.constant(-0.50375F),
               DensityFunctions.spline(
                  TerrainProvider.overworldOffset(
                     densityfunctions$spline$coordinate, densityfunctions$spline$coordinate1, densityfunctions$spline$coordinate3, p_224484_
                  )
               )
            ),
            DensityFunctions.blendOffset()
         )
      );
      DensityFunction densityfunction1 = registerAndWrap(
         p_330992_,
         p_224480_,
         splineWithBlending(
            DensityFunctions.spline(
               TerrainProvider.overworldFactor(
                  densityfunctions$spline$coordinate,
                  densityfunctions$spline$coordinate1,
                  densityfunctions$spline$coordinate2,
                  densityfunctions$spline$coordinate3,
                  p_224484_
               )
            ),
            BLENDING_FACTOR
         )
      );
      DensityFunction densityfunction2 = registerAndWrap(p_330992_, p_224482_, offsetToDepth(densityfunction));
      DensityFunction densityfunction3 = registerAndWrap(
         p_330992_,
         p_224481_,
         splineWithBlending(
            DensityFunctions.spline(
               TerrainProvider.overworldJaggedness(
                  densityfunctions$spline$coordinate,
                  densityfunctions$spline$coordinate1,
                  densityfunctions$spline$coordinate2,
                  densityfunctions$spline$coordinate3,
                  p_224484_
               )
            ),
            BLENDING_JAGGEDNESS
         )
      );
      DensityFunction densityfunction4 = DensityFunctions.mul(densityfunction3, p_224476_.halfNegative());
      DensityFunction densityfunction5 = noiseGradientDensity(densityfunction1, DensityFunctions.add(densityfunction2, densityfunction4));
      p_330992_.register(p_224483_, DensityFunctions.add(densityfunction5, getFunction(p_256393_, BASE_3D_NOISE_OVERWORLD)));
   }

   private static DensityFunction offsetToDepth(DensityFunction p_429524_) {
      return DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), p_429524_);
   }

   private static DensityFunction registerAndWrap(
      BootstrapContext<DensityFunction> p_331307_, ResourceKey<DensityFunction> p_255905_, DensityFunction p_255856_
   ) {
      return new DensityFunctions.HolderHolder(p_331307_.register(p_255905_, p_255856_));
   }

   private static DensityFunction getFunction(HolderGetter<DensityFunction> p_256312_, ResourceKey<DensityFunction> p_256077_) {
      return new DensityFunctions.HolderHolder(p_256312_.getOrThrow(p_256077_));
   }

   private static DensityFunction peaksAndValleys(DensityFunction p_224438_) {
      return DensityFunctions.mul(
         DensityFunctions.add(
            DensityFunctions.add(p_224438_.abs(), DensityFunctions.constant(-0.6666666666666666)).abs(), DensityFunctions.constant(-0.3333333333333333)
         ),
         DensityFunctions.constant(-3.0)
      );
   }

   public static float peaksAndValleys(float p_224436_) {
      return -(Math.abs(Math.abs(p_224436_) - 0.6666667F) - 0.33333334F) * 3.0F;
   }

   private static DensityFunction spaghettiRoughnessFunction(HolderGetter<NormalNoise.NoiseParameters> p_255763_) {
      DensityFunction densityfunction = DensityFunctions.noise(p_255763_.getOrThrow(Noises.SPAGHETTI_ROUGHNESS));
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(p_255763_.getOrThrow(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0, -0.1);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(densityfunction1, DensityFunctions.add(densityfunction.abs(), DensityFunctions.constant(-0.4))));
   }

   private static DensityFunction entrances(HolderGetter<DensityFunction> p_256511_, HolderGetter<NormalNoise.NoiseParameters> p_255899_) {
      DensityFunction densityfunction = DensityFunctions.cacheOnce(DensityFunctions.noise(p_255899_.getOrThrow(Noises.SPAGHETTI_3D_RARITY), 2.0, 1.0));
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(p_255899_.getOrThrow(Noises.SPAGHETTI_3D_THICKNESS), -0.065, -0.088);
      DensityFunction densityfunction2 = DensityFunctions.weirdScaledSampler(
         densityfunction, p_255899_.getOrThrow(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1
      );
      DensityFunction densityfunction3 = DensityFunctions.weirdScaledSampler(
         densityfunction, p_255899_.getOrThrow(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1
      );
      DensityFunction densityfunction4 = DensityFunctions.add(DensityFunctions.max(densityfunction2, densityfunction3), densityfunction1).clamp(-1.0, 1.0);
      DensityFunction densityfunction5 = getFunction(p_256511_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction densityfunction6 = DensityFunctions.noise(p_255899_.getOrThrow(Noises.CAVE_ENTRANCE), 0.75, 0.5);
      DensityFunction densityfunction7 = DensityFunctions.add(
         DensityFunctions.add(densityfunction6, DensityFunctions.constant(0.37)), DensityFunctions.yClampedGradient(-10, 30, 0.3, 0.0)
      );
      return DensityFunctions.cacheOnce(DensityFunctions.min(densityfunction7, DensityFunctions.add(densityfunction5, densityfunction4)));
   }

   private static DensityFunction noodle(HolderGetter<DensityFunction> p_256402_, HolderGetter<NormalNoise.NoiseParameters> p_255632_) {
      DensityFunction densityfunction = getFunction(p_256402_, Y);
      int i = -64;
      int j = -60;
      int k = 320;
      DensityFunction densityfunction1 = yLimitedInterpolatable(
         densityfunction, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE), 1.0, 1.0), -60, 320, -1
      );
      DensityFunction densityfunction2 = yLimitedInterpolatable(
         densityfunction, DensityFunctions.mappedNoise(p_255632_.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05, -0.1), -60, 320, 0
      );
      double d0 = 2.6666666666666665;
      DensityFunction densityfunction3 = yLimitedInterpolatable(
         densityfunction, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), -60, 320, 0
      );
      DensityFunction densityfunction4 = yLimitedInterpolatable(
         densityfunction, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), -60, 320, 0
      );
      DensityFunction densityfunction5 = DensityFunctions.mul(
         DensityFunctions.constant(1.5), DensityFunctions.max(densityfunction3.abs(), densityfunction4.abs())
      );
      return DensityFunctions.rangeChoice(
         densityfunction1, -1000000.0, 0.0, DensityFunctions.constant(64.0), DensityFunctions.add(densityfunction2, densityfunction5)
      );
   }

   private static DensityFunction pillars(HolderGetter<NormalNoise.NoiseParameters> p_255985_) {
      double d0 = 25.0;
      double d1 = 0.3;
      DensityFunction densityfunction = DensityFunctions.noise(p_255985_.getOrThrow(Noises.PILLAR), 25.0, 0.3);
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(p_255985_.getOrThrow(Noises.PILLAR_RARENESS), 0.0, -2.0);
      DensityFunction densityfunction2 = DensityFunctions.mappedNoise(p_255985_.getOrThrow(Noises.PILLAR_THICKNESS), 0.0, 1.1);
      DensityFunction densityfunction3 = DensityFunctions.add(DensityFunctions.mul(densityfunction, DensityFunctions.constant(2.0)), densityfunction1);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(densityfunction3, densityfunction2.cube()));
   }

   private static DensityFunction spaghetti2D(HolderGetter<DensityFunction> p_256535_, HolderGetter<NormalNoise.NoiseParameters> p_255650_) {
      DensityFunction densityfunction = DensityFunctions.noise(p_255650_.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
      DensityFunction densityfunction1 = DensityFunctions.weirdScaledSampler(
         densityfunction, p_255650_.getOrThrow(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2
      );
      DensityFunction densityfunction2 = DensityFunctions.mappedNoise(p_255650_.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(-64, 8), 8.0);
      DensityFunction densityfunction3 = getFunction(p_256535_, SPAGHETTI_2D_THICKNESS_MODULATOR);
      DensityFunction densityfunction4 = DensityFunctions.add(densityfunction2, DensityFunctions.yClampedGradient(-64, 320, 8.0, -40.0)).abs();
      DensityFunction densityfunction5 = DensityFunctions.add(densityfunction4, densityfunction3).cube();
      double d0 = 0.083;
      DensityFunction densityfunction6 = DensityFunctions.add(densityfunction1, DensityFunctions.mul(DensityFunctions.constant(0.083), densityfunction3));
      return DensityFunctions.max(densityfunction6, densityfunction5).clamp(-1.0, 1.0);
   }

   private static DensityFunction underground(
      HolderGetter<DensityFunction> p_256548_, HolderGetter<NormalNoise.NoiseParameters> p_256236_, DensityFunction p_256658_
   ) {
      DensityFunction densityfunction = getFunction(p_256548_, SPAGHETTI_2D);
      DensityFunction densityfunction1 = getFunction(p_256548_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction densityfunction2 = DensityFunctions.noise(p_256236_.getOrThrow(Noises.CAVE_LAYER), 8.0);
      DensityFunction densityfunction3 = DensityFunctions.mul(DensityFunctions.constant(4.0), densityfunction2.square());
      DensityFunction densityfunction4 = DensityFunctions.noise(p_256236_.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666);
      DensityFunction densityfunction5 = DensityFunctions.add(
         DensityFunctions.add(DensityFunctions.constant(0.27), densityfunction4).clamp(-1.0, 1.0),
         DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), p_256658_)).clamp(0.0, 0.5)
      );
      DensityFunction densityfunction6 = DensityFunctions.add(densityfunction3, densityfunction5);
      DensityFunction densityfunction7 = DensityFunctions.min(
         DensityFunctions.min(densityfunction6, getFunction(p_256548_, ENTRANCES)), DensityFunctions.add(densityfunction, densityfunction1)
      );
      DensityFunction densityfunction8 = getFunction(p_256548_, PILLARS);
      DensityFunction densityfunction9 = DensityFunctions.rangeChoice(
         densityfunction8, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), densityfunction8
      );
      return DensityFunctions.max(densityfunction7, densityfunction9);
   }

   private static DensityFunction postProcess(DensityFunction p_224493_) {
      DensityFunction densityfunction = DensityFunctions.blendDensity(p_224493_);
      return DensityFunctions.mul(DensityFunctions.interpolated(densityfunction), DensityFunctions.constant(0.64)).squeeze();
   }

   private static DensityFunction remap(DensityFunction p_428264_, double p_422512_, double p_426363_, double p_431610_, double p_429938_) {
      double d0 = (p_429938_ - p_431610_) / (p_426363_ - p_422512_);
      double d1 = p_431610_ - p_422512_ * d0;
      return DensityFunctions.add(DensityFunctions.mul(p_428264_, DensityFunctions.constant(d0)), DensityFunctions.constant(d1));
   }

   protected static NoiseRouter overworld(
      HolderGetter<DensityFunction> p_255681_, HolderGetter<NormalNoise.NoiseParameters> p_256005_, boolean p_255649_, boolean p_255617_
   ) {
      DensityFunction densityfunction = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
      DensityFunction densityfunction1 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
      DensityFunction densityfunction2 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
      DensityFunction densityfunction3 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_LAVA));
      DensityFunction densityfunction4 = getFunction(p_255681_, SHIFT_X);
      DensityFunction densityfunction5 = getFunction(p_255681_, SHIFT_Z);
      DensityFunction densityfunction6 = DensityFunctions.shiftedNoise2d(
         densityfunction4, densityfunction5, 0.25, p_256005_.getOrThrow(p_255649_ ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE)
      );
      DensityFunction densityfunction7 = DensityFunctions.shiftedNoise2d(
         densityfunction4, densityfunction5, 0.25, p_256005_.getOrThrow(p_255649_ ? Noises.VEGETATION_LARGE : Noises.VEGETATION)
      );
      DensityFunction densityfunction8 = getFunction(p_255681_, p_255649_ ? OFFSET_LARGE : (p_255617_ ? OFFSET_AMPLIFIED : OFFSET));
      DensityFunction densityfunction9 = getFunction(p_255681_, p_255649_ ? FACTOR_LARGE : (p_255617_ ? FACTOR_AMPLIFIED : FACTOR));
      DensityFunction densityfunction10 = getFunction(p_255681_, p_255649_ ? DEPTH_LARGE : (p_255617_ ? DEPTH_AMPLIFIED : DEPTH));
      DensityFunction densityfunction11 = preliminarySurfaceLevel(densityfunction8, densityfunction9, p_255617_);
      DensityFunction densityfunction12 = getFunction(p_255681_, p_255649_ ? SLOPED_CHEESE_LARGE : (p_255617_ ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE));
      DensityFunction densityfunction13 = DensityFunctions.min(
         densityfunction12, DensityFunctions.mul(DensityFunctions.constant(5.0), getFunction(p_255681_, ENTRANCES))
      );
      DensityFunction densityfunction14 = DensityFunctions.rangeChoice(
         densityfunction12, -1000000.0, 1.5625, densityfunction13, underground(p_255681_, p_256005_, densityfunction12)
      );
      DensityFunction densityfunction15 = DensityFunctions.min(postProcess(slideOverworld(p_255617_, densityfunction14)), getFunction(p_255681_, NOODLE));
      DensityFunction densityfunction16 = getFunction(p_255681_, Y);
      int i = Stream.of(OreVeinifier.VeinType.values()).mapToInt(p_224495_ -> p_224495_.minY).min().orElse(-DimensionType.MIN_Y * 2);
      int j = Stream.of(OreVeinifier.VeinType.values()).mapToInt(p_224457_ -> p_224457_.maxY).max().orElse(-DimensionType.MIN_Y * 2);
      DensityFunction densityfunction17 = yLimitedInterpolatable(
         densityfunction16, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5), i, j, 0
      );
      float f = 4.0F;
      DensityFunction densityfunction18 = yLimitedInterpolatable(
            densityfunction16, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0), i, j, 0
         )
         .abs();
      DensityFunction densityfunction19 = yLimitedInterpolatable(
            densityfunction16, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0), i, j, 0
         )
         .abs();
      DensityFunction densityfunction20 = DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max(densityfunction18, densityfunction19));
      DensityFunction densityfunction21 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_GAP));
      return new NoiseRouter(
         densityfunction,
         densityfunction1,
         densityfunction2,
         densityfunction3,
         densityfunction6,
         densityfunction7,
         getFunction(p_255681_, p_255649_ ? CONTINENTS_LARGE : CONTINENTS),
         getFunction(p_255681_, p_255649_ ? EROSION_LARGE : EROSION),
         densityfunction10,
         getFunction(p_255681_, RIDGES),
         densityfunction11,
         densityfunction15,
         densityfunction17,
         densityfunction20,
         densityfunction21
      );
   }

   private static NoiseRouter noNewCaves(
      HolderGetter<DensityFunction> p_255724_, HolderGetter<NormalNoise.NoiseParameters> p_255986_, DensityFunction p_256378_
   ) {
      DensityFunction densityfunction = getFunction(p_255724_, SHIFT_X);
      DensityFunction densityfunction1 = getFunction(p_255724_, SHIFT_Z);
      DensityFunction densityfunction2 = DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, p_255986_.getOrThrow(Noises.TEMPERATURE));
      DensityFunction densityfunction3 = DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25, p_255986_.getOrThrow(Noises.VEGETATION));
      DensityFunction densityfunction4 = postProcess(p_256378_);
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         densityfunction2,
         densityfunction3,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         densityfunction4,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   private static DensityFunction slideOverworld(boolean p_224490_, DensityFunction p_224491_) {
      return slide(p_224491_, -64, 384, p_224490_ ? 16 : 80, p_224490_ ? 0 : 64, -0.078125, 0, 24, p_224490_ ? 0.4 : 0.1171875);
   }

   private static DensityFunction slideNetherLike(HolderGetter<DensityFunction> p_256084_, int p_255802_, int p_255834_) {
      return slide(getFunction(p_256084_, BASE_3D_NOISE_NETHER), p_255802_, p_255834_, 24, 0, 0.9375, -8, 24, 2.5);
   }

   private static DensityFunction slideEndLike(DensityFunction p_224440_, int p_224441_, int p_224442_) {
      return slide(p_224440_, p_224441_, p_224442_, 72, -184, -23.4375, 4, 32, -0.234375);
   }

   protected static NoiseRouter nether(HolderGetter<DensityFunction> p_256256_, HolderGetter<NormalNoise.NoiseParameters> p_256169_) {
      return noNewCaves(p_256256_, p_256169_, slideNetherLike(p_256256_, 0, 128));
   }

   protected static NoiseRouter caves(HolderGetter<DensityFunction> p_256088_, HolderGetter<NormalNoise.NoiseParameters> p_255675_) {
      return noNewCaves(p_256088_, p_255675_, slideNetherLike(p_256088_, -64, 192));
   }

   protected static NoiseRouter floatingIslands(HolderGetter<DensityFunction> p_256633_, HolderGetter<NormalNoise.NoiseParameters> p_255902_) {
      return noNewCaves(p_256633_, p_255902_, slideEndLike(getFunction(p_256633_, BASE_3D_NOISE_END), 0, 256));
   }

   private static DensityFunction slideEnd(DensityFunction p_224506_) {
      return slideEndLike(p_224506_, 0, 128);
   }

   protected static NoiseRouter end(HolderGetter<DensityFunction> p_256079_) {
      DensityFunction densityfunction = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
      DensityFunction densityfunction1 = postProcess(slideEnd(getFunction(p_256079_, SLOPED_CHEESE_END)));
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         densityfunction,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         densityfunction1,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   protected static NoiseRouter none() {
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   private static DensityFunction splineWithBlending(DensityFunction p_224454_, DensityFunction p_224455_) {
      DensityFunction densityfunction = DensityFunctions.lerp(DensityFunctions.blendAlpha(), p_224455_, p_224454_);
      return DensityFunctions.flatCache(DensityFunctions.cache2d(densityfunction));
   }

   private static DensityFunction noiseGradientDensity(DensityFunction p_212272_, DensityFunction p_212273_) {
      DensityFunction densityfunction = DensityFunctions.mul(p_212273_, p_212272_);
      return DensityFunctions.mul(DensityFunctions.constant(4.0), densityfunction.quarterNegative());
   }

   private static DensityFunction preliminarySurfaceLevel(DensityFunction p_425056_, DensityFunction p_424332_, boolean p_427816_) {
      DensityFunction densityfunction = DensityFunctions.cache2d(p_424332_);
      DensityFunction densityfunction1 = DensityFunctions.cache2d(p_425056_);
      DensityFunction densityfunction2 = remap(
         DensityFunctions.add(
            DensityFunctions.mul(DensityFunctions.constant(0.2734375), densityfunction.invert()),
            DensityFunctions.mul(DensityFunctions.constant(-1.0), densityfunction1)
         ),
         1.5,
         -1.5,
         -64.0,
         320.0
      );
      densityfunction2 = densityfunction2.clamp(-40.0, 320.0);
      DensityFunction densityfunction3 = DensityFunctions.add(
         slideOverworld(
            p_427816_,
            DensityFunctions.add(noiseGradientDensity(densityfunction, offsetToDepth(densityfunction1)), DensityFunctions.constant(-0.703125))
               .clamp(-64.0, 64.0)
         ),
         DensityFunctions.constant(-0.390625)
      );
      return DensityFunctions.findTopSurface(densityfunction3, densityfunction2, -64, NoiseSettings.OVERWORLD_NOISE_SETTINGS.getCellHeight());
   }

   private static DensityFunction yLimitedInterpolatable(DensityFunction p_209472_, DensityFunction p_209473_, int p_209474_, int p_209475_, int p_209476_) {
      return DensityFunctions.interpolated(DensityFunctions.rangeChoice(p_209472_, p_209474_, p_209475_ + 1, p_209473_, DensityFunctions.constant(p_209476_)));
   }

   private static DensityFunction slide(
      DensityFunction p_224444_, int p_224445_, int p_224446_, int p_224447_, int p_224448_, double p_224449_, int p_224450_, int p_224451_, double p_224452_
   ) {
      DensityFunction densityfunction1 = DensityFunctions.yClampedGradient(p_224445_ + p_224446_ - p_224447_, p_224445_ + p_224446_ - p_224448_, 1.0, 0.0);
      DensityFunction $$9 = DensityFunctions.lerp(densityfunction1, p_224449_, p_224444_);
      DensityFunction densityfunction2 = DensityFunctions.yClampedGradient(p_224445_ + p_224450_, p_224445_ + p_224451_, 0.0, 1.0);
      return DensityFunctions.lerp(densityfunction2, p_224452_, $$9);
   }

   protected static final class QuantizedSpaghettiRarity {
      protected static double getSphaghettiRarity2D(double p_209564_) {
         if (p_209564_ < -0.75) {
            return 0.5;
         } else if (p_209564_ < -0.5) {
            return 0.75;
         } else if (p_209564_ < 0.5) {
            return 1.0;
         } else {
            return p_209564_ < 0.75 ? 2.0 : 3.0;
         }
      }

      protected static double getSpaghettiRarity3D(double p_209566_) {
         if (p_209566_ < -0.5) {
            return 0.75;
         } else if (p_209566_ < 0.0) {
            return 1.0;
         } else {
            return p_209566_ < 0.5 ? 1.5 : 2.0;
         }
      }
   }
}
