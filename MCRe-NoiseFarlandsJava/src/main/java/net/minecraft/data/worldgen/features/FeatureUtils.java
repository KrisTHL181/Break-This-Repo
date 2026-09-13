package net.minecraft.data.worldgen.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class FeatureUtils {
    public static void bootstrap(final BootstrapContext<ConfiguredFeature<?, ?>> context) {
        AquaticFeatures.bootstrap(context);
        CaveFeatures.bootstrap(context);
        EndFeatures.bootstrap(context);
        MiscOverworldFeatures.bootstrap(context);
        NetherFeatures.bootstrap(context);
        OreFeatures.bootstrap(context);
        PileFeatures.bootstrap(context);
        TreeFeatures.bootstrap(context);
        VegetationFeatures.bootstrap(context);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> createKey(final String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace(name));
    }

    public static void register(
        final BootstrapContext<ConfiguredFeature<?, ?>> context, final ResourceKey<ConfiguredFeature<?, ?>> id, final Feature<NoneFeatureConfiguration> feature
    ) {
        register(context, id, feature, FeatureConfiguration.NONE);
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
        final BootstrapContext<ConfiguredFeature<?, ?>> context, final ResourceKey<ConfiguredFeature<?, ?>> id, final F feature, final FC config
    ) {
        context.register(id, new ConfiguredFeature(feature, config));
    }
}