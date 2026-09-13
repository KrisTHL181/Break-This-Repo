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
import net.minecraft.world.entity.EntityType;
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

   public static int calculateSkyColor(float p_194844_) {
      float $$1 = p_194844_ / 3.0F;
      $$1 = Mth.clamp($$1, -1.0F, 1.0F);
      return ARGB.opaque(Mth.hsvToRgb(0.62222224F - $$1 * 0.05F, 0.5F + $$1 * 0.1F, 1.0F));
   }

   private static Biome.BiomeBuilder baseBiome(float p_453538_, float p_455956_) {
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(p_453538_)
         .downfall(p_455956_)
         .setAttribute(EnvironmentAttributes.SKY_COLOR, calculateSkyColor(p_453538_))
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build());
   }

   private static void globalOverworldGeneration(BiomeGenerationSettings.Builder p_194870_) {
      BiomeDefaultFeatures.addDefaultCarversAndLakes(p_194870_);
      BiomeDefaultFeatures.addDefaultCrystalFormations(p_194870_);
      BiomeDefaultFeatures.addDefaultMonsterRoom(p_194870_);
      BiomeDefaultFeatures.addDefaultUndergroundVariety(p_194870_);
      BiomeDefaultFeatures.addDefaultSprings(p_194870_);
      BiomeDefaultFeatures.addSurfaceFreezing(p_194870_);
   }

   public static Biome oldGrowthTaiga(HolderGetter<PlacedFeature> p_255849_, HolderGetter<ConfiguredWorldCarver<?>> p_256578_, boolean p_194877_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 4));
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3));
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.FOX, 2, 4));
      if (p_194877_) {
         BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      } else {
         BiomeDefaultFeatures.caveSpawns(mobspawnsettings$builder);
         BiomeDefaultFeatures.monsters(mobspawnsettings$builder, 100, 25, 0, 100, false);
      }

      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255849_, p_256578_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addMossyStoneBlock(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addFerns(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      biomegenerationsettings$builder.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION,
         p_194877_ ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA
      );
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addGiantTaigaVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      BiomeDefaultFeatures.addCommonBerryBushes(biomegenerationsettings$builder);
      return baseBiome(p_194877_ ? 0.25F : 0.3F, 0.8F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_OLD_GROWTH_TAIGA))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome sparseJungle(HolderGetter<PlacedFeature> p_255977_, HolderGetter<ConfiguredWorldCarver<?>> p_256531_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 2, 4));
      return baseJungle(p_255977_, p_256531_, 0.8F, false, true, false)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SPARSE_JUNGLE))
         .build();
   }

   public static Biome jungle(HolderGetter<PlacedFeature> p_256033_, HolderGetter<ConfiguredWorldCarver<?>> p_255651_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 40, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 1, 2))
         .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 1, 3))
         .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 1, 2));
      return baseJungle(p_256033_, p_255651_, 0.9F, false, false, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_JUNGLE))
         .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
         .build();
   }

   public static Biome bambooJungle(HolderGetter<PlacedFeature> p_255817_, HolderGetter<ConfiguredWorldCarver<?>> p_256096_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 40, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 1, 2))
         .addSpawn(MobCategory.CREATURE, 80, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 1, 2))
         .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 1, 1));
      return baseJungle(p_255817_, p_256096_, 0.9F, true, false, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_BAMBOO_JUNGLE))
         .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
         .build();
   }

   private static Biome.BiomeBuilder baseJungle(
      HolderGetter<PlacedFeature> p_285208_,
      HolderGetter<ConfiguredWorldCarver<?>> p_285276_,
      float p_285079_,
      boolean p_285393_,
      boolean p_285109_,
      boolean p_285122_
   ) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_285208_, p_285276_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_285393_) {
         BiomeDefaultFeatures.addBambooVegetation(biomegenerationsettings$builder);
      } else {
         if (p_285122_) {
            BiomeDefaultFeatures.addLightBambooVegetation(biomegenerationsettings$builder);
         }

         if (p_285109_) {
            BiomeDefaultFeatures.addSparseJungleTrees(biomegenerationsettings$builder);
         } else {
            BiomeDefaultFeatures.addJungleTrees(biomegenerationsettings$builder);
         }
      }

      BiomeDefaultFeatures.addWarmFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addJungleGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      BiomeDefaultFeatures.addJungleVines(biomegenerationsettings$builder);
      if (p_285109_) {
         BiomeDefaultFeatures.addSparseJungleMelons(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addJungleMelons(biomegenerationsettings$builder);
      }

      return baseBiome(0.95F, p_285079_).generationSettings(biomegenerationsettings$builder.build());
   }

   public static Biome windsweptHills(HolderGetter<PlacedFeature> p_255703_, HolderGetter<ConfiguredWorldCarver<?>> p_256239_, boolean p_194887_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 4, 6));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255703_, p_256239_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_194887_) {
         BiomeDefaultFeatures.addMountainForestTrees(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addMountainTrees(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addBushes(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      return baseBiome(0.2F, 0.3F).mobSpawnSettings(mobspawnsettings$builder.build()).generationSettings(biomegenerationsettings$builder.build()).build();
   }

   public static Biome desert(HolderGetter<PlacedFeature> p_256064_, HolderGetter<ConfiguredWorldCarver<?>> p_255852_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.desertSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256064_, p_255852_);
      BiomeDefaultFeatures.addFossilDecoration(biomegenerationsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDesertVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDesertExtraVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDesertExtraDecoration(biomegenerationsettings$builder);
      return baseBiome(2.0F, 0.0F)
         .hasPrecipitation(false)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DESERT))
         .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome plains(
      HolderGetter<PlacedFeature> p_256382_, HolderGetter<ConfiguredWorldCarver<?>> p_256173_, boolean p_194882_, boolean p_194883_, boolean p_194884_
   ) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256382_, p_256173_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      if (p_194883_) {
         mobspawnsettings$builder.creatureGenerationProbability(0.07F);
         BiomeDefaultFeatures.snowySpawns(mobspawnsettings$builder, !p_194884_);
         if (p_194884_) {
            biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
            biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
         }
      } else {
         BiomeDefaultFeatures.plainsSpawns(mobspawnsettings$builder);
         BiomeDefaultFeatures.addPlainGrass(biomegenerationsettings$builder);
         if (p_194882_) {
            biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
         } else {
            BiomeDefaultFeatures.addBushes(biomegenerationsettings$builder);
         }
      }

      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_194883_) {
         BiomeDefaultFeatures.addSnowyTrees(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addPlainVegetation(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      return baseBiome(p_194883_ ? 0.0F : 0.8F, p_194883_ ? 0.5F : 0.4F)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome mushroomFields(HolderGetter<PlacedFeature> p_255775_, HolderGetter<ConfiguredWorldCarver<?>> p_256480_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.mooshroomSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255775_, p_256480_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addMushroomFieldVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addNearWaterVegetation(biomegenerationsettings$builder);
      return baseBiome(0.9F, 1.0F)
         .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
         .setAttribute(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, false)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome savanna(HolderGetter<PlacedFeature> p_256294_, HolderGetter<ConfiguredWorldCarver<?>> p_256583_, boolean p_194879_, boolean p_194880_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256294_, p_256583_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      if (!p_194879_) {
         BiomeDefaultFeatures.addSavannaGrass(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_194879_) {
         BiomeDefaultFeatures.addShatteredSavannaTrees(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addShatteredSavannaGrass(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addSavannaTrees(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addWarmFlowers(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addSavannaExtraGrass(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.HORSE, 2, 6))
         .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1))
         .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityType.ARMADILLO, 2, 3));
      BiomeDefaultFeatures.commonSpawnWithZombieHorse(mobspawnsettings$builder);
      if (p_194880_) {
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 4, 4));
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 8));
      }

      return baseBiome(2.0F, 0.0F)
         .hasPrecipitation(false)
         .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome badlands(HolderGetter<PlacedFeature> p_256309_, HolderGetter<ConfiguredWorldCarver<?>> p_256430_, boolean p_194897_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 6, new MobSpawnSettings.SpawnerData(EntityType.ARMADILLO, 1, 2));
      mobspawnsettings$builder.creatureGenerationProbability(0.03F);
      if (p_194897_) {
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 8));
         mobspawnsettings$builder.creatureGenerationProbability(0.04F);
      }

      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256309_, p_256430_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addExtraGold(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_194897_) {
         BiomeDefaultFeatures.addBadlandsTrees(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addBadlandGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addBadlandExtraVegetation(biomegenerationsettings$builder);
      return baseBiome(2.0F, 0.0F)
         .hasPrecipitation(false)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_BADLANDS))
         .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).foliageColorOverride(10387789).grassColorOverride(9470285).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   private static Biome.BiomeBuilder baseOcean() {
      return baseBiome(0.5F, 0.5F).setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD.withUnderwater(Musics.UNDER_WATER));
   }

   private static BiomeGenerationSettings.Builder baseOceanGeneration(HolderGetter<PlacedFeature> p_256289_, HolderGetter<ConfiguredWorldCarver<?>> p_256514_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256289_, p_256514_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addWaterTrees(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      return biomegenerationsettings$builder;
   }

   public static Biome coldOcean(HolderGetter<PlacedFeature> p_256141_, HolderGetter<ConfiguredWorldCarver<?>> p_255841_, boolean p_194900_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns(mobspawnsettings$builder, 3, 4, 15);
      mobspawnsettings$builder.addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 1, 5));
      mobspawnsettings$builder.addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.NAUTILUS, 1, 1));
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = baseOceanGeneration(p_256141_, p_255841_);
      biomegenerationsettings$builder.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION, p_194900_ ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD
      );
      BiomeDefaultFeatures.addColdOceanExtraVegetation(biomegenerationsettings$builder);
      return baseOcean()
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4020182).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome ocean(HolderGetter<PlacedFeature> p_256265_, HolderGetter<ConfiguredWorldCarver<?>> p_256537_, boolean p_255752_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns(mobspawnsettings$builder, 1, 4, 10);
      mobspawnsettings$builder.addSpawn(MobCategory.WATER_CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 1, 2))
         .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.NAUTILUS, 1, 1));
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = baseOceanGeneration(p_256265_, p_256537_);
      biomegenerationsettings$builder.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION, p_255752_ ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL
      );
      BiomeDefaultFeatures.addColdOceanExtraVegetation(biomegenerationsettings$builder);
      return baseOcean().mobSpawnSettings(mobspawnsettings$builder.build()).generationSettings(biomegenerationsettings$builder.build()).build();
   }

   public static Biome lukeWarmOcean(HolderGetter<PlacedFeature> p_255660_, HolderGetter<ConfiguredWorldCarver<?>> p_256231_, boolean p_194906_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      if (p_194906_) {
         BiomeDefaultFeatures.oceanSpawns(mobspawnsettings$builder, 8, 4, 8);
      } else {
         BiomeDefaultFeatures.oceanSpawns(mobspawnsettings$builder, 10, 2, 15);
      }

      mobspawnsettings$builder.addSpawn(MobCategory.WATER_AMBIENT, 5, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 1, 3))
         .addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 8, 8))
         .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 1, 2))
         .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.NAUTILUS, 1, 1));
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = baseOceanGeneration(p_255660_, p_256231_);
      biomegenerationsettings$builder.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION, p_194906_ ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM
      );
      BiomeDefaultFeatures.addLukeWarmKelp(biomegenerationsettings$builder);
      return baseOcean()
         .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -16509389)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4566514).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome warmOcean(HolderGetter<PlacedFeature> p_256477_, HolderGetter<ConfiguredWorldCarver<?>> p_256024_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 1, 3))
         .addSpawn(MobCategory.WATER_CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.NAUTILUS, 1, 1));
      BiomeDefaultFeatures.warmOceanSpawns(mobspawnsettings$builder, 10, 4);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = baseOceanGeneration(p_256477_, p_256024_)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
      return baseOcean()
         .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -16507085)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4445678).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome frozenOcean(HolderGetter<PlacedFeature> p_256482_, HolderGetter<ConfiguredWorldCarver<?>> p_256660_, boolean p_194909_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 1, 5))
         .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.POLAR_BEAR, 1, 2))
         .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.NAUTILUS, 1, 1));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 1, 1));
      float f = p_194909_ ? 0.5F : 0.0F;
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256482_, p_256660_);
      BiomeDefaultFeatures.addIcebergs(biomegenerationsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addBlueIce(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addWaterTrees(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      return baseBiome(f, 0.5F)
         .temperatureAdjustment(Biome.TemperatureModifier.FROZEN)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(3750089).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome forest(
      HolderGetter<PlacedFeature> p_255788_, HolderGetter<ConfiguredWorldCarver<?>> p_256461_, boolean p_194892_, boolean p_194893_, boolean p_194894_
   ) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255788_, p_256461_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BackgroundMusic backgroundmusic;
      if (p_194894_) {
         backgroundmusic = new BackgroundMusic(SoundEvents.MUSIC_BIOME_FLOWER_FOREST);
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
      } else {
         backgroundmusic = new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST);
         BiomeDefaultFeatures.addForestFlowers(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_194894_) {
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
         BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      } else {
         if (p_194892_) {
            BiomeDefaultFeatures.addBirchForestFlowers(biomegenerationsettings$builder);
            if (p_194893_) {
               BiomeDefaultFeatures.addTallBirchTrees(biomegenerationsettings$builder);
            } else {
               BiomeDefaultFeatures.addBirchTrees(biomegenerationsettings$builder);
            }
         } else {
            BiomeDefaultFeatures.addOtherBirchTrees(biomegenerationsettings$builder);
         }

         BiomeDefaultFeatures.addBushes(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addForestGrass(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      if (p_194894_) {
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3));
      } else if (!p_194892_) {
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 4));
      }

      return baseBiome(p_194892_ ? 0.6F : 0.7F, p_194892_ ? 0.6F : 0.8F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, backgroundmusic)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome taiga(HolderGetter<PlacedFeature> p_256177_, HolderGetter<ConfiguredWorldCarver<?>> p_255727_, boolean p_194912_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 4))
         .addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3))
         .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.FOX, 2, 4));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256177_, p_255727_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addFerns(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addTaigaTrees(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addTaigaGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      if (p_194912_) {
         BiomeDefaultFeatures.addRareBerryBushes(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addCommonBerryBushes(biomegenerationsettings$builder);
      }

      int i = p_194912_ ? 4020182 : 4159204;
      return baseBiome(p_194912_ ? -0.5F : 0.25F, p_194912_ ? 0.4F : 0.8F)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(i).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome darkForest(HolderGetter<PlacedFeature> p_256140_, HolderGetter<ConfiguredWorldCarver<?>> p_256223_, boolean p_367219_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      if (!p_367219_) {
         BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      }

      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256140_, p_256223_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      biomegenerationsettings$builder.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION, p_367219_ ? VegetationPlacements.PALE_GARDEN_VEGETATION : VegetationPlacements.DARK_FOREST_VEGETATION
      );
      if (!p_367219_) {
         BiomeDefaultFeatures.addForestFlowers(biomegenerationsettings$builder);
      } else {
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PALE_MOSS_PATCH);
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PALE_GARDEN_FLOWERS);
      }

      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (!p_367219_) {
         BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      } else {
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_PALE_GARDEN);
      }

      BiomeDefaultFeatures.addForestGrass(biomegenerationsettings$builder);
      if (!p_367219_) {
         BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
         BiomeDefaultFeatures.addLeafLitterPatch(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      EnvironmentAttributeMap environmentattributemap = EnvironmentAttributeMap.builder()
         .set(EnvironmentAttributes.SKY_COLOR, -4605511)
         .set(EnvironmentAttributes.FOG_COLOR, -8292496)
         .set(EnvironmentAttributes.WATER_FOG_COLOR, -11179648)
         .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.EMPTY)
         .set(EnvironmentAttributes.MUSIC_VOLUME, 0.0F)
         .build();
      EnvironmentAttributeMap environmentattributemap1 = EnvironmentAttributeMap.builder()
         .set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
         .build();
      return baseBiome(0.7F, 0.8F)
         .putAttributes(p_367219_ ? environmentattributemap : environmentattributemap1)
         .specialEffects(
            p_367219_
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
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome swamp(HolderGetter<PlacedFeature> p_256058_, HolderGetter<ConfiguredWorldCarver<?>> p_256016_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(mobspawnsettings$builder);
      BiomeDefaultFeatures.swampSpawns(mobspawnsettings$builder, 70);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256058_, p_256016_);
      BiomeDefaultFeatures.addFossilDecoration(biomegenerationsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addSwampClayDisk(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addSwampVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addSwampExtraVegetation(biomegenerationsettings$builder);
      biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
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
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome mangroveSwamp(HolderGetter<PlacedFeature> p_256353_, HolderGetter<ConfiguredWorldCarver<?>> p_256103_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.swampSpawns(mobspawnsettings$builder, 70);
      mobspawnsettings$builder.addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 8, 8));
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256353_, p_256103_);
      BiomeDefaultFeatures.addFossilDecoration(biomegenerationsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addMangroveSwampDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addMangroveSwampVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addMangroveSwampExtraVegetation(biomegenerationsettings$builder);
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
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome river(HolderGetter<PlacedFeature> p_256613_, HolderGetter<ConfiguredWorldCarver<?>> p_256581_, boolean p_194915_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, 5, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 1, 5));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      mobspawnsettings$builder.addSpawn(MobCategory.MONSTER, p_194915_ ? 1 : 100, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 1, 1));
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256613_, p_256581_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addWaterTrees(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addBushes(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      if (!p_194915_) {
         biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
      }

      return baseBiome(p_194915_ ? 0.0F : 0.5F, 0.5F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD.withUnderwater(Musics.UNDER_WATER))
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(p_194915_ ? 3750089 : 4159204).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome beach(HolderGetter<PlacedFeature> p_256157_, HolderGetter<ConfiguredWorldCarver<?>> p_255712_, boolean p_194889_, boolean p_194890_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      boolean flag = !p_194890_ && !p_194889_;
      if (flag) {
         mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 2, 5));
      }

      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256157_, p_255712_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultFlowers(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, true);
      float f;
      if (p_194889_) {
         f = 0.05F;
      } else if (p_194890_) {
         f = 0.2F;
      } else {
         f = 0.8F;
      }

      int i = p_194889_ ? 4020182 : 4159204;
      return baseBiome(f, flag ? 0.4F : 0.3F)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(i).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome theVoid(HolderGetter<PlacedFeature> p_256509_, HolderGetter<ConfiguredWorldCarver<?>> p_256544_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256509_, p_256544_);
      biomegenerationsettings$builder.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
      return baseBiome(0.5F, 0.5F)
         .hasPrecipitation(false)
         .mobSpawnSettings(new MobSpawnSettings.Builder().build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome meadowOrCherryGrove(HolderGetter<PlacedFeature> p_273564_, HolderGetter<ConfiguredWorldCarver<?>> p_273374_, boolean p_273710_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_273564_, p_273374_);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(p_273710_ ? EntityType.PIG : EntityType.DONKEY, 1, 2))
         .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 6))
         .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 4));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      if (p_273710_) {
         BiomeDefaultFeatures.addCherryGroveVegetation(biomegenerationsettings$builder);
      } else {
         BiomeDefaultFeatures.addMeadowVegetation(biomegenerationsettings$builder);
      }

      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      if (p_273710_) {
         BiomeSpecialEffects.Builder biomespecialeffects$builder = new BiomeSpecialEffects.Builder()
            .waterColor(6141935)
            .grassColorOverride(11983713)
            .foliageColorOverride(11983713);
         return baseBiome(0.5F, 0.8F)
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, -10635281)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_CHERRY_GROVE))
            .specialEffects(biomespecialeffects$builder.build())
            .mobSpawnSettings(mobspawnsettings$builder.build())
            .generationSettings(biomegenerationsettings$builder.build())
            .build();
      } else {
         return baseBiome(0.5F, 0.8F)
            .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_MEADOW))
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(937679).build())
            .mobSpawnSettings(mobspawnsettings$builder.build())
            .generationSettings(biomegenerationsettings$builder.build())
            .build();
      }
   }

   private static Biome.BiomeBuilder basePeaks(HolderGetter<PlacedFeature> p_451948_, HolderGetter<ConfiguredWorldCarver<?>> p_460582_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_451948_, p_460582_);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 1, 3));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addFrozenSprings(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      return baseBiome(-0.7F, 0.9F)
         .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build());
   }

   public static Biome frozenPeaks(HolderGetter<PlacedFeature> p_255713_, HolderGetter<ConfiguredWorldCarver<?>> p_256092_) {
      return basePeaks(p_255713_, p_256092_)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS))
         .build();
   }

   public static Biome jaggedPeaks(HolderGetter<PlacedFeature> p_256512_, HolderGetter<ConfiguredWorldCarver<?>> p_255908_) {
      return basePeaks(p_256512_, p_255908_)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_JAGGED_PEAKS))
         .build();
   }

   public static Biome stonyPeaks(HolderGetter<PlacedFeature> p_256490_, HolderGetter<ConfiguredWorldCarver<?>> p_255694_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256490_, p_255694_);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      return baseBiome(1.0F, 0.3F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome snowySlopes(HolderGetter<PlacedFeature> p_255927_, HolderGetter<ConfiguredWorldCarver<?>> p_255982_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255927_, p_255982_);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3))
         .addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 1, 3));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addFrozenSprings(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, false);
      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      return baseBiome(-0.3F, 0.9F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SNOWY_SLOPES))
         .setAttribute(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, true)
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome grove(HolderGetter<PlacedFeature> p_256094_, HolderGetter<ConfiguredWorldCarver<?>> p_256431_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256094_, p_256431_);
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      mobspawnsettings$builder.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 1, 1))
         .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3))
         .addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.FOX, 2, 4));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addFrozenSprings(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addGroveTrees(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, false);
      BiomeDefaultFeatures.addExtraEmeralds(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addInfestedStone(biomegenerationsettings$builder);
      return baseBiome(-0.2F, 0.8F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_GROVE))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome lushCaves(HolderGetter<PlacedFeature> p_255944_, HolderGetter<ConfiguredWorldCarver<?>> p_255654_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      mobspawnsettings$builder.addSpawn(MobCategory.AXOLOTLS, 10, new MobSpawnSettings.SpawnerData(EntityType.AXOLOTL, 4, 6));
      mobspawnsettings$builder.addSpawn(MobCategory.WATER_AMBIENT, 25, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 8, 8));
      BiomeDefaultFeatures.commonSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255944_, p_255654_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addLushCavesSpecialOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addLushCavesVegetationFeatures(biomegenerationsettings$builder);
      return baseBiome(0.5F, 0.5F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome dripstoneCaves(HolderGetter<PlacedFeature> p_256253_, HolderGetter<ConfiguredWorldCarver<?>> p_255644_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.dripstoneCavesSpawns(mobspawnsettings$builder);
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256253_, p_255644_);
      globalOverworldGeneration(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder, true);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, false);
      BiomeDefaultFeatures.addDripstone(biomegenerationsettings$builder);
      return baseBiome(0.8F, 0.4F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }

   public static Biome deepDark(HolderGetter<PlacedFeature> p_256073_, HolderGetter<ConfiguredWorldCarver<?>> p_256212_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256073_, p_256212_);
      biomegenerationsettings$builder.addCarver(Carvers.CAVE);
      biomegenerationsettings$builder.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
      biomegenerationsettings$builder.addCarver(Carvers.CANYON);
      BiomeDefaultFeatures.addDefaultCrystalFormations(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMonsterRoom(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultUndergroundVariety(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addSurfaceFreezing(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainGrass(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultOres(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultSoftDisks(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addPlainVegetation(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultMushrooms(biomegenerationsettings$builder);
      BiomeDefaultFeatures.addDefaultExtraVegetation(biomegenerationsettings$builder, false);
      BiomeDefaultFeatures.addSculk(biomegenerationsettings$builder);
      return baseBiome(0.8F, 0.4F)
         .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_DEEP_DARK))
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(biomegenerationsettings$builder.build())
         .build();
   }
}
