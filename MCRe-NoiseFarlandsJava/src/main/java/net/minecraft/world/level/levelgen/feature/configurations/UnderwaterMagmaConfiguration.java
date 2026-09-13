package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class UnderwaterMagmaConfiguration implements FeatureConfiguration {
    public static final Codec<UnderwaterMagmaConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.intRange(0, 512).fieldOf("floor_search_range").forGetter(c -> c.floorSearchRange),
                Codec.intRange(0, 64).fieldOf("placement_radius_around_floor").forGetter(c -> c.placementRadiusAroundFloor),
                Codec.floatRange(0.0F, 1.0F).fieldOf("placement_probability_per_valid_position").forGetter(c -> c.placementProbabilityPerValidPosition)
            )
            .apply(i, UnderwaterMagmaConfiguration::new)
    );
    public final int floorSearchRange;
    public final int placementRadiusAroundFloor;
    public final float placementProbabilityPerValidPosition;

    public UnderwaterMagmaConfiguration(final int floorSearchRange, final int placementRadiusAroundFloor, final float placementProbabilityPerValidPosition) {
        this.floorSearchRange = floorSearchRange;
        this.placementRadiusAroundFloor = placementRadiusAroundFloor;
        this.placementProbabilityPerValidPosition = placementProbabilityPerValidPosition;
    }
}