package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record BlockColumnConfiguration(List<BlockColumnConfiguration.Layer> layers, Direction direction, BlockPredicate allowedPlacement, boolean prioritizeTip)
    implements FeatureConfiguration {
    public static final Codec<BlockColumnConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockColumnConfiguration.Layer.CODEC.listOf().fieldOf("layers").forGetter(BlockColumnConfiguration::layers),
                Direction.CODEC.fieldOf("direction").forGetter(BlockColumnConfiguration::direction),
                BlockPredicate.CODEC.fieldOf("allowed_placement").forGetter(BlockColumnConfiguration::allowedPlacement),
                Codec.BOOL.fieldOf("prioritize_tip").forGetter(BlockColumnConfiguration::prioritizeTip)
            )
            .apply(i, BlockColumnConfiguration::new)
    );

    public static BlockColumnConfiguration.Layer layer(final IntProvider height, final BlockStateProvider state) {
        return new BlockColumnConfiguration.Layer(height, state);
    }

    public static BlockColumnConfiguration simple(final IntProvider height, final BlockStateProvider state) {
        return new BlockColumnConfiguration(List.of(layer(height, state)), Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false);
    }

    public record Layer(IntProvider height, BlockStateProvider state) {
        public static final Codec<BlockColumnConfiguration.Layer> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    IntProviders.NON_NEGATIVE_CODEC.fieldOf("height").forGetter(BlockColumnConfiguration.Layer::height),
                    BlockStateProvider.CODEC.fieldOf("provider").forGetter(BlockColumnConfiguration.Layer::state)
                )
                .apply(i, BlockColumnConfiguration.Layer::new)
        );
    }
}