package net.minecraft.data.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class OverworldBiomes {
    protected static final int NORMAL_WATER_COLOR = 4159204;
    private static final int DARK_DRY_FOLIAGE_COLOR = 8082228;
    public static final int SWAMP_SKELETON_WEIGHT = 70;

    public static int calculateSkyColor(final float temperature) {
        float temp = temperature;
        temp /= 3.0F;
        temp = Mth.clamp(temp, -1.0F, 1.0F);
        return ARGB.opaque(Mth.hsvToRgb(0.62222224F - temp * 0.05F, 0.5F + temp * 0.1F, 1.0F));
    }

    private static Biome.BiomeBuilder baseBiome(final float temperature, final float downfall) {
        return new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(temperature)
            .downfall(downfall)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, calculateSkyColor(temperature))
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build());
    }

    private static void globalOverworldGeneration(final BiomeGenerationSettings.Builder generation) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
    }

    public static Biome oldGrowthTaiga(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean spruce
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 4));
        mobs.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 3));
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.FOX, 2, 4));
        if (spruce) {
            BiomeDefaultFeatures.commonSpawns(mobs);
        } else {
            BiomeDefaultFeatures.caveSpawns(mobs);
            BiomeDefaultFeatures.monsters(mobs, 100, 25, 0, 100, false);
        }

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addMossyStoneBlock(generation);
        BiomeDefaultFeatures.addFerns(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        generation.addFeature(
            GenerationStep.Decoration.VEGETAL_DECORATION,
            spruce ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA
        );
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addGiantTaigaVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        BiomeDefaultFeatures.addCommonBerryBushes(generation);
        return baseBiome(spruce ? 0.25F : 0.3F, 0.8F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_OLD_GROWTH_TAIGA))
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome sparseJungle(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 2, 4));
        return baseJungle(placedFeatures, carvers, 0.8F, false, true, false)
            .mobSpawnSettings(mobs.build())
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SPARSE_JUNGLE))
            .build();
    }

    public static Biome jungle(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 40, new MobSpawnSettings.SpawnerData(EntityTypes.PARROT, 1, 2))
            .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityTypes.OCELOT, 1, 3))
            .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.PANDA, 1, 2));
        return baseJungle(placedFeatures, carvers, 0.9F, false, false, true)
            .mobSpawnSettings(mobs.build())
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_JUNGLE))
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .build();
    }

    public static Biome bambooJungle(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 40, new MobSpawnSettings.SpawnerData(EntityTypes.PARROT, 1, 2))
            .addSpawn(MobCategory.CREATURE, 80, new MobSpawnSettings.SpawnerData(EntityTypes.PANDA, 1, 2))
            .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityTypes.OCELOT, 1, 1));
        return baseJungle(placedFeatures, carvers, 0.9F, true, false, true)
            .mobSpawnSettings(mobs.build())
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_BAMBOO_JUNGLE))
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .build();
    }

    private static Biome.BiomeBuilder baseJungle(
        final HolderGetter<PlacedFeature> placedFeatures,
        final HolderGetter<ConfiguredWorldCarver<?>> carvers,
        final float downfall,
        final boolean bamboo,
        final boolean sparse,
        final boolean core
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (bamboo) {
            BiomeDefaultFeatures.addBambooVegetation(generation);
        } else {
            if (core) {
                BiomeDefaultFeatures.addLightBambooVegetation(generation);
            }

            if (sparse) {
                BiomeDefaultFeatures.addSparseJungleTrees(generation);
            } else {
                BiomeDefaultFeatures.addJungleTrees(generation);
            }
        }

        BiomeDefaultFeatures.addWarmFlowers(generation);
        BiomeDefaultFeatures.addJungleGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        BiomeDefaultFeatures.addJungleVines(generation);
        if (sparse) {
            BiomeDefaultFeatures.addSparseJungleMelons(generation);
        } else {
            BiomeDefaultFeatures.addJungleMelons(generation);
        }

        return baseBiome(0.95F, downfall).generationSettings(generation.build());
    }

    public static Biome windsweptHills(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean moreTrees
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.LLAMA, 4, 6));
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (moreTrees) {
            BiomeDefaultFeatures.addMountainForestTrees(generation);
        } else {
            BiomeDefaultFeatures.addMountainTrees(generation);
        }

        BiomeDefaultFeatures.addBushes(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        return baseBiome(0.2F, 0.3F).mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build();
    }

    public static Biome desert(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.desertSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addFossilDecoration(generation);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDesertVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDesertExtraVegetation(generation);
        BiomeDefaultFeatures.addDesertExtraDecoration(generation);
        return baseBiome(2.0F, 0.0F)
            .hasPrecipitation(false)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DESERT))
            .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome plains(
        final HolderGetter<PlacedFeature> placedFeatures,
        final HolderGetter<ConfiguredWorldCarver<?>> carvers,
        final boolean sunflower,
        final boolean snowy,
        final boolean spikes
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        if (snowy) {
            mobs.creatureGenerationProbability(0.07F);
            BiomeDefaultFeatures.snowySpawns(mobs, !spikes);
            if (spikes) {
                generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
                generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
            }
        } else {
            BiomeDefaultFeatures.plainsSpawns(mobs);
            BiomeDefaultFeatures.addPlainGrass(generation);
            if (sunflower) {
                generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
            } else {
                BiomeDefaultFeatures.addBushes(generation);
            }
        }

        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (snowy) {
            BiomeDefaultFeatures.addSnowyTrees(generation);
            BiomeDefaultFeatures.addDefaultFlowers(generation);
            BiomeDefaultFeatures.addDefaultGrass(generation);
        } else {
            BiomeDefaultFeatures.addPlainVegetation(generation);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        return baseBiome(snowy ? 0.0F : 0.8F, snowy ? 0.5F : 0.4F).mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build();
    }

    public static Biome mushroomFields(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.mooshroomSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addMushroomFieldVegetation(generation);
        BiomeDefaultFeatures.addNearWaterVegetation(generation);
        return baseBiome(0.9F, 1.0F)
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .setAttribute(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, false)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome savanna(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean shattered, final boolean plateau
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        if (!shattered) {
            BiomeDefaultFeatures.addSavannaGrass(generation);
        }

        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (shattered) {
            BiomeDefaultFeatures.addShatteredSavannaTrees(generation);
            BiomeDefaultFeatures.addDefaultFlowers(generation);
            BiomeDefaultFeatures.addShatteredSavannaGrass(generation);
        } else {
            BiomeDefaultFeatures.addSavannaTrees(generation);
            BiomeDefaultFeatures.addWarmFlowers(generation);
            BiomeDefaultFeatures.addSavannaExtraGrass(generation);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.HORSE, 2, 6))
            .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.DONKEY, 1, 1))
            .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.ARMADILLO, 2, 3));
        BiomeDefaultFeatures.commonSpawnWithZombieHorse(mobs);
        if (plateau) {
            mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.LLAMA, 4, 4));
            mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 8));
        }

        return baseBiome(2.0F, 0.0F)
            .hasPrecipitation(false)
            .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome badlands(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean wooded) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        BiomeDefaultFeatures.commonSpawns(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 6, new MobSpawnSettings.SpawnerData(EntityTypes.ARMADILLO, 1, 2));
        mobs.creatureGenerationProbability(0.03F);
        if (wooded) {
            mobs.addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 8));
            mobs.creatureGenerationProbability(0.04F);
        }

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addExtraGold(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (wooded) {
            BiomeDefaultFeatures.addBadlandsTrees(generation);
        }

        BiomeDefaultFeatures.addBadlandGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addBadlandExtraVegetation(generation);
        return baseBiome(2.0F, 0.0F)
            .hasPrecipitation(false)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_BADLANDS))
            .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).foliageColorOverride(10387789).grassColorOverride(9470285).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    private static Biome.BiomeBuilder baseOcean() {
        return baseBiome(0.5F, 0.5F).setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD.withUnderwater(Musics.UNDER_WATER));
    }

    private static BiomeGenerationSettings.Builder baseOceanGeneration(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addWaterTrees(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        return generation;
    }

    public static Biome coldOcean(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean deep) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.oceanSpawns(mobs, 3, 4, 15);
        mobs.addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityTypes.SALMON, 1, 5));
        mobs.addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.NAUTILUS, 1, 1));
        BiomeGenerationSettings.Builder generation = baseOceanGeneration(placedFeatures, carvers);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD);
        BiomeDefaultFeatures.addColdOceanExtraVegetation(generation);
        return baseOcean()
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4020182).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome ocean(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean deep) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.oceanSpawns(mobs, 1, 4, 10);
        mobs.addSpawn(MobCategory.WATER_CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.DOLPHIN, 1, 2))
            .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.NAUTILUS, 1, 1));
        BiomeGenerationSettings.Builder generation = baseOceanGeneration(placedFeatures, carvers);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL);
        BiomeDefaultFeatures.addColdOceanExtraVegetation(generation);
        return baseOcean().mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build();
    }

    public static Biome lukeWarmOcean(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean deep
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        if (deep) {
            BiomeDefaultFeatures.oceanSpawns(mobs, 8, 4, 8);
        } else {
            BiomeDefaultFeatures.oceanSpawns(mobs, 10, 2, 15);
        }

        mobs.addSpawn(MobCategory.WATER_AMBIENT, 5, new MobSpawnSettings.SpawnerData(EntityTypes.PUFFERFISH, 1, 3))
            .addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityTypes.TROPICAL_FISH, 8, 8))
            .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.DOLPHIN, 1, 2))
            .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.NAUTILUS, 1, 1));
        BiomeGenerationSettings.Builder generation = baseOceanGeneration(placedFeatures, carvers);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM);
        BiomeDefaultFeatures.addLukeWarmKelp(generation);
        return baseOcean()
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -16509389)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4566514).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome warmOcean(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
            .addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityTypes.PUFFERFISH, 1, 3))
            .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.NAUTILUS, 1, 1));
        BiomeDefaultFeatures.warmOceanSpawns(mobs, 10, 4);
        BiomeGenerationSettings.Builder generation = baseOceanGeneration(placedFeatures, carvers)
            .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION)
            .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM)
            .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
        return baseOcean()
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -16507085)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4445678).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome frozenOcean(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean deep) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
            .addSpawn(MobCategory.WATER_CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 1, 4))
            .addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityTypes.SALMON, 1, 5))
            .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.POLAR_BEAR, 1, 2))
            .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.NAUTILUS, 1, 1));
        BiomeDefaultFeatures.commonSpawns(mobs);
        mobs.addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(EntityTypes.DROWNED, 1, 1));
        float temperature = deep ? 0.5F : 0.0F;
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addIcebergs(generation);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addBlueIce(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addWaterTrees(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        return baseBiome(temperature, 0.5F)
            .temperatureAdjustment(Biome.TemperatureModifier.FROZEN)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(3750089).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome forest(
        final HolderGetter<PlacedFeature> placedFeatures,
        final HolderGetter<ConfiguredWorldCarver<?>> carvers,
        final boolean birch,
        final boolean tall,
        final boolean flower
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BackgroundMusic music;
        if (flower) {
            music = new BackgroundMusic(SoundEvents.MUSIC_BIOME_FLOWER_FOREST);
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
        } else {
            music = new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST);
            BiomeDefaultFeatures.addForestFlowers(generation);
        }

        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (flower) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
            BiomeDefaultFeatures.addDefaultGrass(generation);
        } else {
            if (birch) {
                BiomeDefaultFeatures.addBirchForestFlowers(generation);
                if (tall) {
                    BiomeDefaultFeatures.addTallBirchTrees(generation);
                } else {
                    BiomeDefaultFeatures.addBirchTrees(generation);
                }
            } else {
                BiomeDefaultFeatures.addOtherBirchTrees(generation);
            }

            BiomeDefaultFeatures.addBushes(generation);
            BiomeDefaultFeatures.addDefaultFlowers(generation);
            BiomeDefaultFeatures.addForestGrass(generation);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        BiomeDefaultFeatures.commonSpawns(mobs);
        if (flower) {
            mobs.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 3));
        } else if (!birch) {
            mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 4));
        }

        return baseBiome(birch ? 0.6F : 0.7F, birch ? 0.6F : 0.8F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, music)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome taiga(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean snowy) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 4))
            .addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 3))
            .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.FOX, 2, 4));
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addFerns(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addTaigaTrees(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addTaigaGrass(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        if (snowy) {
            BiomeDefaultFeatures.addRareBerryBushes(generation);
        } else {
            BiomeDefaultFeatures.addCommonBerryBushes(generation);
        }

        int waterColor = snowy ? 4020182 : 4159204;
        return baseBiome(snowy ? -0.5F : 0.25F, snowy ? 0.4F : 0.8F)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(waterColor).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome darkForest(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean isPaleGarden
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        if (!isPaleGarden) {
            BiomeDefaultFeatures.farmAnimals(mobs);
        }

        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        generation.addFeature(
            GenerationStep.Decoration.VEGETAL_DECORATION,
            isPaleGarden ? VegetationPlacements.PALE_GARDEN_VEGETATION : VegetationPlacements.DARK_FOREST_VEGETATION
        );
        if (!isPaleGarden) {
            BiomeDefaultFeatures.addForestFlowers(generation);
        } else {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PALE_MOSS_PATCH);
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PALE_GARDEN_FLOWERS);
        }

        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (!isPaleGarden) {
            BiomeDefaultFeatures.addDefaultFlowers(generation);
        } else {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_PALE_GARDEN);
        }

        BiomeDefaultFeatures.addForestGrass(generation);
        if (!isPaleGarden) {
            BiomeDefaultFeatures.addDefaultMushrooms(generation);
            BiomeDefaultFeatures.addLeafLitterPatch(generation);
        }

        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        EnvironmentAttributeMap paleGardenAttributes = EnvironmentAttributeMap.builder()
            .set(EnvironmentAttributes.SKY_COLOR, -4605511)
            .set(EnvironmentAttributes.FOG_COLOR, -8292496)
            .set(EnvironmentAttributes.WATER_FOG_COLOR, -11179648)
            .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.EMPTY)
            .set(EnvironmentAttributes.MUSIC_VOLUME, 0.0F)
            .build();
        EnvironmentAttributeMap darkForestAttributes = EnvironmentAttributeMap.builder()
            .set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
            .build();
        return baseBiome(0.7F, 0.8F)
            .putAttributes(isPaleGarden ? paleGardenAttributes : darkForestAttributes)
            .specialEffects(
                isPaleGarden
                    ? new BiomeSpecialEffects.Builder()
                        .waterColor(7768221)
                        .grassColorOverride(7832178)
                        .foliageColorOverride(8883574)
                        .dryFoliageColorOverride(10528412)
                        .build()
                    : new BiomeSpecialEffects.Builder()
                        .waterColor(4159204)
                        .dryFoliageColorOverride(8082228)
                        .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
                        .build()
            )
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome swamp(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        BiomeDefaultFeatures.swampSpawns(mobs, 70);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addFossilDecoration(generation);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addSwampClayDisk(generation);
        BiomeDefaultFeatures.addSwampVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addSwampExtraVegetation(generation);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
        return baseBiome(0.8F, 0.9F)
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -14474473)
            .modifyAttribute(EnvironmentAttributes.WATER_FOG_END_DISTANCE, FloatModifier.MULTIPLY, 0.85F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SWAMP))
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .waterColor(6388580)
                    .foliageColorOverride(6975545)
                    .dryFoliageColorOverride(8082228)
                    .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
                    .build()
            )
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome mangroveSwamp(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.swampSpawns(mobs, 70);
        mobs.addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityTypes.TROPICAL_FISH, 8, 8));
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addFossilDecoration(generation);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addMangroveSwampDisks(generation);
        BiomeDefaultFeatures.addMangroveSwampVegetation(generation);
        BiomeDefaultFeatures.addMangroveSwampExtraVegetation(generation);
        return baseBiome(0.8F, 0.9F)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, -4138753)
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -11699616)
            .modifyAttribute(EnvironmentAttributes.WATER_FOG_END_DISTANCE, FloatModifier.MULTIPLY, 0.85F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SWAMP))
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .waterColor(3832426)
                    .foliageColorOverride(9285927)
                    .dryFoliageColorOverride(8082228)
                    .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
                    .build()
            )
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome river(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean frozen) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
            .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 1, 4))
            .addSpawn(MobCategory.WATER_AMBIENT, 5, new MobSpawnSettings.SpawnerData(EntityTypes.SALMON, 1, 5));
        BiomeDefaultFeatures.commonSpawns(mobs);
        mobs.addSpawn(MobCategory.MONSTER, frozen ? 1 : 100, new MobSpawnSettings.SpawnerData(EntityTypes.DROWNED, 1, 1));
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addWaterTrees(generation);
        BiomeDefaultFeatures.addBushes(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        if (!frozen) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
        }

        return baseBiome(frozen ? 0.0F : 0.5F, 0.5F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD.withUnderwater(Musics.UNDER_WATER))
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(frozen ? 3750089 : 4159204).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome beach(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean snowy, final boolean stony
    ) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        boolean sandy = !stony && !snowy;
        if (sandy) {
            mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.TURTLE, 2, 5));
        }

        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        float temperature;
        if (snowy) {
            temperature = 0.05F;
        } else if (stony) {
            temperature = 0.2F;
        } else {
            temperature = 0.8F;
        }

        int waterColor = snowy ? 4020182 : 4159204;
        return baseBiome(temperature, sandy ? 0.4F : 0.3F)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(waterColor).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome theVoid(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
        return baseBiome(0.5F, 0.5F)
            .hasPrecipitation(false)
            .mobSpawnSettings(new MobSpawnSettings.Builder().build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome meadowOrCherryGrove(
        final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers, final boolean cherryGrove
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(cherryGrove ? EntityTypes.PIG : EntityTypes.DONKEY, 1, 2))
            .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 6))
            .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityTypes.SHEEP, 2, 4));
        BiomeDefaultFeatures.commonSpawns(mobs);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        if (cherryGrove) {
            BiomeDefaultFeatures.addCherryGroveVegetation(generation);
        } else {
            BiomeDefaultFeatures.addMeadowVegetation(generation);
        }

        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        if (cherryGrove) {
            BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(6141935)
                .grassColorOverride(11983713)
                .foliageColorOverride(11983713);
            return baseBiome(0.5F, 0.8F)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -10635281)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_CHERRY_GROVE))
                .specialEffects(effects.build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
        } else {
            return baseBiome(0.5F, 0.8F)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_MEADOW))
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(937679).build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
        }
    }

    private static Biome.BiomeBuilder basePeaks(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.GOAT, 1, 3));
        BiomeDefaultFeatures.commonSpawns(mobs);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addFrozenSprings(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        return baseBiome(-0.7F, 0.9F)
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build());
    }

    public static Biome frozenPeaks(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        return basePeaks(placedFeatures, carvers)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS))
            .build();
    }

    public static Biome jaggedPeaks(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        return basePeaks(placedFeatures, carvers)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_JAGGED_PEAKS))
            .build();
    }

    public static Biome stonyPeaks(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(mobs);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        return baseBiome(1.0F, 0.3F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS))
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome snowySlopes(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 3))
            .addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.GOAT, 1, 3));
        BiomeDefaultFeatures.commonSpawns(mobs);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addFrozenSprings(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, false);
        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        return baseBiome(-0.3F, 0.9F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SNOWY_SLOPES))
            .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome grove(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 1, 1))
            .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.RABBIT, 2, 3))
            .addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityTypes.FOX, 2, 4));
        BiomeDefaultFeatures.commonSpawns(mobs);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addFrozenSprings(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addGroveTrees(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, false);
        BiomeDefaultFeatures.addExtraEmeralds(generation);
        BiomeDefaultFeatures.addInfestedStone(generation);
        return baseBiome(-0.2F, 0.8F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_GROVE))
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome sulfurCaves(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.AMBIENT, 10, new MobSpawnSettings.SpawnerData(EntityTypes.BAT, 8, 8));
        mobs.addSpawn(MobCategory.MONSTER, 100, new MobSpawnSettings.SpawnerData(EntityTypes.SULFUR_CUBE, 2, 4));
        mobs.addSpawn(MobCategory.MONSTER, 50, new MobSpawnSettings.SpawnerData(EntityTypes.CREEPER, 2, 2));
        mobs.addSpawn(MobCategory.MONSTER, 50, new MobSpawnSettings.SpawnerData(EntityTypes.SKELETON, 2, 2));
        mobs.addSpawn(MobCategory.MONSTER, 25, new MobSpawnSettings.SpawnerData(EntityTypes.SLIME, 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 20, new MobSpawnSettings.SpawnerData(EntityTypes.CAVE_SPIDER, 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 50, new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE, 2, 2));
        mobs.addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.ENDERMAN, 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 1, new MobSpawnSettings.SpawnerData(EntityTypes.WITCH, 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE_VILLAGER, 1, 1));
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addSulfurCavesFeatures(generation);
        return baseBiome(0.8F, 0.4F)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, -7555023)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SULFUR_CAVES))
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -15248324)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(-13320311).grassColorOverride(11249231).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome lushCaves(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.AXOLOTLS, 10, new MobSpawnSettings.SpawnerData(EntityTypes.AXOLOTL, 4, 6));
        mobs.addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityTypes.TROPICAL_FISH, 8, 8));
        BiomeDefaultFeatures.commonSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addLushCavesSpecialOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addLushCavesVegetationFeatures(generation);
        return baseBiome(0.5F, 0.5F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES))
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome dripstoneCaves(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.dripstoneCavesSpawns(mobs);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation, true);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addPlainVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, false);
        BiomeDefaultFeatures.addDripstone(generation);
        return baseBiome(0.8F, 0.4F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES))
            .mobSpawnSettings(mobs.build())
            .generationSettings(generation.build())
            .build();
    }

    public static Biome deepDark(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder noMobs = new MobSpawnSettings.Builder();
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addCarver(Carvers.CAVE);
        generation.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        generation.addCarver(Carvers.CANYON);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addPlainVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, false);
        BiomeDefaultFeatures.addSculk(generation);
        return baseBiome(0.8F, 0.4F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DEEP_DARK))
            .mobSpawnSettings(noMobs.build())
            .generationSettings(generation.build())
            .build();
    }
}