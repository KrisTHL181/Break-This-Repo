package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public final class OverworldBiomeBuilder {
    private static final float VALLEY_SIZE = 0.05F;
    private static final float LOW_START = 0.26666668F;
    public static final float HIGH_START = 0.4F;
    private static final float HIGH_END = 0.93333334F;
    private static final float PEAK_SIZE = 0.1F;
    public static final float PEAK_START = 0.56666666F;
    private static final float PEAK_END = 0.7666667F;
    public static final float NEAR_INLAND_START = -0.11F;
    public static final float MID_INLAND_START = 0.03F;
    public static final float FAR_INLAND_START = 0.3F;
    public static final float EROSION_INDEX_1_START = -0.78F;
    public static final float EROSION_INDEX_2_START = -0.375F;
    private static final float EROSION_DEEP_DARK_DRYNESS_THRESHOLD = -0.225F;
    private static final float DEPTH_DEEP_DARK_DRYNESS_THRESHOLD = 0.9F;
    private final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
    private final Climate.Parameter[] temperatures = new Climate.Parameter[]{
        Climate.Parameter.span(-1.0F, -0.45F),
        Climate.Parameter.span(-0.45F, -0.15F),
        Climate.Parameter.span(-0.15F, 0.2F),
        Climate.Parameter.span(0.2F, 0.55F),
        Climate.Parameter.span(0.55F, 1.0F)
    };
    private final Climate.Parameter[] humidities = new Climate.Parameter[]{
        Climate.Parameter.span(-1.0F, -0.35F),
        Climate.Parameter.span(-0.35F, -0.1F),
        Climate.Parameter.span(-0.1F, 0.1F),
        Climate.Parameter.span(0.1F, 0.3F),
        Climate.Parameter.span(0.3F, 1.0F)
    };
    private final Climate.Parameter[] erosions = new Climate.Parameter[]{
        Climate.Parameter.span(-1.0F, -0.78F),
        Climate.Parameter.span(-0.78F, -0.375F),
        Climate.Parameter.span(-0.375F, -0.2225F),
        Climate.Parameter.span(-0.2225F, 0.05F),
        Climate.Parameter.span(0.05F, 0.45F),
        Climate.Parameter.span(0.45F, 0.55F),
        Climate.Parameter.span(0.55F, 1.0F)
    };
    private final Climate.Parameter FROZEN_RANGE = this.temperatures[0];
    private final Climate.Parameter UNFROZEN_RANGE = Climate.Parameter.span(this.temperatures[1], this.temperatures[4]);
    private final Climate.Parameter mushroomFieldsContinentalness = Climate.Parameter.span(-1.2F, -1.05F);
    private final Climate.Parameter deepOceanContinentalness = Climate.Parameter.span(-1.05F, -0.455F);
    private final Climate.Parameter oceanContinentalness = Climate.Parameter.span(-0.455F, -0.19F);
    private final Climate.Parameter coastContinentalness = Climate.Parameter.span(-0.19F, -0.11F);
    private final Climate.Parameter inlandContinentalness = Climate.Parameter.span(-0.11F, 0.55F);
    private final Climate.Parameter nearInlandContinentalness = Climate.Parameter.span(-0.11F, 0.03F);
    private final Climate.Parameter midInlandContinentalness = Climate.Parameter.span(0.03F, 0.3F);
    private final Climate.Parameter farInlandContinentalness = Climate.Parameter.span(0.3F, 1.0F);
    private final ResourceKey<Biome>[][] OCEANS = new ResourceKey[][]{
        {Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN},
        {Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN}
    };
    private final ResourceKey<Biome>[][] MIDDLE_BIOMES = new ResourceKey[][]{
        {Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.TAIGA},
        {Biomes.PLAINS, Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA},
        {Biomes.FLOWER_FOREST, Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST},
        {Biomes.SAVANNA, Biomes.SAVANNA, Biomes.FOREST, Biomes.JUNGLE, Biomes.JUNGLE},
        {Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT}
    };
    private final ResourceKey<Biome>[][] MIDDLE_BIOMES_VARIANT = new ResourceKey[][]{
        {Biomes.ICE_SPIKES, null, Biomes.SNOWY_TAIGA, null, null},
        {null, null, null, null, Biomes.OLD_GROWTH_PINE_TAIGA},
        {Biomes.SUNFLOWER_PLAINS, null, null, Biomes.OLD_GROWTH_BIRCH_FOREST, null},
        {null, null, Biomes.PLAINS, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE},
        {null, null, null, null, null}
    };
    private final ResourceKey<Biome>[][] PLATEAU_BIOMES = new ResourceKey[][]{
        {Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_TAIGA},
        {Biomes.MEADOW, Biomes.MEADOW, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA},
        {Biomes.MEADOW, Biomes.MEADOW, Biomes.MEADOW, Biomes.MEADOW, Biomes.PALE_GARDEN},
        {Biomes.SAVANNA_PLATEAU, Biomes.SAVANNA_PLATEAU, Biomes.FOREST, Biomes.FOREST, Biomes.JUNGLE},
        {Biomes.BADLANDS, Biomes.BADLANDS, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.WOODED_BADLANDS}
    };
    private final ResourceKey<Biome>[][] PLATEAU_BIOMES_VARIANT = new ResourceKey[][]{
        {Biomes.ICE_SPIKES, null, null, null, null},
        {Biomes.CHERRY_GROVE, null, Biomes.MEADOW, Biomes.MEADOW, Biomes.OLD_GROWTH_PINE_TAIGA},
        {Biomes.CHERRY_GROVE, Biomes.CHERRY_GROVE, Biomes.FOREST, Biomes.BIRCH_FOREST, null},
        {null, null, null, null, null},
        {Biomes.ERODED_BADLANDS, Biomes.ERODED_BADLANDS, null, null, null}
    };
    private final ResourceKey<Biome>[][] SHATTERED_BIOMES = new ResourceKey[][]{
        {Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST},
        {Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST},
        {Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST},
        {null, null, null, null, null},
        {null, null, null, null, null}
    };

    public List<Climate.ParameterPoint> spawnTarget() {
        Climate.Parameter surfaceDepth = Climate.Parameter.point(0.0F);
        float riverClearance = 0.16F;
        return List.of(
            new Climate.ParameterPoint(
                this.FULL_RANGE,
                this.FULL_RANGE,
                Climate.Parameter.span(this.inlandContinentalness, this.FULL_RANGE),
                this.FULL_RANGE,
                surfaceDepth,
                Climate.Parameter.span(-1.0F, -0.16F),
                0L
            ),
            new Climate.ParameterPoint(
                this.FULL_RANGE,
                this.FULL_RANGE,
                Climate.Parameter.span(this.inlandContinentalness, this.FULL_RANGE),
                this.FULL_RANGE,
                surfaceDepth,
                Climate.Parameter.span(0.16F, 1.0F),
                0L
            )
        );
    }

    void addBiomes(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        if (SharedConstants.debugGenerateSquareTerrainWithoutNoise) {
            this.addDebugBiomes(biomes);
        } else {
            this.addOffCoastBiomes(biomes);
            this.addInlandBiomes(biomes);
            this.addUndergroundBiomes(biomes);
        }
    }

    private void addDebugBiomes(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        HolderLookup.Provider builtIns = new RegistrySetBuilder()
            .add(Registries.DENSITY_FUNCTION, NoiseRouterData::bootstrap)
            .add(Registries.NOISE, NoiseData::bootstrap)
            .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        HolderGetter<DensityFunction> densityFunctions = builtIns.lookupOrThrow(Registries.DENSITY_FUNCTION);
        DensityFunctions.Spline.Coordinate continents = new DensityFunctions.Spline.Coordinate(
            new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.CONTINENTS))
        );
        DensityFunctions.Spline.Coordinate erosion = new DensityFunctions.Spline.Coordinate(
            new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.EROSION))
        );
        DensityFunctions.Spline.Coordinate ridges = new DensityFunctions.Spline.Coordinate(
            new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.RIDGES_FOLDED))
        );
        biomes.accept(
            Pair.of(
                Climate.parameters(this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.point(0.0F), this.FULL_RANGE, 0.01F),
                Biomes.PLAINS
            )
        );
        if (TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, Float2FloatFunction.identity()) instanceof CubicSpline.Multipoint<?> multipoint
            )
         {
            ResourceKey<Biome> biome = Biomes.DESERT;

            for (float location : multipoint.locations()) {
                biomes.accept(
                    Pair.of(
                        Climate.parameters(
                            this.FULL_RANGE,
                            this.FULL_RANGE,
                            this.FULL_RANGE,
                            Climate.Parameter.point(location),
                            Climate.Parameter.point(0.0F),
                            this.FULL_RANGE,
                            0.0F
                        ),
                        biome
                    )
                );
                biome = biome == Biomes.DESERT ? Biomes.BADLANDS : Biomes.DESERT;
            }
        }

        if (TerrainProvider.overworldOffset(continents, erosion, ridges, false) instanceof CubicSpline.Multipoint<?> multipoint) {
            for (float location : multipoint.locations()) {
                biomes.accept(
                    Pair.of(
                        Climate.parameters(
                            this.FULL_RANGE,
                            this.FULL_RANGE,
                            Climate.Parameter.point(location),
                            this.FULL_RANGE,
                            Climate.Parameter.point(0.0F),
                            this.FULL_RANGE,
                            0.0F
                        ),
                        Biomes.SNOWY_TAIGA
                    )
                );
            }
        }
    }

    private void addOffCoastBiomes(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addSurfaceBiome(
            biomes, this.FULL_RANGE, this.FULL_RANGE, this.mushroomFieldsContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0F, Biomes.MUSHROOM_FIELDS
        );

        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            this.addSurfaceBiome(
                biomes, temperature, this.FULL_RANGE, this.deepOceanContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0F, this.OCEANS[0][temperatureIndex]
            );
            this.addSurfaceBiome(
                biomes, temperature, this.FULL_RANGE, this.oceanContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0F, this.OCEANS[1][temperatureIndex]
            );
        }
    }

    private void addInlandBiomes(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addMidSlice(biomes, Climate.Parameter.span(-1.0F, -0.93333334F));
        this.addHighSlice(biomes, Climate.Parameter.span(-0.93333334F, -0.7666667F));
        this.addPeaks(biomes, Climate.Parameter.span(-0.7666667F, -0.56666666F));
        this.addHighSlice(biomes, Climate.Parameter.span(-0.56666666F, -0.4F));
        this.addMidSlice(biomes, Climate.Parameter.span(-0.4F, -0.26666668F));
        this.addLowSlice(biomes, Climate.Parameter.span(-0.26666668F, -0.05F));
        this.addValleys(biomes, Climate.Parameter.span(-0.05F, 0.05F));
        this.addLowSlice(biomes, Climate.Parameter.span(0.05F, 0.26666668F));
        this.addMidSlice(biomes, Climate.Parameter.span(0.26666668F, 0.4F));
        this.addHighSlice(biomes, Climate.Parameter.span(0.4F, 0.56666666F));
        this.addPeaks(biomes, Climate.Parameter.span(0.56666666F, 0.7666667F));
        this.addHighSlice(biomes, Climate.Parameter.span(0.7666667F, 0.93333334F));
        this.addMidSlice(biomes, Climate.Parameter.span(0.93333334F, 1.0F));
    }

    private void addPeaks(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, final Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];

            for (int humidityIndex = 0; humidityIndex < this.humidities.length; humidityIndex++) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness
                );
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(
                    temperatureIndex, humidityIndex, weirdness, shatteredBiome
                );
                ResourceKey<Biome> peakBiome = this.pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                    this.erosions[0],
                    weirdness,
                    0.0F,
                    peakBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    this.erosions[1],
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHotOrSlopeIfCold
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[1],
                    weirdness,
                    0.0F,
                    peakBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    Climate.Parameter.span(this.erosions[2], this.erosions[3]),
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[2],
                    weirdness,
                    0.0F,
                    plateauBiome
                );
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[3], weirdness, 0.0F, middleBiomeOrBadlandsIfHot
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[3], weirdness, 0.0F, plateauBiome);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                    this.erosions[4],
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    shatteredBiomeOrWindsweptSavanna
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    shatteredBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                    this.erosions[6],
                    weirdness,
                    0.0F,
                    middleBiome
                );
            }
        }
    }

    private void addHighSlice(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, final Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];

            for (int humidityIndex = 0; humidityIndex < this.humidities.length; humidityIndex++) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness
                );
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> slopeBiome = this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> peakBiome = this.pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    this.coastContinentalness,
                    Climate.Parameter.span(this.erosions[0], this.erosions[1]),
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[0], weirdness, 0.0F, slopeBiome);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[0],
                    weirdness,
                    0.0F,
                    peakBiome
                );
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[1], weirdness, 0.0F, middleBiomeOrBadlandsIfHotOrSlopeIfCold
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[1],
                    weirdness,
                    0.0F,
                    slopeBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    Climate.Parameter.span(this.erosions[2], this.erosions[3]),
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[2],
                    weirdness,
                    0.0F,
                    plateauBiome
                );
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[3], weirdness, 0.0F, middleBiomeOrBadlandsIfHot
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[3], weirdness, 0.0F, plateauBiome);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                    this.erosions[4],
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    middleBiomeOrWindsweptSavanna
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    shatteredBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                    this.erosions[6],
                    weirdness,
                    0.0F,
                    middleBiome
                );
            }
        }
    }

    private void addMidSlice(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, final Climate.Parameter weirdness) {
        this.addSurfaceBiome(
            biomes,
            this.FULL_RANGE,
            this.FULL_RANGE,
            this.coastContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[2]),
            weirdness,
            0.0F,
            Biomes.STONY_SHORE
        );
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[1], this.temperatures[2]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.SWAMP
        );
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[3], this.temperatures[4]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.MANGROVE_SWAMP
        );

        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];

            for (int humidityIndex = 0; humidityIndex < this.humidities.length; humidityIndex++) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness
                );
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> beachBiome = this.pickBeachBiome(temperatureIndex, humidityIndex);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> shatteredCoastBiome = this.pickShatteredCoastBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> slopeBiome = this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[0],
                    weirdness,
                    0.0F,
                    slopeBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.nearInlandContinentalness, this.midInlandContinentalness),
                    this.erosions[1],
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHotOrSlopeIfCold
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    this.farInlandContinentalness,
                    this.erosions[1],
                    weirdness,
                    0.0F,
                    temperatureIndex == 0 ? slopeBiome : plateauBiome
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[2], weirdness, 0.0F, middleBiome);
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[2], weirdness, 0.0F, middleBiomeOrBadlandsIfHot
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[2], weirdness, 0.0F, plateauBiome);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness),
                    this.erosions[3],
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[3],
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHot
                );
                if (weirdness.max() < 0L) {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[4], weirdness, 0.0F, beachBiome);
                    this.addSurfaceBiome(
                        biomes,
                        temperature,
                        humidity,
                        Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
                        this.erosions[4],
                        weirdness,
                        0.0F,
                        middleBiome
                    );
                } else {
                    this.addSurfaceBiome(
                        biomes,
                        temperature,
                        humidity,
                        Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
                        this.erosions[4],
                        weirdness,
                        0.0F,
                        middleBiome
                    );
                }

                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[5], weirdness, 0.0F, shatteredCoastBiome);
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[5], weirdness, 0.0F, middleBiomeOrWindsweptSavanna
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    shatteredBiome
                );
                if (weirdness.max() < 0L) {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0F, beachBiome);
                } else {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0F, middleBiome);
                }

                if (temperatureIndex == 0) {
                    this.addSurfaceBiome(
                        biomes,
                        temperature,
                        humidity,
                        Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
                        this.erosions[6],
                        weirdness,
                        0.0F,
                        middleBiome
                    );
                }
            }
        }
    }

    private void addLowSlice(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, final Climate.Parameter weirdness) {
        this.addSurfaceBiome(
            biomes,
            this.FULL_RANGE,
            this.FULL_RANGE,
            this.coastContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[2]),
            weirdness,
            0.0F,
            Biomes.STONY_SHORE
        );
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[1], this.temperatures[2]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.SWAMP
        );
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[3], this.temperatures[4]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.MANGROVE_SWAMP
        );

        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];

            for (int humidityIndex = 0; humidityIndex < this.humidities.length; humidityIndex++) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness
                );
                ResourceKey<Biome> beachBiome = this.pickBeachBiome(temperatureIndex, humidityIndex);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> shatteredCoastBiome = this.pickShatteredCoastBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    this.nearInlandContinentalness,
                    Climate.Parameter.span(this.erosions[0], this.erosions[1]),
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHot
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    Climate.Parameter.span(this.erosions[0], this.erosions[1]),
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHotOrSlopeIfCold
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    this.nearInlandContinentalness,
                    Climate.Parameter.span(this.erosions[2], this.erosions[3]),
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    Climate.Parameter.span(this.erosions[2], this.erosions[3]),
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHot
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    this.coastContinentalness,
                    Climate.Parameter.span(this.erosions[3], this.erosions[4]),
                    weirdness,
                    0.0F,
                    beachBiome
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[4],
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[5], weirdness, 0.0F, shatteredCoastBiome);
                this.addSurfaceBiome(
                    biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[5], weirdness, 0.0F, middleBiomeOrWindsweptSavanna
                );
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    this.erosions[5],
                    weirdness,
                    0.0F,
                    middleBiome
                );
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0F, beachBiome);
                if (temperatureIndex == 0) {
                    this.addSurfaceBiome(
                        biomes,
                        temperature,
                        humidity,
                        Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness),
                        this.erosions[6],
                        weirdness,
                        0.0F,
                        middleBiome
                    );
                }
            }
        }
    }

    private void addValleys(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, final Climate.Parameter weirdness) {
        this.addSurfaceBiome(
            biomes,
            this.FROZEN_RANGE,
            this.FULL_RANGE,
            this.coastContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[1]),
            weirdness,
            0.0F,
            weirdness.max() < 0L ? Biomes.STONY_SHORE : Biomes.FROZEN_RIVER
        );
        this.addSurfaceBiome(
            biomes,
            this.UNFROZEN_RANGE,
            this.FULL_RANGE,
            this.coastContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[1]),
            weirdness,
            0.0F,
            weirdness.max() < 0L ? Biomes.STONY_SHORE : Biomes.RIVER
        );
        this.addSurfaceBiome(
            biomes,
            this.FROZEN_RANGE,
            this.FULL_RANGE,
            this.nearInlandContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[1]),
            weirdness,
            0.0F,
            Biomes.FROZEN_RIVER
        );
        this.addSurfaceBiome(
            biomes,
            this.UNFROZEN_RANGE,
            this.FULL_RANGE,
            this.nearInlandContinentalness,
            Climate.Parameter.span(this.erosions[0], this.erosions[1]),
            weirdness,
            0.0F,
            Biomes.RIVER
        );
        this.addSurfaceBiome(
            biomes,
            this.FROZEN_RANGE,
            this.FULL_RANGE,
            Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
            Climate.Parameter.span(this.erosions[2], this.erosions[5]),
            weirdness,
            0.0F,
            Biomes.FROZEN_RIVER
        );
        this.addSurfaceBiome(
            biomes,
            this.UNFROZEN_RANGE,
            this.FULL_RANGE,
            Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness),
            Climate.Parameter.span(this.erosions[2], this.erosions[5]),
            weirdness,
            0.0F,
            Biomes.RIVER
        );
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, this.erosions[6], weirdness, 0.0F, Biomes.FROZEN_RIVER);
        this.addSurfaceBiome(biomes, this.UNFROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, this.erosions[6], weirdness, 0.0F, Biomes.RIVER);
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[1], this.temperatures[2]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.SWAMP
        );
        this.addSurfaceBiome(
            biomes,
            Climate.Parameter.span(this.temperatures[3], this.temperatures[4]),
            this.FULL_RANGE,
            Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.MANGROVE_SWAMP
        );
        this.addSurfaceBiome(
            biomes,
            this.FROZEN_RANGE,
            this.FULL_RANGE,
            Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness),
            this.erosions[6],
            weirdness,
            0.0F,
            Biomes.FROZEN_RIVER
        );

        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; temperatureIndex++) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];

            for (int humidityIndex = 0; humidityIndex < this.humidities.length; humidityIndex++) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(
                    biomes,
                    temperature,
                    humidity,
                    Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness),
                    Climate.Parameter.span(this.erosions[0], this.erosions[1]),
                    weirdness,
                    0.0F,
                    middleBiomeOrBadlandsIfHot
                );
            }
        }
    }

    private void addUndergroundBiomes(final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addUndergroundBiome(
            biomes, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(0.8F, 1.0F), this.FULL_RANGE, this.FULL_RANGE, 0.0F, Biomes.DRIPSTONE_CAVES
        );
        this.addUndergroundBiome(
            biomes, this.FULL_RANGE, Climate.Parameter.span(0.7F, 1.0F), this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, 0.0F, Biomes.LUSH_CAVES
        );
        this.addUndergroundBiome(
            biomes,
            this.FULL_RANGE,
            this.FULL_RANGE,
            Climate.Parameter.span(this.coastContinentalness, this.inlandContinentalness),
            Climate.Parameter.span(this.erosions[5], this.erosions[6]),
            Climate.Parameter.span(-1.1F, -0.85F),
            0.0F,
            Biomes.SULFUR_CAVES
        );
        this.addBottomBiome(
            biomes,
            this.FULL_RANGE,
            this.FULL_RANGE,
            this.FULL_RANGE,
            Climate.Parameter.span(this.erosions[0], this.erosions[1]),
            this.FULL_RANGE,
            0.0F,
            Biomes.DEEP_DARK
        );
    }

    private ResourceKey<Biome> pickMiddleBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        if (weirdness.max() < 0L) {
            return this.MIDDLE_BIOMES[temperatureIndex][humidityIndex];
        }

        ResourceKey<Biome> variant = this.MIDDLE_BIOMES_VARIANT[temperatureIndex][humidityIndex];
        return variant == null ? this.MIDDLE_BIOMES[temperatureIndex][humidityIndex] : variant;
    }

    private ResourceKey<Biome> pickMiddleBiomeOrBadlandsIfHot(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        return temperatureIndex == 4 ? this.pickBadlandsBiome(humidityIndex, weirdness) : this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
    }

    private ResourceKey<Biome> pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
        final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness
    ) {
        return temperatureIndex == 0
            ? this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness)
            : this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
    }

    private ResourceKey<Biome> maybePickWindsweptSavannaBiome(
        final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness, final ResourceKey<Biome> underlyingBiome
    ) {
        return temperatureIndex > 1 && humidityIndex < 4 && weirdness.max() >= 0L ? Biomes.WINDSWEPT_SAVANNA : underlyingBiome;
    }

    private ResourceKey<Biome> pickShatteredCoastBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        ResourceKey<Biome> beachOrMiddleBiome = weirdness.max() >= 0L
            ? this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness)
            : this.pickBeachBiome(temperatureIndex, humidityIndex);
        return this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, beachOrMiddleBiome);
    }

    private ResourceKey<Biome> pickBeachBiome(final int temperatureIndex, final int humidityIndex) {
        if (temperatureIndex == 0) {
            return Biomes.SNOWY_BEACH;
        } else {
            return temperatureIndex == 4 ? Biomes.DESERT : Biomes.BEACH;
        }
    }

    private ResourceKey<Biome> pickBadlandsBiome(final int humidityIndex, final Climate.Parameter weirdness) {
        if (humidityIndex < 2) {
            return weirdness.max() < 0L ? Biomes.BADLANDS : Biomes.ERODED_BADLANDS;
        } else {
            return humidityIndex < 3 ? Biomes.BADLANDS : Biomes.WOODED_BADLANDS;
        }
    }

    private ResourceKey<Biome> pickPlateauBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        if (weirdness.max() >= 0L) {
            ResourceKey<Biome> variant = this.PLATEAU_BIOMES_VARIANT[temperatureIndex][humidityIndex];
            if (variant != null) {
                return variant;
            }
        }

        return this.PLATEAU_BIOMES[temperatureIndex][humidityIndex];
    }

    private ResourceKey<Biome> pickPeakBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        if (temperatureIndex <= 2) {
            return weirdness.max() < 0L ? Biomes.JAGGED_PEAKS : Biomes.FROZEN_PEAKS;
        } else {
            return temperatureIndex == 3 ? Biomes.STONY_PEAKS : this.pickBadlandsBiome(humidityIndex, weirdness);
        }
    }

    private ResourceKey<Biome> pickSlopeBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        if (temperatureIndex >= 3) {
            return this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
        } else {
            return humidityIndex <= 1 ? Biomes.SNOWY_SLOPES : Biomes.GROVE;
        }
    }

    private ResourceKey<Biome> pickShatteredBiome(final int temperatureIndex, final int humidityIndex, final Climate.Parameter weirdness) {
        ResourceKey<Biome> biome = this.SHATTERED_BIOMES[temperatureIndex][humidityIndex];
        return biome == null ? this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness) : biome;
    }

    private void addSurfaceBiome(
        final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes,
        final Climate.Parameter temperature,
        final Climate.Parameter humidity,
        final Climate.Parameter continentalness,
        final Climate.Parameter erosion,
        final Climate.Parameter weirdness,
        final float offset,
        final ResourceKey<Biome> second
    ) {
        biomes.accept(Pair.of(Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(0.0F), weirdness, offset), second));
        biomes.accept(Pair.of(Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(1.0F), weirdness, offset), second));
    }

    private void addUndergroundBiome(
        final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes,
        final Climate.Parameter temperature,
        final Climate.Parameter humidity,
        final Climate.Parameter continentalness,
        final Climate.Parameter erosion,
        final Climate.Parameter weirdness,
        final float offset,
        final ResourceKey<Biome> biome
    ) {
        biomes.accept(
            Pair.of(Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.span(0.2F, 0.9F), weirdness, offset), biome)
        );
    }

    private void addBottomBiome(
        final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes,
        final Climate.Parameter temperature,
        final Climate.Parameter humidity,
        final Climate.Parameter continentalness,
        final Climate.Parameter erosion,
        final Climate.Parameter weirdness,
        final float offset,
        final ResourceKey<Biome> biome
    ) {
        biomes.accept(Pair.of(Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(1.1F), weirdness, offset), biome));
    }

    public static boolean isDeepDarkRegion(final DensityFunction erosion, final DensityFunction depth, final DensityFunction.FunctionContext context) {
        return erosion.compute(context) < -0.225F && depth.compute(context) > 0.9F;
    }

    public static String getDebugStringForPeaksAndValleys(final double peaksAndValleys) {
        if (peaksAndValleys < NoiseRouterData.peaksAndValleys(0.05F)) {
            return "Valley";
        } else if (peaksAndValleys < NoiseRouterData.peaksAndValleys(0.26666668F)) {
            return "Low";
        } else if (peaksAndValleys < NoiseRouterData.peaksAndValleys(0.4F)) {
            return "Mid";
        } else {
            return peaksAndValleys < NoiseRouterData.peaksAndValleys(0.56666666F) ? "High" : "Peak";
        }
    }

    public String getDebugStringForContinentalness(final double continentalness) {
        double continentalnessQuantized = Climate.quantizeCoord((float)continentalness);
        if (continentalnessQuantized < this.mushroomFieldsContinentalness.max()) {
            return "Mushroom fields";
        } else if (continentalnessQuantized < this.deepOceanContinentalness.max()) {
            return "Deep ocean";
        } else if (continentalnessQuantized < this.oceanContinentalness.max()) {
            return "Ocean";
        } else if (continentalnessQuantized < this.coastContinentalness.max()) {
            return "Coast";
        } else if (continentalnessQuantized < this.nearInlandContinentalness.max()) {
            return "Near inland";
        } else {
            return continentalnessQuantized < this.midInlandContinentalness.max() ? "Mid inland" : "Far inland";
        }
    }

    public String getDebugStringForErosion(final double erosion) {
        return getDebugStringForNoiseValue(erosion, this.erosions);
    }

    public String getDebugStringForTemperature(final double temperature) {
        return getDebugStringForNoiseValue(temperature, this.temperatures);
    }

    public String getDebugStringForHumidity(final double humidity) {
        return getDebugStringForNoiseValue(humidity, this.humidities);
    }

    private static String getDebugStringForNoiseValue(final double noiseValue, final Climate.Parameter[] array) {
        double noiseValueQuantized = Climate.quantizeCoord((float)noiseValue);

        for (int i = 0; i < array.length; i++) {
            if (noiseValueQuantized < array[i].max()) {
                return i + "";
            }
        }

        return "?";
    }

    @VisibleForDebug
    public Climate.Parameter[] getTemperatureThresholds() {
        return this.temperatures;
    }

    @VisibleForDebug
    public Climate.Parameter[] getHumidityThresholds() {
        return this.humidities;
    }

    @VisibleForDebug
    public Climate.Parameter[] getErosionThresholds() {
        return this.erosions;
    }

    @VisibleForDebug
    public Climate.Parameter[] getContinentalnessThresholds() {
        return new Climate.Parameter[]{
            this.mushroomFieldsContinentalness,
            this.deepOceanContinentalness,
            this.oceanContinentalness,
            this.coastContinentalness,
            this.nearInlandContinentalness,
            this.midInlandContinentalness,
            this.farInlandContinentalness
        };
    }

    @VisibleForDebug
    public Climate.Parameter[] getPeaksAndValleysThresholds() {
        return new Climate.Parameter[]{
            Climate.Parameter.span(-2.0F, NoiseRouterData.peaksAndValleys(0.05F)),
            Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.05F), NoiseRouterData.peaksAndValleys(0.26666668F)),
            Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.26666668F), NoiseRouterData.peaksAndValleys(0.4F)),
            Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.4F), NoiseRouterData.peaksAndValleys(0.56666666F)),
            Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.56666666F), 2.0F)
        };
    }

    @VisibleForDebug
    public Climate.Parameter[] getWeirdnessThresholds() {
        return new Climate.Parameter[]{Climate.Parameter.span(-2.0F, 0.0F), Climate.Parameter.span(0.0F, 2.0F)};
    }
}