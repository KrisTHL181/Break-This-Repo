package net.minecraft.data.registries;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.StructureSets;
import net.minecraft.data.worldgen.Structures;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestInstances;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.util.Util;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.SulfurCubeArchetypes;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.entity.animal.cow.CowVariants;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariants;
import net.minecraft.world.entity.animal.pig.PigSoundVariants;
import net.minecraft.world.entity.animal.pig.PigVariants;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.animal.wolf.WolfVariants;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.JukeboxSongs;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfigs;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.timeline.Timelines;

public class VanillaRegistries {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.DIMENSION_TYPE, DimensionTypes::bootstrap)
        .add(Registries.CONFIGURED_CARVER, Carvers::bootstrap)
        .add(Registries.CONFIGURED_FEATURE, FeatureUtils::bootstrap)
        .add(Registries.PLACED_FEATURE, PlacementUtils::bootstrap)
        .add(Registries.STRUCTURE, Structures::bootstrap)
        .add(Registries.STRUCTURE_SET, StructureSets::bootstrap)
        .add(Registries.PROCESSOR_LIST, ProcessorLists::bootstrap)
        .add(Registries.TEMPLATE_POOL, Pools::bootstrap)
        .add(Registries.BIOME, BiomeData::bootstrap)
        .add(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterLists::bootstrap)
        .add(Registries.NOISE, NoiseData::bootstrap)
        .add(Registries.DENSITY_FUNCTION, NoiseRouterData::bootstrap)
        .add(Registries.NOISE_SETTINGS, NoiseGeneratorSettings::bootstrap)
        .add(Registries.WORLD_PRESET, WorldPresets::bootstrap)
        .add(Registries.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPresets::bootstrap)
        .add(Registries.CHAT_TYPE, ChatType::bootstrap)
        .add(Registries.TRIM_PATTERN, TrimPatterns::bootstrap)
        .add(Registries.TRIM_MATERIAL, TrimMaterials::bootstrap)
        .add(Registries.TRIAL_SPAWNER_CONFIG, TrialSpawnerConfigs::bootstrap)
        .add(Registries.WOLF_VARIANT, WolfVariants::bootstrap)
        .add(Registries.WOLF_SOUND_VARIANT, WolfSoundVariants::bootstrap)
        .add(Registries.PAINTING_VARIANT, PaintingVariants::bootstrap)
        .add(Registries.DAMAGE_TYPE, DamageTypes::bootstrap)
        .add(Registries.BANNER_PATTERN, BannerPatterns::bootstrap)
        .add(Registries.ENCHANTMENT, Enchantments::bootstrap)
        .add(Registries.ENCHANTMENT_PROVIDER, VanillaEnchantmentProviders::bootstrap)
        .add(Registries.JUKEBOX_SONG, JukeboxSongs::bootstrap)
        .add(Registries.INSTRUMENT, Instruments::bootstrap)
        .add(Registries.PIG_VARIANT, PigVariants::bootstrap)
        .add(Registries.PIG_SOUND_VARIANT, PigSoundVariants::bootstrap)
        .add(Registries.COW_VARIANT, CowVariants::bootstrap)
        .add(Registries.COW_SOUND_VARIANT, CowSoundVariants::bootstrap)
        .add(Registries.CHICKEN_VARIANT, ChickenVariants::bootstrap)
        .add(Registries.CHICKEN_SOUND_VARIANT, ChickenSoundVariants::bootstrap)
        .add(Registries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariants::bootstrap)
        .add(Registries.SULFUR_CUBE_ARCHETYPE, SulfurCubeArchetypes::bootstrap)
        .add(Registries.TEST_ENVIRONMENT, GameTestEnvironments::bootstrap)
        .add(Registries.TEST_INSTANCE, GameTestInstances::bootstrap)
        .add(Registries.FROG_VARIANT, FrogVariants::bootstrap)
        .add(Registries.CAT_VARIANT, CatVariants::bootstrap)
        .add(Registries.CAT_SOUND_VARIANT, CatSoundVariants::bootstrap)
        .add(Registries.DIALOG, Dialogs::bootstrap)
        .add(Registries.WORLD_CLOCK, WorldClocks::bootstrap)
        .add(Registries.TIMELINE, Timelines::bootstrap)
        .add(Registries.VILLAGER_TRADE, VillagerTrades::bootstrap)
        .add(Registries.TRADE_SET, TradeSets::bootstrap);

    private static void validateThatAllBiomeFeaturesHaveBiomeFilter(final HolderLookup.Provider provider) {
        validateThatAllBiomeFeaturesHaveBiomeFilter(provider.lookupOrThrow(Registries.PLACED_FEATURE), provider.lookupOrThrow(Registries.BIOME));
    }

    public static void validateThatAllBiomeFeaturesHaveBiomeFilter(final HolderGetter<PlacedFeature> placedFeatures, final HolderLookup<Biome> biomes) {
        biomes.listElements().forEach(biome -> {
            Identifier biomeKey = biome.key().identifier();
            List<HolderSet<PlacedFeature>> biomeFeatures = biome.value().getGenerationSettings().features();
            biomeFeatures.stream().flatMap(HolderSet::stream).forEach(feature -> feature.unwrap().ifLeft(key -> {
                Holder.Reference<PlacedFeature> value = placedFeatures.getOrThrow((ResourceKey<PlacedFeature>)key);
                if (!validatePlacedFeature(value.value())) {
                    Util.logAndPauseIfInIde("Placed feature " + key.identifier() + " in biome " + biomeKey + " is missing BiomeFilter.biome()");
                }
            }).ifRight(value -> {
                if (!validatePlacedFeature(value)) {
                    Util.logAndPauseIfInIde("Placed inline feature in biome " + biome + " is missing BiomeFilter.biome()");
                }
            }));
        });
    }

    private static boolean validatePlacedFeature(final PlacedFeature value) {
        return value.placement().contains(BiomeFilter.biome());
    }

    public static HolderLookup.Provider createLookup() {
        RegistryAccess.Frozen staticRegistries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider newRegistries = BUILDER.build(staticRegistries);
        validateThatAllBiomeFeaturesHaveBiomeFilter(newRegistries);
        return newRegistries;
    }
}