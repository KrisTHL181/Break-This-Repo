package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record RootSystemConfiguration(
    Holder<PlacedFeature> treeFeature,
    int requiredVerticalSpaceForTree,
    int levelTestDistance,
    int maxLevelDeviation,
    int rootRadius,
    HolderSet<Block> rootReplaceable,
    BlockStateProvider rootStateProvider,
    int rootPlacementAttempts,
    int rootColumnMaxHeight,
    int hangingRootRadius,
    int hangingRootsVerticalSpan,
    BlockStateProvider hangingRootStateProvider,
    int hangingRootPlacementAttempts,
    int allowedVerticalWaterForTree,
    BlockPredicate allowedTreePosition
) implements FeatureConfiguration {
    public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                PlacedFeature.CODEC.fieldOf("feature").forGetter(RootSystemConfiguration::treeFeature),
                Codec.intRange(1, 64).fieldOf("required_vertical_space_for_tree").forGetter(RootSystemConfiguration::requiredVerticalSpaceForTree),
                Codec.intRange(0, 16).fieldOf("level_test_distance").forGetter(RootSystemConfiguration::levelTestDistance),
                Codec.intRange(0, 64).fieldOf("max_level_deviation").forGetter(RootSystemConfiguration::maxLevelDeviation),
                Codec.intRange(1, 64).fieldOf("root_radius").forGetter(RootSystemConfiguration::rootRadius),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("root_replaceable").forGetter(RootSystemConfiguration::rootReplaceable),
                BlockStateProvider.CODEC.fieldOf("root_state_provider").forGetter(RootSystemConfiguration::rootStateProvider),
                Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter(RootSystemConfiguration::rootPlacementAttempts),
                Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter(RootSystemConfiguration::rootColumnMaxHeight),
                Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter(RootSystemConfiguration::hangingRootRadius),
                Codec.intRange(1, 16).fieldOf("hanging_roots_vertical_span").forGetter(RootSystemConfiguration::hangingRootsVerticalSpan),
                BlockStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter(RootSystemConfiguration::hangingRootStateProvider),
                Codec.intRange(1, 256).fieldOf("hanging_root_placement_attempts").forGetter(RootSystemConfiguration::hangingRootPlacementAttempts),
                Codec.intRange(1, 64).fieldOf("allowed_vertical_water_for_tree").forGetter(RootSystemConfiguration::allowedVerticalWaterForTree),
                BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter(RootSystemConfiguration::allowedTreePosition)
            )
            .apply(i, RootSystemConfiguration::new)
    );
}