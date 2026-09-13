package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record SpeleothemClusterConfiguration(
    BlockState baseBlock,
    BlockState pointedBlock,
    HolderSet<Block> replaceableBlocks,
    int floorToCeilingSearchRange,
    IntProvider height,
    IntProvider radius,
    int maxStalagmiteStalactiteHeightDiff,
    int heightDeviation,
    IntProvider speleothemBlockLayerThickness,
    FloatProvider density,
    FloatProvider wetness,
    float chanceOfSpeleothemAtMaxDistanceFromCenter,
    int maxDistanceFromEdgeAffectingChanceOfSpeleothem,
    int maxDistanceFromCenterAffectingHeightBias
) implements FeatureConfiguration {
    public static final Codec<SpeleothemClusterConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("base_block").forGetter(c -> c.baseBlock),
                BlockState.CODEC.fieldOf("pointed_block").forGetter(c -> c.pointedBlock),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks),
                Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").forGetter(c -> c.floorToCeilingSearchRange),
                IntProviders.codec(1, 128).fieldOf("height").forGetter(c -> c.height),
                IntProviders.codec(1, 128).fieldOf("radius").forGetter(c -> c.radius),
                Codec.intRange(0, 64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter(c -> c.maxStalagmiteStalactiteHeightDiff),
                Codec.intRange(1, 64).fieldOf("height_deviation").forGetter(c -> c.heightDeviation),
                IntProviders.codec(0, 128).fieldOf("speleothem_block_layer_thickness").forGetter(c -> c.speleothemBlockLayerThickness),
                FloatProviders.codec(0.0F, 2.0F).fieldOf("density").forGetter(c -> c.density),
                FloatProviders.codec(0.0F, 2.0F).fieldOf("wetness").forGetter(c -> c.wetness),
                Codec.floatRange(0.0F, 1.0F)
                    .fieldOf("chance_of_speleothem_at_max_distance_from_center")
                    .forGetter(c -> c.chanceOfSpeleothemAtMaxDistanceFromCenter),
                Codec.intRange(1, 64)
                    .fieldOf("max_distance_from_edge_affecting_chance_of_speleothem")
                    .forGetter(c -> c.maxDistanceFromEdgeAffectingChanceOfSpeleothem),
                Codec.intRange(1, 64).fieldOf("max_distance_from_center_affecting_height_bias").forGetter(c -> c.maxDistanceFromCenterAffectingHeightBias)
            )
            .apply(i, SpeleothemClusterConfiguration::new)
    );
}