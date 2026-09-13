package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WeightedStateProvider extends BlockStateProvider {
    public static final MapCodec<WeightedStateProvider> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(WeightedList.nonEmptyCodec(BlockState.CODEC).fieldOf("entries").forGetter(o -> o.weightedList)).apply(i, WeightedStateProvider::new)
    );
    private final WeightedList<BlockState> weightedList;

    public WeightedStateProvider(final WeightedList<BlockState> weightedList) {
        if (weightedList.isEmpty()) {
            throw new IllegalArgumentException("Weighted list must have at least one entry");
        }

        this.weightedList = weightedList;
    }

    public WeightedStateProvider(final WeightedList.Builder<BlockState> weightedList) {
        this(weightedList.build());
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
    }

    @Override
    public BlockState getState(final WorldGenLevel level, final RandomSource random, final BlockPos pos) {
        return this.weightedList.getRandomOrThrow(random);
    }
}