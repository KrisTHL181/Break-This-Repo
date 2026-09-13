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

public class LargeDripstoneConfiguration implements FeatureConfiguration {
    public static final Codec<LargeDripstoneConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks),
                Codec.intRange(1, 512).optionalFieldOf("floor_to_ceiling_search_range", 30).forGetter(c -> c.floorToCeilingSearchRange),
                IntProviders.codec(1, 16).fieldOf("column_radius").forGetter(c -> c.columnRadius),
                FloatProviders.codec(0.0F, 20.0F).fieldOf("height_scale").forGetter(c -> c.heightScale),
                Codec.floatRange(0.1F, 1.0F).fieldOf("max_column_radius_to_cave_height_ratio").forGetter(c -> c.maxColumnRadiusToCaveHeightRatio),
                FloatProviders.codec(0.1F, 10.0F).fieldOf("stalactite_bluntness").forGetter(c -> c.stalactiteBluntness),
                FloatProviders.codec(0.1F, 10.0F).fieldOf("stalagmite_bluntness").forGetter(c -> c.stalagmiteBluntness),
                FloatProviders.codec(0.0F, 2.0F).fieldOf("wind_speed").forGetter(c -> c.windSpeed),
                Codec.intRange(0, 100).fieldOf("min_radius_for_wind").forGetter(c -> c.minRadiusForWind),
                Codec.floatRange(0.0F, 5.0F).fieldOf("min_bluntness_for_wind").forGetter(c -> c.minBluntnessForWind)
            )
            .apply(i, LargeDripstoneConfiguration::new)
    );
    public final HolderSet<Block> replaceableBlocks;
    public final int floorToCeilingSearchRange;
    public final IntProvider columnRadius;
    public final FloatProvider heightScale;
    public final float maxColumnRadiusToCaveHeightRatio;
    public final FloatProvider stalactiteBluntness;
    public final FloatProvider stalagmiteBluntness;
    public final FloatProvider windSpeed;
    public final int minRadiusForWind;
    public final float minBluntnessForWind;

    public LargeDripstoneConfiguration(
        final HolderSet<Block> replaceableBlocks,
        final int floorToCeilingSearchRange,
        final IntProvider columnRadius,
        final FloatProvider heightScale,
        final float maxColumnRadiusToCaveHeightRatio,
        final FloatProvider stalactiteBluntness,
        final FloatProvider stalagmiteBluntness,
        final FloatProvider windSpeed,
        final int minRadiusForWind,
        final float minBluntnessForWind
    ) {
        this.replaceableBlocks = replaceableBlocks;
        this.floorToCeilingSearchRange = floorToCeilingSearchRange;
        this.columnRadius = columnRadius;
        this.heightScale = heightScale;
        this.maxColumnRadiusToCaveHeightRatio = maxColumnRadiusToCaveHeightRatio;
        this.stalactiteBluntness = stalactiteBluntness;
        this.stalagmiteBluntness = stalagmiteBluntness;
        this.windSpeed = windSpeed;
        this.minRadiusForWind = minRadiusForWind;
        this.minBluntnessForWind = minBluntnessForWind;
    }
}