package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record HugeMushroomFeatureConfiguration(BlockStateProvider capProvider, BlockStateProvider stemProvider, int foliageRadius, BlockPredicate canPlaceOn)
    implements FeatureConfiguration {
    public static final Codec<HugeMushroomFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockStateProvider.CODEC.fieldOf("cap_provider").forGetter(c -> c.capProvider),
                BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter(c -> c.stemProvider),
                Codec.INT.optionalFieldOf("foliage_radius", 2).forGetter(c -> c.foliageRadius),
                BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(c -> c.canPlaceOn)
            )
            .apply(i, HugeMushroomFeatureConfiguration::new)
    );
}