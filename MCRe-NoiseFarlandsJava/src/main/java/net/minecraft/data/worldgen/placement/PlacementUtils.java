package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class PlacementUtils {
    public static final PlacementModifier HEIGHTMAP = HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING);
    public static final PlacementModifier HEIGHTMAP_NO_LEAVES = HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    public static final PlacementModifier HEIGHTMAP_TOP_SOLID = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG);
    public static final PlacementModifier HEIGHTMAP_WORLD_SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    public static final PlacementModifier HEIGHTMAP_OCEAN_FLOOR = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR);
    public static final PlacementModifier FULL_RANGE = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top());
    public static final PlacementModifier RANGE_10_10 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(10), VerticalAnchor.belowTop(10));
    public static final PlacementModifier RANGE_8_8 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8));
    public static final PlacementModifier RANGE_4_4 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.belowTop(4));
    public static final PlacementModifier RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT = HeightRangePlacement.uniform(
        VerticalAnchor.bottom(), VerticalAnchor.absolute(256)
    );

    public static void bootstrap(final BootstrapContext<PlacedFeature> context) {
        AquaticPlacements.bootstrap(context);
        CavePlacements.bootstrap(context);
        EndPlacements.bootstrap(context);
        MiscOverworldPlacements.bootstrap(context);
        NetherPlacements.bootstrap(context);
        OrePlacements.bootstrap(context);
        TreePlacements.bootstrap(context);
        VegetationPlacements.bootstrap(context);
        VillagePlacements.bootstrap(context);
    }

    public static ResourceKey<PlacedFeature> createKey(final String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(name));
    }

    public static void register(
        final BootstrapContext<PlacedFeature> context,
        final ResourceKey<PlacedFeature> id,
        final Holder<ConfiguredFeature<?, ?>> feature,
        final List<PlacementModifier> placementModifiers
    ) {
        context.register(id, new PlacedFeature(feature, List.copyOf(placementModifiers)));
    }

    public static void register(
        final BootstrapContext<PlacedFeature> context,
        final ResourceKey<PlacedFeature> id,
        final Holder<ConfiguredFeature<?, ?>> feature,
        final PlacementModifier... placementModifiers
    ) {
        register(context, id, feature, List.of(placementModifiers));
    }

    public static PlacementModifier countExtra(final int count, final float chance, final int extra) {
        float weight = 1.0F / chance;
        if (Math.abs(weight - (int)weight) > 1.0E-5F) {
            throw new IllegalStateException("Chance data cannot be represented as list weight");
        }

        WeightedList<IntProvider> distribution = WeightedList.<IntProvider>builder()
            .add(ConstantInt.of(count), (int)weight - 1)
            .add(ConstantInt.of(count + extra), 1)
            .build();
        return CountPlacement.of(new WeightedListInt(distribution));
    }

    public static PlacementFilter isEmpty() {
        return BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE);
    }

    public static BlockPredicateFilter filteredByBlockSurvival(final Block block) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO));
    }

    public static Holder<PlacedFeature> inlinePlaced(final Holder<ConfiguredFeature<?, ?>> configuredFeature, final PlacementModifier... placedFeatures) {
        return Holder.direct(new PlacedFeature(configuredFeature, List.of(placedFeatures)));
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> inlinePlaced(
        final F feature, final FC config, final PlacementModifier... placedFeatures
    ) {
        return inlinePlaced(Holder.direct(new ConfiguredFeature(feature, config)), placedFeatures);
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> onlyWhenEmpty(final F feature, final FC config) {
        return filtered(feature, config, BlockPredicate.ONLY_IN_AIR_PREDICATE);
    }

    public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> filtered(
        final F feature, final FC config, final BlockPredicate predicate
    ) {
        return inlinePlaced(feature, config, BlockPredicateFilter.forPredicate(predicate));
    }
}